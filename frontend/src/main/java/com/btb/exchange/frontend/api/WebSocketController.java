package com.btb.exchange.frontend.api;

import com.btb.exchange.frontend.service.WebSocketService;
import com.btb.exchange.shared.dto.ExchangeEnum;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.knowm.xchange.currency.CurrencyPair;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

  private final WebSocketService webSocketService;

  @MessageMapping("/exchange")
  public void changeExchange(@Payload ExchangeEnum message, Principal principal) {
    log.info("changeExchange({}) : {}", principal.getName(), message);
    webSocketService.register(principal, message);
  }

  @MessageMapping("/cp")
  public void changeCurrencyPair(@Payload CurrencyPair message, Principal principal) {
    log.info("changeCurrencyPair({}) : {}", principal.getName(), message);
    webSocketService.register(principal, message);
  }
}
