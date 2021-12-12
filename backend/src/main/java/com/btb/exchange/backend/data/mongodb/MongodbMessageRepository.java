package com.btb.exchange.backend.data.mongodb;

import org.springframework.data.repository.reactive.RxJava2SortingRepository;
import org.springframework.stereotype.Repository;

@Repository
interface MongodbMessageRepository extends RxJava2SortingRepository<Message, String> {
}
