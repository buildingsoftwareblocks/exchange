package com.btb.exchange.shared.annotation;

import com.btb.exchange.shared.CommonBase;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(CommonBase.class)
public @interface EnableCommonComponents {}