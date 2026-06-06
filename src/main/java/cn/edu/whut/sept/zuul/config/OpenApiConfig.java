package cn.edu.whut.sept.zuul.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI zuulOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("World of Zuul: Uncertain Fate API")
                        .version("v1.0")
                        .description("阶段 2 接口契约：统一游戏状态快照、玩家动作、存档与读档接口。")
                        .contact(new Contact().name("kai-fa-galgamer team")));
    }
}
