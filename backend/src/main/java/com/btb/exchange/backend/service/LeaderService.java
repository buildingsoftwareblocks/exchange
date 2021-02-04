package com.btb.exchange.backend.service;

import com.btb.exchange.backend.config.ApplicationConfig;
import com.btb.exchange.shared.dto.ExchangeEnum;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.bitrich.xchangestream.bankera.BankeraStreamingExchange;
import info.bitrich.xchangestream.binance.BinanceStreamingExchange;
import info.bitrich.xchangestream.bitfinex.BitfinexStreamingExchange;
import info.bitrich.xchangestream.bitmex.BitmexStreamingExchange;
import info.bitrich.xchangestream.bitstamp.v2.BitstampStreamingExchange;
import info.bitrich.xchangestream.btcmarkets.BTCMarketsStreamingExchange;
import info.bitrich.xchangestream.cexio.CexioStreamingExchange;
import info.bitrich.xchangestream.coinbasepro.CoinbaseProStreamingExchange;
import info.bitrich.xchangestream.coinjar.CoinjarStreamingExchange;
import info.bitrich.xchangestream.coinmate.CoinmateStreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import info.bitrich.xchangestream.gemini.GeminiStreamingExchange;
import info.bitrich.xchangestream.hitbtc.HitbtcStreamingExchange;
import info.bitrich.xchangestream.huobi.HuobiStreamingExchange;
import info.bitrich.xchangestream.kraken.KrakenStreamingExchange;
import info.bitrich.xchangestream.lgo.LgoStreamingExchange;
import info.bitrich.xchangestream.okcoin.OkCoinStreamingExchange;
import info.bitrich.xchangestream.poloniex2.PoloniexStreamingExchange;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.nodes.GroupMember;
import org.apache.curator.framework.recipes.watch.PersistentWatcher;
import org.apache.curator.utils.CloseableUtils;
import org.apache.zookeeper.WatchedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LeaderService {

    @Value("${backend.leader.interval.ms:5000}")
    private int interval;

    private final CuratorFramework client;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final ApplicationConfig config;

    private static final String BASE = "/backend/exchange";
    private final GroupMember groupMember;
    // log the status, but prevent it do it every X seconds.
    private Set<ExchangeEnum> exchangeslogged = new HashSet<>();

    private final ConcurrentHashMap<ExchangeEnum, ExchangeService> clients = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<ExchangeService, Semaphore> semaphores = new ConcurrentHashMap<>();

    public LeaderService(CuratorFramework client, KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper, ApplicationConfig config) {
        this.client = client;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.config = config;

        String id = "backend-" + UUID.randomUUID().toString();
        groupMember = new GroupMember(client, "/backend/leaders", id);
        groupMember.start();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        PersistentWatcher watcher = new PersistentWatcher(client, BASE, true);
        watcher.getListenable().addListener(this::process);
        watcher.start();

        Arrays.stream(ExchangeEnum.values()).forEach(e -> {
            if (exchangeFactory(e) != null) {
                String path = BASE + "/" + e.toString();
                Semaphore semaphore = new Semaphore(1, true);
                semaphore.acquireUninterruptibly();
                ExchangeService exchangeService = new ExchangeService(client, this, exchangeFactory(e),
                        kafkaTemplate, objectMapper, config, e, path, semaphore);
                semaphores.put(exchangeService, semaphore);
                clients.put(e, exchangeService);
                exchangeService.start();
            }
        });

        Observable.interval(interval, TimeUnit.MILLISECONDS).observeOn(Schedulers.io()).subscribe(e -> checkExchangeDistribution());
    }

    private void process(WatchedEvent event) {
        checkExchangeDistribution();
    }

    private void checkExchangeDistribution() {
        var exchangePerMember = ceiling(ExchangeEnum.values().length, groupMember.getCurrentMembers().keySet().size());
        var leaders = clients.values().stream().filter(ExchangeService::hasLeadership).map(ExchangeService::leaderOf).collect(Collectors.toSet());
        if (leaders.size() > exchangePerMember) {
            log.info("reschuffle needed : {} / {}", leaders.size(), exchangePerMember);
            // we must reschedule number of exchanges we should not have
            var toReschedule = leaders.size() - exchangePerMember;
            clients.values().stream().filter(ExchangeService::hasLeadership).limit(toReschedule).forEach(c -> semaphores.get(c).release());
        } else {
            if (!exchangeslogged.equals(leaders)) {
                exchangeslogged = leaders;
                log.info("Handling exchanges : {}", leaders);
            } else {
                log.debug("No reschuffle needed : {} / {}", leaders.size(), exchangePerMember);
            }
        }
    }

    /**
     * Integer version of A / B ceiling function.
     */
    int ceiling(int a, int b) {
        return a / b + ((a % b == 0) ? 0 : 1);
    }

    void acquire(ExchangeService client) {
        semaphores.get(client).acquireUninterruptibly();
    }

    @PreDestroy
    public void predestroy() {
        clients.values().forEach(ExchangeService::close);
        CloseableUtils.closeQuietly(groupMember);
    }

    StreamingExchange exchangeFactory(ExchangeEnum exchange) {
        return switch (exchange) {
            case BANKERA -> StreamingExchangeFactory.INSTANCE.createExchange(BankeraStreamingExchange.class);
            case BINANCE -> StreamingExchangeFactory.INSTANCE.createExchange(BinanceStreamingExchange.class);
            case BITFINEX -> StreamingExchangeFactory.INSTANCE.createExchange(BitfinexStreamingExchange.class);
            case BITMEX -> StreamingExchangeFactory.INSTANCE.createExchange(BitmexStreamingExchange.class);
            case BITSTAMP -> StreamingExchangeFactory.INSTANCE.createExchange(BitstampStreamingExchange.class);
            case BTCMARKETS -> StreamingExchangeFactory.INSTANCE.createExchange(BTCMarketsStreamingExchange.class);
            case CEXIO -> StreamingExchangeFactory.INSTANCE.createExchange(CexioStreamingExchange.class);
            case COINBASE-> StreamingExchangeFactory.INSTANCE.createExchange(CoinbaseProStreamingExchange.class);
            case COINJAR -> StreamingExchangeFactory.INSTANCE.createExchange(CoinjarStreamingExchange.class);
            case COINMATE -> StreamingExchangeFactory.INSTANCE.createExchange(CoinmateStreamingExchange.class);
            case GEMINI -> StreamingExchangeFactory.INSTANCE.createExchange(GeminiStreamingExchange.class);
            case HITBTC -> StreamingExchangeFactory.INSTANCE.createExchange(HitbtcStreamingExchange.class);
            case HUOBI -> StreamingExchangeFactory.INSTANCE.createExchange(HuobiStreamingExchange.class);
            case KRAKEN-> StreamingExchangeFactory.INSTANCE.createExchange(KrakenStreamingExchange.class);
            case LGO -> StreamingExchangeFactory.INSTANCE.createExchange(LgoStreamingExchange.class);
            case OKCOIN -> StreamingExchangeFactory.INSTANCE.createExchange(OkCoinStreamingExchange.class);
            case POLONIEX -> StreamingExchangeFactory.INSTANCE.createExchange(PoloniexStreamingExchange.class);
            default -> null;
        };
    }
}