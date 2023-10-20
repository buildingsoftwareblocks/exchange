package com.btb.exchange.frontend.api;

import com.btb.exchange.frontend.service.ExchangeService;
import com.btb.exchange.shared.dto.ExchangeEnum;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.knowm.xchange.currency.CurrencyPair;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/exchange")
@RequiredArgsConstructor
@Slf4j
public class ExchangeController {

    private final ExchangeService exchangeService;

    @GetMapping("/all")
    public List<ExchangeEnum> exchanges() {
        log.info("exchanges()");
        return exchangeService.activeExchanges();
    }

    @GetMapping("/currencies/{exchange}")
    public List<String> exchangeCurrencies(@PathVariable String exchange) {
        log.info("exchangeCurrencies({})", exchange);
        try {
            ExchangeEnum e = ExchangeEnum.valueOf(exchange);
            return exchangeService.activeCurrencies(e).stream()
                    .map(CurrencyPair::toString)
                    .toList();
        } catch (IllegalArgumentException e) {
            log.info("Exception {} : {}", exchange, e.getMessage());
        }
        return Collections.emptyList();
    }
}
