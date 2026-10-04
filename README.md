# Cosmo Cats Intergalactic Marketplace

Backend маркетплейсу космічних товарів для котів. Лабораторна робота 1: API-контракт, DDD, CRUD для `Product`, обробка помилок за RFC 9457, пагінація.

## Стек

- Java 17+
- Spring Boot 3.3 (Web, Validation)
- Gradle (wrapper у репозиторії)
- springdoc-openapi (Swagger UI для перегляду контракту)

## Запуск

```bash
./gradlew bootRun          # Linux / macOS / Git Bash
gradlew.bat bootRun        # Windows cmd / PowerShell
```

Застосунок стартує на `http://localhost:8080`.

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
├── application       ProductService, ProductValidationService, курсор пагінації, доменні винятки
├── infrastructure    інтерфейси репозиторіїв і in-memory реалізації з mock-даними
└── web               REST-контролер, DTO, ProductMapper, пагінація, валідація, обробка помилок
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

Коди помилок: 400 (валідація), 404 (продукт не знайдено), 409 (назва вже зайнята), 422 (категорія не існує), 500 (непередбачена помилка без деталей реалізації).

### Валідація `ProductRequestDto`

| Поле | Правила |
|---|---|
| `name` | обов'язкове, 3–100 символів, має містити космічне слово (`@CosmicWordCheck`) |
| `description` | необов'язкове, до 500 символів |
| `price` | обов'язкове, більше 0, не більше 2 знаків після коми |
| `stockQuantity` | обов'язкове, не менше 0 |
| `categoryId` | обов'язкове, UUID існуючої категорії |

Параметри пагінації: `1 <= pageSize <= 100` (за замовчуванням 20), `pageToken` з `nextPageToken` попередньої відповіді. Перша сторінка запитується без `pageToken`.

## Приклади

Категорії з mock-даних:

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

## Обмеження

- Дані зберігаються в пам'яті й скидаються після перезапуску. База даних буде в Lab 3.
- Перевірка унікальності назви не атомарна. У Lab 3 її замінить unique constraint у БД.
