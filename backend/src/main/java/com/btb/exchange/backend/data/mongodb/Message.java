package com.btb.exchange.backend.data.mongodb;

import static org.springframework.data.mongodb.core.mapping.FieldType.STRING;

import com.btb.exchange.shared.dto.ExchangeEnum;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.knowm.xchange.currency.CurrencyPair;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Builder
@Document
@AllArgsConstructor
class Message {

    @Id
    private String id;

    @Indexed
    private Date created;

    private ExchangeEnum exchange;
    private long order;
    private @Field(targetType = STRING) CurrencyPair currencyPair;
    private String data;
}
