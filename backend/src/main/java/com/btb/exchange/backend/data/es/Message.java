package com.btb.exchange.backend.data.es;

import com.btb.exchange.shared.dto.ExchangeEnum;
import com.btb.exchange.shared.dto.Orders;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;

@Data
@Document(indexName = "orderbook-#{T(java.time.LocalDate).now().toString()}")
@Builder
@AllArgsConstructor
class Message {

    @Id
    private String id;
    @Field(type = FieldType.Date, format = DateFormat.basic_date_time)
    private Date created;
    @Field(type = FieldType.Text)
    private ExchangeEnum exchange;
    @Field(type = FieldType.Text)
    private String currencyPair;
    @Field(type = FieldType.Nested)
    private Orders orders;
}

