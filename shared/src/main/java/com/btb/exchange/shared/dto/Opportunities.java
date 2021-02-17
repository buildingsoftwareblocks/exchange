package com.btb.exchange.shared.dto;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@Jacksonized
public class Opportunities {
    long order;
    LocalTime timestamp;
    @Singular
    List<Opportunity> values;
}
