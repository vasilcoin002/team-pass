package com.example.teampass.service;

import com.example.teampass.model.product.Price;

import java.math.BigDecimal;

public interface ExchangeRateService {
    BigDecimal convertToCzk(Price price);
}
