package cn.edu.whut.sept.zuul;


import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
public class Api {

    @Test
    void PrintHello(){
        log.info("日志-hello");
        System.out.println("hello");
    }
}
