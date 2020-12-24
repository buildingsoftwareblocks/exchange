package com.btb.exchange.backend.data;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

import java.util.Date;

@Data
@Builder
public class Message {

    @Id
    private String id;
    @Indexed
    private Date created;
    private String currencyPair;
    private String message;
}
