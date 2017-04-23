package com.clianz.zipkin;

import com.clianz.zipkin.inmemory.LimitedInMemoryStorage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import zipkin.server.EnableZipkinServer;
import zipkin.storage.StorageComponent;

@SpringBootApplication
@EnableZipkinServer
public class CheckzipkinApplication {

    public static void main(String[] args) {
        SpringApplication.run(CheckzipkinApplication.class, args);
    }

//    @Bean
//    public StorageComponent storage() {
//        return new LimitedInMemoryStorage(false);
//    }
}
