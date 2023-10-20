package com.btb.exchange.shared.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.text.CharSequenceLength.hasLength;

import org.junit.jupiter.api.Test;

class IDUtilsTest {

    @Test
    void generateId() {
        String id = IDUtils.generateID();
        assertThat(id, is(notNullValue()));
        assertThat(id, hasLength(3));
    }
}
