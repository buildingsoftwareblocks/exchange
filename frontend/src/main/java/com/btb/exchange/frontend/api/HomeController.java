package com.btb.exchange.frontend.api;

import com.btb.exchange.frontend.service.UserState;
import com.btb.exchange.frontend.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class HomeController {

    private final WebSocketService webSocketService;
    private final UserState userState;

    @GetMapping("/")
    public String main(HttpServletRequest request) {
        String sessionId = request.getSession().getId();
        userState.init(sessionId);

        return "index";
    }

    @MessageMapping("/init")
    public void init(Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionAttributes().get("HTTP.SESSION.ID").toString();
        log.info("User state: {}/{}", sessionId, principal.getName());
        webSocketService.register(sessionId, principal.getName());
    }
}
