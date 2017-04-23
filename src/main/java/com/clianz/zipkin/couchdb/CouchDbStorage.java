package com.clianz.zipkin.couchdb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import zipkin.storage.AsyncSpanConsumer;
import zipkin.storage.AsyncSpanStore;
import zipkin.storage.StorageComponent;

import static zipkin.storage.StorageAdapters.blockingToAsync;

@Component
public final class CouchDbStorage implements StorageComponent {

    private final CouchDbSpanStore spanStore;
    private final AsyncSpanStore asyncSpanStore;
    private final AsyncSpanConsumer asyncConsumer;

    @Autowired
    public CouchDbStorage(CouchDbSpanStore spanStore) {
        this.spanStore = spanStore;
        this.asyncSpanStore = blockingToAsync(spanStore, Runnable::run);
        this.asyncConsumer = blockingToAsync(spanStore.getSpanConsumer(), Runnable::run);
    }

    @Override
    public CouchDbSpanStore spanStore() {
        return spanStore;
    }

    @Override
    public AsyncSpanStore asyncSpanStore() {
        return asyncSpanStore;
    }

    @Override
    public AsyncSpanConsumer asyncSpanConsumer() {
        return asyncConsumer;
    }

    @Override
    public CheckResult check() {
        return CheckResult.OK;
    }

    @Override
    public void close() {
    }
}
