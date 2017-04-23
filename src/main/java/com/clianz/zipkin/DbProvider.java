package com.clianz.zipkin;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class DbProvider {

    @FunctionalInterface
    public interface CouchDbProvider {
        Database getDb();
    }

    @Bean
    public CouchDbProvider mydb(CloudantClient cloudant) {
        // TODO: Rotate DB to cleanup.
        return () -> cloudant.database("zipkin", true);
    }
}
