package com.btb.exchange.frontend.api;

import com.btb.exchange.frontend.service.ExchangeService;
import com.btb.exchange.frontend.service.UserState;
import com.btb.exchange.shared.dto.ExchangeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.knowm.xchange.currency.CurrencyPair;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/exchange")
@RequiredArgsConstructor
@Slf4j
public class ExchangeController {

    private final ExchangeService exchangeService;
    private final UserState userState;

    @GetMapping("/all")
    public List<ExchangeEnum> exchanges() {
        return exchangeService.activeExchanges();
    }

    @PostMapping("/{exchange}")
    public void setExchange(@PathVariable ExchangeEnum exchange) {
        userState.changeExchange(exchange);
    }

    @GetMapping("/currencies")
    public List<String> exchangeCurrencies() {
        return userState.activeCurrencies().stream().map(CurrencyPair::toString).collect(Collectors.toList());
    }

    @PostMapping("/currency/{base}/{counter}")
    public void setCurrency(@PathVariable String base, @PathVariable String counter) {
        userState.changeCurrency(new CurrencyPair(base, counter));
    }
}
