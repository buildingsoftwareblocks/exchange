package com.btb.exchange.backend.data.es;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
interface ESMessageRepository extends ElasticsearchRepository<Message, String> {}
