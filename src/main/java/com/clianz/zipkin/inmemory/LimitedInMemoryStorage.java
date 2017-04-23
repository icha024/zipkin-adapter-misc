package com.clianz.zipkin.inmemory;

import zipkin.storage.AsyncSpanConsumer;
import zipkin.storage.AsyncSpanStore;
import zipkin.storage.StorageComponent;

import static zipkin.storage.StorageAdapters.blockingToAsync;

public final class LimitedInMemoryStorage implements StorageComponent {

    private final LimitedInMemorySpanStore spanStore;
    private final AsyncSpanStore asyncSpanStore;
    private final AsyncSpanConsumer asyncConsumer;

    public LimitedInMemoryStorage(boolean strictTraceId) {
        spanStore = new LimitedInMemorySpanStore(strictTraceId);
        asyncSpanStore = blockingToAsync(spanStore, Runnable::run);
        asyncConsumer = blockingToAsync(spanStore.spanConsumer, Runnable::run);
    }

    @Override
    public LimitedInMemorySpanStore spanStore() {
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
