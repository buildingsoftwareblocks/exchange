package com.btb.exchange.frontend;

import com.btb.exchange.shared.annotation.EnableCommonComponents;
import com.btb.exchange.shared.utils.CommonApplication;
import lombok.Generated;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableCommonComponents
@Generated // prevent JoCoCo from complaining
public class FrontendApplication {

    public static void main(String[] args) {
        CommonApplication.run(FrontendApplication.class, args);
    }
}
