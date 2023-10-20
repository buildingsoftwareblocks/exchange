package config;

import com.btb.exchange.shared.annotation.EnableCommonComponents;
import com.btb.exchange.shared.utils.CommonApplication;
import lombok.Generated;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

@SpringBootApplication
@EnableCommonComponents
@EnableConfigServer
@Generated // prevent JoCoCo from complaining
public class ConfigApplication {

    public static void main(String[] args) {
        CommonApplication.run(ConfigApplication.class, args);
    }
}
