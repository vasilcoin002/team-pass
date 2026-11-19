package com.example.teampass.service;

import com.example.teampass.model.product.Currency;
import com.example.teampass.model.product.Price;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Primary
public class FixedExchangeRateService implements ExchangeRateService {
    @Override
    public BigDecimal convertToCzk(Price price) {
        if (price == null || price.currency() == null || price.amount() == null) {
            throw new IllegalArgumentException();
        }

        if (price.currency() == Currency.CZK) {
            return price.amount();
        } else if (price.currency() == Currency.USD) {
            return price.amount().multiply(BigDecimal.valueOf(22));
        } else if (price.currency() == Currency.EUR) {
            return price.amount().multiply(BigDecimal.valueOf(25));
        }

        return null;
    }
}
