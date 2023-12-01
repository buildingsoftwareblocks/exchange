package com.btb.exchange.backend.data.mongodb;

import org.springframework.data.repository.reactive.RxJava3CrudRepository;
import org.springframework.data.repository.reactive.RxJava3SortingRepository;
import org.springframework.stereotype.Repository;

@Repository
interface MongodbMessageRepository
        extends RxJava3SortingRepository<Message, String>, RxJava3CrudRepository<Message, String> {
}
