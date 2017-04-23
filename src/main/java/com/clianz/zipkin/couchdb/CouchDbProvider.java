package com.clianz.zipkin.couchdb;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.DesignDocumentManager;
import com.cloudant.client.api.model.DesignDocument;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;

@Component
public class CouchDbProvider {

    private Database zipkinDb;

    @FunctionalInterface
    public interface DbInstanceProvider {
        Database getDb();
    }

    @Bean
    public DbInstanceProvider mydb(CloudantClient cloudant) throws FileNotFoundException {
        zipkinDb = createAndGetDb(cloudant);
        return () -> zipkinDb;
    }

    private Database createAndGetDb(CloudantClient cloudant) throws FileNotFoundException {
        // TODO: Rotate DB to cleanup.
        Database db = cloudant.database("zipkin", true);
        putDesignDoc(db);
        return db;
    }

    private void putDesignDoc(Database db) throws FileNotFoundException {
        File file = ResourceUtils.getFile("classpath:zipkin-view.js");
        DesignDocument designDocument = DesignDocumentManager.fromFile(file);
        DesignDocumentManager designDocumentManager = db.getDesignDocumentManager();
        designDocumentManager.put(designDocument);
    }
}
