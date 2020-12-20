package com.btb.exchange.backend.data;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.util.Date;

@Data
@Builder
public class Message {

    @Id
    private String id;

    private Date created;
    private MessageType messageType;
    private String message;
}
