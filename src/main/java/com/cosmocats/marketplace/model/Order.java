package com.cosmocats.marketplace.model;

import java.util.Date;
import java.util.List;

public class Order {

    public Long id;
    public Long userId;
    public List<Product> products;
    public Double total;
    public String status;
    public Date date;
}
