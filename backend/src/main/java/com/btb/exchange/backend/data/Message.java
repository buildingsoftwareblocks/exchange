package com.btb.exchange.backend.data;

import com.btb.exchange.shared.dto.ExchangeEnum;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Builder
@Document
public class Message {

    @Id
    private String id;
    @Indexed
    private Date created;
    private ExchangeEnum exchange;
    private String currencyPair;
    private String message;
}
