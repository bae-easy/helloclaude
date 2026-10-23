package com.example.board;

import com.example.board.support.TestcontainersConfiguration;
import org.springframework.boot.SpringApplication;

/**
 * 로컬 DB 없이 Testcontainers PostgreSQL로 앱을 띄울 때 사용한다 (IntelliJ에서 이 main 실행).
 */
public class TestBoardApplication {

    public static void main(String[] args) {
        SpringApplication.from(BoardApplication::main).with(TestcontainersConfiguration.class).run(args);
    }
}
