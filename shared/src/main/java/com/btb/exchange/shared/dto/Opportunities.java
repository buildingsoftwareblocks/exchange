package com.btb.exchange.shared.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class Opportunities {
    long order;

    @JsonFormat(pattern = "HH:mm:ss.SSS")
    LocalTime timestamp;

    @Singular
    List<Opportunity> values;
}
