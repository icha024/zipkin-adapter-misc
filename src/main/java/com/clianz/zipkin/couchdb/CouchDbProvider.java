package com.clianz.zipkin.couchdb;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class CouchDbProvider {

    @FunctionalInterface
    public interface DbInstanceProvider {
        Database getDb();
    }

    @Bean
    public DbInstanceProvider mydb(CloudantClient cloudant) {
        // TODO: Rotate DB to cleanup.
        return () -> cloudant.database("zipkin", true);
    }
}
