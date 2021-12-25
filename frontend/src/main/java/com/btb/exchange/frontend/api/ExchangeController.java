package com.btb.exchange.frontend.api;

import com.btb.exchange.frontend.service.ExchangeService;
import com.btb.exchange.shared.dto.ExchangeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.knowm.xchange.currency.CurrencyPair;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/exchange")
@RequiredArgsConstructor
@Slf4j
public class ExchangeController {

    private final ExchangeService exchangeService;

    @GetMapping("/all")
    public List<ExchangeEnum> exchanges() {
        return exchangeService.activeExchanges();
    }

    @PostMapping("/{exchange}")
    public void setExchange(@PathVariable ExchangeEnum exchange) {
        exchangeService.changeExchange(exchange);
    }

    @GetMapping("/currencies")
    public List<String> exchangeCurrencies() {
        return exchangeService.activeCurrencies();
    }

    @PostMapping("/currency/{base}/{counter}")
    public void setCurrency(@PathVariable String base, @PathVariable String counter) {
        exchangeService.changeCurrency(new CurrencyPair(base, counter));
    }
}
