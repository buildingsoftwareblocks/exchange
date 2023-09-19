package config;

import com.btb.exchange.shared.annotation.EnableCommonComponents;
import lombok.Generated;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.hazelcast.HazelcastAutoConfiguration;
import org.springframework.cloud.config.server.EnableConfigServer;

@SpringBootApplication(exclude = {HazelcastAutoConfiguration.class})
@EnableConfigServer
@EnableCommonComponents
@Generated // prevent JoCoCo from complaining
public class ConfigApplication {

  public static void main(String[] args) {
    SpringApplication.run(ConfigApplication.class, args);
  }
}
