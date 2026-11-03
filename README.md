# Cosmo Cats Intergalactic Marketplace

Backend маркетплейсу космічних товарів для котів.

- Lab 1: API-контракт, DDD, CRUD для `Product`, обробка помилок за RFC 9457, пагінація.
- Lab 2: юніт-, контролерні та інтеграційні тести, покриття JaCoCo з перевіркою в CI, ціна в іншій валюті через зовнішній сервіс, застабований WireMock.
- Lab 2.1 (додаткова): feature toggles на рівні середовища зі Spring AOP.
- Lab 3: PostgreSQL, схема в 3NF через Liquibase, Spring Data JPA, транзакції, Natural ID для замовлень, звіт з projection, тести з Testcontainers.

## Стек

- Java 17+
- Spring Boot 3.3 (Web, Validation, AOP, Data JPA)
- PostgreSQL 16, Liquibase
- Gradle (wrapper у репозиторії)
- springdoc-openapi (Swagger UI для перегляду контракту)
- Тести: JUnit 5, Mockito, AssertJ, MockMvc, WireMock, Testcontainers, JaCoCo

## Запуск

Потрібна база PostgreSQL. Локально її піднімає Docker Compose, схему й початкові дані створює Liquibase при старті застосунку:

```bash
docker compose -f scripts/docker/compose.yaml up -d
```

```bash
./gradlew bootRun          # Linux / macOS / Git Bash
gradlew.bat bootRun        # Windows cmd / PowerShell
```

Застосунок стартує на `http://localhost:8080`. Підключення до іншої бази задається змінними `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.

- Swagger UI: http://localhost:8080/swagger-ui.html
- Контракт: `src/main/resources/api-specs/cosmo-cats-product-api.yml`

## Архітектура

Шари за принципом Onion: `web` → `application` → `domain`. Домен не знає про Spring. Інтерфейси репозиторіїв лежать разом з реалізаціями в `infrastructure/persistence`.

```
com.cosmocats.marketplace
├── domain            доменна модель
│   ├── product       Product
│   ├── category      Category
│   ├── cart          Cart, CartItem
│   └── order         Order, OrderItem, OrderStatus
├── application       сервіси продуктів, замовлень, звітів і цін, курсор пагінації, винятки
├── infrastructure    persistence: JPA-сутності, Spring Data репозиторії, projection; exchangerate: HTTP-клієнт сервісу курсів
├── featuretoggle     @FeatureToggle, FeatureToggleAspect, FeatureToggleService, конфігурація прапорів
└── web               REST-контролери, DTO, мапери, пагінація, валідація, обробка помилок
```

Рішення:

- Гроші зберігаються як `BigDecimal`, щоб уникнути помилок округлення.
- DTO і доменні об'єкти зроблені як Java records.
- Валідація в три кроки: Bean Validation на DTO, базові правила в `ProductMapper` під час маппінгу, перевірки з даними (існування категорії, унікальність назви) в `ProductValidationService`.
- `Product` посилається на категорію через `categoryId`, бо `Category` є окремим агрегатом.
- Маппінг DTO ↔ домен ручний, клас `ProductMapper` (`toDto` / `toDomain`).
- Id генерує сервер. Клієнт не може передати або змінити його через тіло запиту.

## Ендпоінти

| Метод | URL | Опис | Успіх |
|---|---|---|---|
| GET | `/api/v1/products?pageSize=20&pageToken=...` | Сторінка продуктів | 200 |
| GET | `/api/v1/products/{id}` | Продукт за id | 200 |
| POST | `/api/v1/products` | Створити продукт | 201 + `Location` |
| PUT | `/api/v1/products/{id}` | Повністю оновити продукт | 200 |
| DELETE | `/api/v1/products/{id}` | Видалити продукт | 204 |
| GET | `/api/v1/products/{id}/price?currency=EUR` | Ціна продукту в іншій валюті | 200 |
| GET | `/api/v1/cosmo-cats` | Космо-коти (за прапором `cosmoCats`) | 200 |
| GET | `/api/v1/kitty-products` | Товари для кошенят (за прапором `kittyProducts`) | 200 |
| POST | `/api/v1/orders` | Оформити кошик як замовлення | 201 + `Location` |
| GET | `/api/v1/orders/{cartId}` | Замовлення за id кошика | 200 |
| GET | `/api/v1/reports/popular-products?limit=10` | Товари, які купують найчастіше | 200 |

Коди помилок: 400 (валідація), 404 (продукт або замовлення не знайдено), 409 (назва вже зайнята в категорії, кошик уже оформлений, не вистачає товару, продукт є в замовленнях, одночасна зміна тих самих даних), 422 (категорія, кіт або продукт із тіла запиту не існує, валюту не знає сервіс курсів), 500 (непередбачена помилка без деталей реалізації), 503 (сервіс курсів недоступний).

### Валідація `ProductRequestDto`

| Поле | Правила |
|---|---|
| `name` | обов'язкове, 3–100 символів, має містити космічне слово в однині або множині (`@CosmicWordCheck`), унікальне в межах категорії без урахування регістру |
| `description` | необов'язкове, до 500 символів |
| `price` | обов'язкове, більше 0, не більше 2 знаків після коми |
| `stockQuantity` | обов'язкове, не менше 0 |
| `categoryId` | обов'язкове, UUID існуючої категорії |

Значення неправильного типу (`"categoryId": "abc"`, `"stockQuantity": 2.7`) дає 400 з назвою поля і повідомленням `has invalid value`.

Параметри пагінації: `1 <= pageSize <= 100` (за замовчуванням 20), `pageToken` з `nextPageToken` попередньої відповіді. Перша сторінка запитується без `pageToken`.

## Приклади

Категорії з початкових даних (`005-seed-catalog-and-cosmo-cats.yaml`):

| id | Назва |
|---|---|
| `11111111-1111-1111-1111-111111111111` | Anti-gravity toys |
| `22222222-2222-2222-2222-222222222222` | Cosmic food |
| `33333333-3333-3333-3333-333333333333` | Space gear |

Створення продукту:

```bash
curl -i -X POST http://localhost:8080/api/v1/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Moon dust catnip","description":"Collected on the dark side","price":7.25,"stockQuantity":50,"categoryId":"22222222-2222-2222-2222-222222222222"}'
```

Помилка валідації:

```bash
curl -i -X POST http://localhost:8080/api/v1/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Yarn","price":0,"stockQuantity":5,"categoryId":"22222222-2222-2222-2222-222222222222"}'
```

```json
{
  "type": "https://cosmo-cats.market/errors/validation",
  "title": "Validation failed",
  "status": 400,
  "detail": "Field 'name' must contain at least one cosmic word (...). Field 'price' must be greater than 0.",
  "instance": "/api/v1/products",
  "errors": [
    { "field": "name", "message": "must contain at least one cosmic word (...)" },
    { "field": "price", "message": "must be greater than 0" }
  ]
}
```

Сторінка продуктів:

```bash
curl "http://localhost:8080/api/v1/products?pageSize=2"
```

```json
{
  "content": [ { "id": "...", "name": "Anti-gravity star yarn ball", "...": "..." }, { "...": "..." } ],
  "page": {
    "size": 2,
    "totalElements": 4,
    "nextPageToken": "YWFhYWFhYWEtMDAwMC0wMDAwLTAwMDAtMDAwMDAwMDAwMDAzOkNvbWV0IHRhaWwgc2NyYXRjaGluZyBwb3N0"
  }
}
```

Наступна сторінка:

```bash
curl "http://localhost:8080/api/v1/products?pageSize=2&pageToken=YWFhYWFhYWEtMDAwMC0wMDAwLTAwMDAtMDAwMDAwMDAwMDAzOkNvbWV0IHRhaWwgc2NyYXRjaGluZyBwb3N0"
```

Пагінація keyset: токен кодує назву й id останнього продукту на сторінці, наступна сторінка починається одразу після нього. Тому вставки й видалення між запитами не зсувають сторінки. На останній сторінці `nextPageToken` дорівнює `null`.

## Ціна в іншій валюті

Ціни в каталозі зберігаються в USD. `GET /api/v1/products/{id}/price?currency=EUR` бере курс із зовнішнього сервісу курсів (`GET {base-url}/v1/rates/USD/EUR` → `{"base": "USD", "target": "EUR", "rate": 0.9215}`) і округлює ціну до центів (half-even).

```json
{
  "productId": "aaaaaaaa-0000-0000-0000-000000000001",
  "basePrice": 12.50,
  "baseCurrency": "USD",
  "price": 11.52,
  "currency": "EUR",
  "exchangeRate": 0.9215
}
```

| Ситуація | Відповідь |
|---|---|
| `currency` не з 3 великих латинських літер | 400, сервіс курсів не викликається |
| продукт не існує | 404, сервіс курсів не викликається |
| `currency=USD` | 200 з курсом 1, сервіс курсів не викликається |
| сервіс курсів повернув 404 | 422 `currency-not-supported` |
| 5xx, інший 4xx, таймаут, обрив з'єднання, не JSON, курс відсутній або ≤ 0 | 503 `exchange-rate-unavailable`, деталі тільки в лозі |

Налаштування в `application.yml`: `application.exchange-rate-service.base-url`, `connect-timeout` (1s), `read-timeout` (2s).

Для ручної перевірки сервіс курсів можна підняти з готовими стабами з `scripts/wiremock` (EUR, UAH, GBP; JPY відповідає з затримкою 5 с, щоб побачити 503; решта валют дає 404):

```bash
docker run --rm -p 8090:8080 -v "$PWD/scripts/wiremock:/home/wiremock" wiremock/wiremock:3.9.2
```

```bash
curl "http://localhost:8080/api/v1/products/aaaaaaaa-0000-0000-0000-000000000001/price?currency=EUR"
```

## Feature toggles

Функції вмикаються й вимикаються в конфігурації без зміни коду:

```yaml
feature:
  cosmoCats:
    enabled: true      # GET /api/v1/cosmo-cats
  kittyProducts:
    enabled: false     # GET /api/v1/kitty-products
```

- Метод або весь клас позначається `@FeatureToggle(Feature.COSMO_CATS)`. `FeatureToggleAspect` (`@Around`) перевіряє прапор до виклику: якщо функція вимкнена, метод не викликається, кидається `FeatureNotAvailableException`, клієнт отримує 404 з типом `feature-not-available`. Анотація на методі має пріоритет над анотацією на класі.
- `FeatureToggleConfig` читає всі `feature.<key>.enabled` з `Environment` у мапу, тому прапор можна перевизначити для середовища: профілем, змінною оточення `FEATURE_COSMOCATS_ENABLED=false` або аргументом `--feature.cosmoCats.enabled=false`. `cosmoCats`, `cosmo-cats` і `cosmocats` вважаються однією функцією.
- Прапор, якого немає в конфігурації, означає «вимкнено»: помилка в назві не вмикає недороблену функцію. Значення не `true`/`false` або одна функція з різними значеннями під різними написаннями зупиняють запуск застосунку.
- Обмеження Spring AOP через проксі: виклик анотованого методу з іншого методу того самого класу не перевіряється (є тест, що це фіксує).
- Прапори читаються один раз при старті, для зміни потрібен перезапуск.

Тести: `FeatureToggleAspectTest`, `FeatureToggleConfigTest`, `CosmoCatServiceTest` і `KittyProductServiceTest` (маленький Spring-контекст зі справжнім аспектом), `FeatureToggledControllersTest`, `DefaultFeatureTogglesIT` і `SwitchedFeatureTogglesIT` (той самий застосунок з прапорами з `application.yml` і з перевизначеними для середовища).

## База даних (Lab 3)

### Схема

```
category (id, reference UK, name UK)
    1 ── * product (id, reference UK, name, description, price, stock_quantity, category_id FK, version)
                        1 ── * order_item (id, order_id FK, product_id FK, quantity, unit_price)   UK (order_id, product_id)
cosmo_cat (id, reference UK, name, email UK, home_planet)                          * ── 1
    1 ── * orders (id, cart_id UK — natural id, cosmo_cat_id FK, status, created_at) 1 ── *
```

- 3NF: кожна таблиця описує одну сутність, неключові колонки залежать тільки від ключа. Назва категорії зберігається один раз у `category`, а не в кожному продукті. Сума замовлення не зберігається, а рахується з позицій, тому не може розійтися з ними.
- `order_item.unit_price` — ціна на момент замовлення. Це окремий факт, а не копія `product.price`: зміна ціни в каталозі не змінює старі замовлення.
- `id` — сурогатні ключі з послідовностей (`*_id_seq`, крок 50, як `allocationSize` у сутностях). Назовні вони не виходять: API працює з UUID (`reference`), а замовлення шукаються за natural id `cart_id`.
- `product.version` — оптимістичне блокування: два замовлення не можуть одночасно зменшити той самий залишок.
- CHECK-обмеження дублюють правила застосунку: ціна > 0, залишок ≥ 0, кількість > 0, статус з переліку.

### Liquibase

`src/main/resources/db/changelog/db.changelog-master.yaml` підключає зміни по порядку:

| Файл | Що робить |
|---|---|
| `001-create-catalog-tables.yaml` | послідовності, `category`, `product`, FK, індекс на `product.category_id`, CHECK |
| `002-create-cosmo-cat-and-order-tables.yaml` | `cosmo_cat`, `orders`, `order_item`, FK, індекси, unique `cart_id` |
| тег `1.0.0` | початкова схема |
| `003-product-name-unique-in-category.yaml` | unique index `(category_id, lower(name))` і індекс `lower(name)` для пошуку й пагінації |
| `004-cosmo-cat-email-unique.yaml` | unique constraint на `cosmo_cat.email` |
| `005-seed-catalog-and-cosmo-cats.yaml` | початкові категорії, продукти й коти (id з `nextval`, щоб не конфліктувати з Hibernate) |
| тег `1.1.0` | обмеження й дані |

Застосовані файли не редагуються, кожна зміна йде новим файлом. Hibernate працює з `ddl-auto: validate`: він тільки перевіряє, що сутності збігаються зі схемою Liquibase.

### Доступ до даних

- Spring Data репозиторії в `infrastructure/persistence`: `ProductRepository`, `CategoryRepository`, `CosmoCatRepository`, `OrderRepository`. Сервіси працюють із сутностями всередині транзакції й повертають доменні records (`EntityMapper`), сутності не доходять до контролерів (`open-in-view: false`).
- `@Transactional` на сервісах: читання в `readOnly`-транзакціях, кожен запис — одна транзакція. Оформлення замовлення списує залишки всіх позицій; якщо одна позиція не проходить, відкочується все.
- Natural ID: `OrderEntity.cartId` позначений `@NaturalId`, `NaturalIdRepository.findByNaturalId` шукає через Hibernate API (`bySimpleNaturalId`). Базовий клас репозиторіїв задано в `JpaConfig`.
- Keyset-пагінація продуктів — JPQL-запити з `WHERE lower(name) > ...` і `ORDER BY lower(name), reference`.
- Звіт для Інтергалактичного комітету: `OrderRepository.findPopularProducts` — JPQL з `WHERE status IN`, `GROUP BY`, `ORDER BY sum(quantity) DESC` в interface projection `PopularProductView` (тільки 4 колонки, сутності не завантажуються). Скасовані замовлення не рахуються.

Приклад замовлення:

```bash
curl -i -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{"cartId":"dddddddd-0000-0000-0000-000000000001","customerId":"cccccccc-0000-0000-0000-000000000001","items":[{"productId":"aaaaaaaa-0000-0000-0000-000000000002","quantity":2}]}'
```

```bash
curl "http://localhost:8080/api/v1/reports/popular-products?limit=5"
```

## Тести

```bash
./gradlew build            # усі тести + звіт покриття + перевірка мінімального покриття
./gradlew test             # те саме без складання jar
```

Скрипти `gradle/test.gradle` і `gradle/jacoco.gradle` взяті з репозиторію курсу:

- `test` запускає юніт-тести (`*Test`), `integrationTest` запускає інтеграційні (`*IT`), `test` залежить від `integrationTest`. Інтеграційним тестам потрібен Docker: PostgreSQL піднімає Testcontainers (`PostgresTestContainer`, один контейнер на JVM);
- `jacocoTestReport` збирає спільний звіт з обох запусків у `build/reports/coverage/index.html` (HTML і CSV);
- `jacocoTestCoverageVerification` валить збірку, якщо будь-який файл має менше 80% покриття рядків. Виключений тільки клас з `main()`;
- зведення по тестах пишеться в `build/reports/tests/test-summary.md`.

Поточне покриття: 100% рядків, 98.8% гілок (239 юніт-тестів і 72 інтеграційні).

| Рівень | Класи | Що перевіряють |
|---|---|---|
| Сервіси (Mockito) | `ProductServiceImplTest`, `ProductValidationServiceTest`, `ProductPriceServiceTest` | кожен метод сервісів: генерація id, 404, 409, 422, пагінація з курсором, конвертація й округлення, що репозиторій/клієнт не викликається після помилки |
| Контролер (`@WebMvcTest`) | `ProductControllerTest` | валідація тіла, query- і path-параметрів, позитивні межові значення і негативні сценарії, неправильні типи, битий JSON, 404/409/422/500/503 у форматі problem details |
| Сервіси замовлень і звітів (Mockito) | `OrderServiceTest`, `ProductReportServiceTest` | ціни з каталогу, списання залишку, 409/422, статуси для звіту |
| Контролери замовлень і звітів (`@WebMvcTest`) | `OrderControllerTest`, `ReportControllerTest` | валідація замовлення (той самий продукт двічі, кількість, порожній список), коди помилок, межі `limit` |
| Юніт | `PageTokenCodecTest`, `CosmicWordValidatorTest`, `ProductMapperTest`, `EntityMapperTest`, `EachProductOnceValidatorTest`, `GlobalExceptionHandlerTest`, доменні тести | кодування токена сторінки, космічні слова, маппінг, інваріанти доменних records |
| Репозиторії (`@DataJpaTest` + Testcontainers) | `ProductRepositoryIT`, `CategoryRepositoryIT`, `CosmoCatRepositoryIT`, `OrderRepositoryIT`, `LiquibaseMigrationIT` | CRUD кожної сутності, natural id, keyset-сторінки, звіт з projection, оптимістичне блокування, change sets і теги Liquibase, індекси, послідовності, unique і CHECK обмеження |
| Інтеграційні (`@SpringBootTest` + Testcontainers) | `ProductApiIT`, `ProductPriceIT`, `ApiContractIT`, `OrderApiIT`, `ConcurrentRequestsIT`, тести прапорів | повний CRUD через усі шари, відкат транзакції замовлення, одночасні запити (409 замість 500), сервіс курсів через WireMock, відповідність ендпоінтів OpenAPI-контракту |

CI: `.github/workflows/pull_request.yml` запускає `./gradlew build` на кожен pull request, додає зведення тестів і покриття в summary запуску і зберігає HTML-звіт покриття як артефакт `test-coverage-report`.

## Обмеження

- Категорій і котів можна змінювати тільки міграціями, REST API для них немає.
- Перевірки в сервісах (назва, залишок) не атомарні. Одночасні запити ловлять обмеження БД і `@Version`, клієнт отримує 409 і має повторити запит сам.
- `PUT /products/{id}` перезаписує залишок цілком, а версію клієнт не передає. Якщо між GET і PUT хтось оформив замовлення, PUT поверне старий залишок. Виправлення — `ETag`/`If-Match` з `version`, це поза межами лаби.
- Курси валют не кешуються, повторних спроб і circuit breaker для сервісу курсів немає: при збої одразу 503.
