package com.btb.exchange.analysis;

import com.btb.exchange.shared.annotation.EnableCommonComponents;
import com.btb.exchange.shared.utils.CommonApplication;
import lombok.Generated;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableCommonComponents
@EnableAsync
@Generated // prevent JoCoCo from complaining
public class AnalysisApplication {

    public static void main(String[] args) {
        CommonApplication.run(AnalysisApplication.class, args);
    }
}
