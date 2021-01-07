package com.btb.exchange.frontend.api;

import com.btb.exchange.frontend.service.ExchangeService;
import com.btb.exchange.shared.dto.ExchangeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/exchange")
@RequiredArgsConstructor
@Slf4j
public class ExchangeController {

    private final ExchangeService exchangeService;

    @GetMapping("/all")
    public List<ExchangeEnum> exchanges() {
        return List.of(ExchangeEnum.values());
    }

    @GetMapping("/{exchange}")
    public void setExchange(@PathVariable ExchangeEnum exchange) {
        log.info("setExchange() : {}", exchange);
        exchangeService.setExchange(exchange);
    }
}
