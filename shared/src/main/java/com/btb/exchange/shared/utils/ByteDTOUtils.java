package com.btb.exchange.shared.utils;

import com.btb.exchange.shared.dto.ExchangeEnum;
import com.btb.exchange.shared.dto.ExchangeOrderBook;
import com.btb.exchange.shared.dto.Opportunities;
import com.btb.exchange.shared.dto.Opportunity;
import com.btb.exchange.shared.proto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.dto.trade.LimitOrder;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
class ByteDTOUtils {

    public <T> T from(byte[] content, Class<T> valueType) {
        try {
        if (valueType.equals(ExchangeOrderBook.class)) {
            var orderBook = P_ExchangeOrderBook.parseFrom(content);
            return (T) transform(orderBook);
        }
        if (valueType.equals(Opportunities.class)) {
            var opportunities = P_Opportunities.parseFrom(content);
            return (T) transform(opportunities);
        }
    } catch (InvalidProtocolBufferException e) {
        log.error("Exception", e);
        throw new RuntimeException(e.getCause());
    }
        throw new RuntimeException("unknown valuetype");
    }

    private ExchangeOrderBook transform(P_ExchangeOrderBook eob) {
        List<LimitOrder> asks = eob.getOrderBook().getAsksList().stream().map(lo ->
                new LimitOrder(Order.OrderType.valueOf(lo.getType()),
                        convert(lo.getOriginalAmount()),
                        convert(lo.getCurrencyPair()),
                        lo.getId(),
                        new Date(lo.getTimestamp()),
                        convert(lo.getLimitPrice())))
                .collect(Collectors.toList());

        List<LimitOrder> bids = eob.getOrderBook().getBidsList().stream().map(lo ->
                new LimitOrder(Order.OrderType.valueOf(lo.getType()),
                        convert(lo.getOriginalAmount()),
                        convert(lo.getCurrencyPair()),
                        lo.getId(),
                        new Date(lo.getTimestamp()),
                        convert(lo.getLimitPrice())))
                .collect(Collectors.toList());

        var orderBook = new OrderBook(new Date(eob.getOrderBook().getTimestamp()), asks, bids);

        return new ExchangeOrderBook(eob.getOrder(), convert(eob.getTimestamp()), convert(eob.getExchange()),
                convert(eob.getCurrencyPair()),orderBook);
    }

    private Opportunities transform(P_Opportunities opportunities) {
        return Opportunities.builder()
                .order(opportunities.getOrder())
                .timestamp(convert(opportunities.getTimestamp()))
                .values(opportunities.getValuesList().stream().map(o -> new Opportunity(
                       convert(o.getCurrencyPair()),
                        convert(o.getAmount()),
                        convert(o.getProfit()),
                        convert(o.getFrom()),
                        convert(o.getAsk()),
                        convert(o.getTo()),
                        convert(o.getBid()),
                        convert(o.getCreated())
                        )).collect(Collectors.toList())).build();
    }

    byte[] to(Object value) {
            if (value instanceof ExchangeOrderBook) {
                return to((ExchangeOrderBook)value);
            }
            if (value instanceof Opportunities) {
                return to((Opportunities)value);
            }
        throw new RuntimeException("unknown valuetype");
    }

    byte[] to(ExchangeOrderBook value) {
        var asks= value.getOrderBook().getAsks().stream().map(l -> P_LimitOrder.newBuilder()
                .setType(l.getType().name())
                .setOriginalAmount(convert(l.getOriginalAmount()))
                .setCurrencyPair(convert(l.getCurrencyPair()))
                .setId(l.getId())
                .setTimestamp(l.getTimestamp().getTime())
                .setLimitPrice(convert(l.getLimitPrice()))
                .build())
                .collect(Collectors.toList());

        var bids= value.getOrderBook().getBids().stream().map(l -> P_LimitOrder.newBuilder()
                .setType(l.getType().name())
                .setOriginalAmount(convert(l.getOriginalAmount()))
                .setCurrencyPair(convert(l.getCurrencyPair()))
                .setId(l.getId())
                .setTimestamp(l.getTimestamp().getTime())
                .setLimitPrice(convert(l.getLimitPrice()))
                .build())
                .collect(Collectors.toList());

        var builder = P_ExchangeOrderBook.newBuilder()
                .setOrder(value.getOrder())
                .setTimestamp(convert(value.getTimestamp()))
                .setExchange(convert(value.getExchange()))
                .setCurrencyPair(convert(value.getCurrencyPair()))
                .setOrderBook(P_OrderBook.newBuilder()
                        .addAllAsks(asks)
                        .addAllBids(bids)
                        .setTimestamp(value.getOrderBook().getTimeStamp().getTime())
                ).build();

        return builder.toByteArray();
    }

    byte[] to(Opportunities value) {
        var builder = P_Opportunities.newBuilder()
                .setOrder(value.getOrder())
                .setTimestamp(value.getTimestamp().toNanoOfDay())
                .addAllValues(
                        value.getValues().stream().map(o -> P_Opportunity.newBuilder()
                                .setCurrencyPair(convert(o.getCurrencyPair()))
                                .setAmount(convert(o.getAmount()))
                                .setProfit(convert(o.getProfit()))
                                .setFrom(convert(o.getFrom()))
                                .setAsk(convert(o.getAsk()))
                                .setTo(convert(o.getTo()))
                                .setBid(convert(o.getBid()))
                                .setCreated(convert(o.getCreated()))
                                .build())
                .collect(Collectors.toList())).build();

        return builder.toByteArray();
    }

    P_DecimalValue convert(BigDecimal bigDecimal) {
        return P_DecimalValue.newBuilder()
                .setScale(bigDecimal.scale())
                .setPrecision(bigDecimal.precision())
                .setValue(ByteString.copyFrom(bigDecimal.unscaledValue().toByteArray()))
                .build();
    }

    BigDecimal convert(P_DecimalValue dc) {
        return new BigDecimal(
                new java.math.BigInteger(dc.getValue().toByteArray()),
                dc.getScale(),
                new java.math.MathContext(dc.getPrecision()));
    }

    P_CurrencyPair convert(CurrencyPair cp) {
        return P_CurrencyPair.newBuilder().setValue(cp.toString()).build();
    }

    CurrencyPair convert(P_CurrencyPair cp) {
        return new CurrencyPair(cp.getValue());
    }

    P_Exchange convert (ExchangeEnum exchangeEnum) {
        return P_Exchange.newBuilder().setValue(exchangeEnum.name()).build();
    }

    ExchangeEnum convert(P_Exchange exchange) {
        return ExchangeEnum.valueOf(exchange.getValue());
    }


    long convert(LocalTime time) {
        return time.toNanoOfDay();
    }

    LocalTime convert(long time) {
        return LocalTime.ofNanoOfDay(time);
    }
}
