package com.btb.exchange.backend.data;

import org.springframework.data.repository.reactive.RxJava2SortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends RxJava2SortingRepository<Message, String> {
}
