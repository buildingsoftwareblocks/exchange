package com.btb.exchange.shared.dto;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Value;
import org.knowm.xchange.dto.trade.LimitOrder;

import java.util.ArrayList;
import java.util.List;

@Value
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
public class Orders {
    List<Order> asks;
    List<Order> bids;

    public Orders(List<LimitOrder> asks, List<LimitOrder> bids) {
        this.asks = new ArrayList<>(asks.size());
        this.bids = new ArrayList<>(bids.size());
        asks.forEach(ask -> this.asks.add(new Order(ask.getLimitPrice(), ask.getOriginalAmount())));
        bids.forEach(bid -> this.bids.add(new Order(bid.getLimitPrice(), bid.getOriginalAmount())));
    }

    public Orders(List<LimitOrder> asks, List<LimitOrder> bids, int maxOrders) {
        this.asks = new ArrayList<>(maxOrders);
        this.bids = new ArrayList<>(maxOrders);
        sublist(asks, maxOrders).forEach(ask -> this.asks.add(new Order(ask.getLimitPrice(), ask.getOriginalAmount())));
        sublist(bids, maxOrders).forEach(bid -> this.bids.add(new Order(bid.getLimitPrice(), bid.getOriginalAmount())));
    }

    private List<LimitOrder> sublist(List<LimitOrder> orders, int maxOrders) {
        int min = Math.min(orders.size(), maxOrders);
        return orders.subList(0, min);
    }
}
