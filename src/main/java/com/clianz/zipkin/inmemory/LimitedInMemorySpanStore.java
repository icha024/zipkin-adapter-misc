package com.clianz.zipkin.inmemory;

import com.clianz.zipkin.fork.InMemorySpanStore;
import zipkin.DependencyLink;
import zipkin.Span;
import zipkin.internal.DependencyLinker;
import zipkin.storage.QueryRequest;
import zipkin.storage.SpanStore;
import zipkin.storage.StorageAdapters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static zipkin.internal.GroupByTraceId.TRACE_DESCENDING;

public final class LimitedInMemorySpanStore implements SpanStore {

    private List<InMemorySpanStore> memStores = new ArrayList<>();
    private int totalStorageInst = 3;
    private int maxStoragePerInst = 2;
    private volatile int currentStorageIdx = 0;

    LimitedInMemorySpanStore(boolean strictTraceId) {
        for (int i = 0; i < totalStorageInst; i++) {
            memStores.add(new InMemorySpanStore(strictTraceId));
        }
    }

    final StorageAdapters.SpanConsumer spanConsumer = new StorageAdapters.SpanConsumer() {
        @Override
        public void accept(List<Span> spans) {
            synchronized (LimitedInMemorySpanStore.this) {
                InMemorySpanStore activeInst = memStores.get(currentStorageIdx);
                activeInst.spanConsumer.accept(spans);
                if (activeInst.acceptedSpanCount > maxStoragePerInst) {
                    currentStorageIdx = nextStorageIdx();
                    memStores.get(currentStorageIdx).clear();
                    // System.out.println("CLEARING NEXT DATA STORE");
                }
            }
        }

        @Override
        public String toString() {
            return "LimitedInMemorySpanConsumer";
        }
    };

    @Override
    public List<List<Span>> getTraces(QueryRequest request) {
        List<List<Span>> result = new ArrayList<>();
        for (InMemorySpanStore memStore : memStores) {
            result.addAll(memStore.getTraces(request));
        }

        result.sort(TRACE_DESCENDING);
        return result;
    }

    @Override
    public List<Span> getTrace(long traceIdHigh, long traceIdLow) {
        for (InMemorySpanStore memStore : memStores) {
            List<Span> trace = memStore.getTrace(traceIdHigh, traceIdLow);
            if (trace != null) {
                return trace;
            }
        }
        return null;
    }

    @Override
    public List<Span> getRawTrace(long traceIdHigh, long traceIdLow) {
        for (InMemorySpanStore memStore : memStores) {
            List<Span> trace = memStore.getRawTrace(traceIdHigh, traceIdLow);
            if (trace != null) {
                return trace;
            }
        }
        return null;
    }

    @Override
    public List<Span> getTrace(long traceId) {
        return getTrace(0L, traceId);
    }

    @Override
    public List<Span> getRawTrace(long traceId) {
        return getRawTrace(0L, traceId);
    }

    @Override
    public List<String> getServiceNames() {
        HashSet<String> tmpResult = new HashSet<>();
        for (InMemorySpanStore memStore : memStores) {
            tmpResult.addAll(memStore.getServiceNames());
        }
        ArrayList<String> result = new ArrayList<>(tmpResult);
        Collections.sort(result);
        return result;
    }

    @Override
    public List<String> getSpanNames(String serviceName) {
        HashSet<String> tmpResult = new HashSet<>();
        for (InMemorySpanStore memStore : memStores) {
            tmpResult.addAll(memStore.getSpanNames(serviceName));
        }
        ArrayList<String> result = new ArrayList<>(tmpResult);
        Collections.sort(result);
        return result;
    }

    @Override
    public List<DependencyLink> getDependencies(long endTs, Long lookback) {
        QueryRequest request = QueryRequest.builder()
                .endTs(endTs)
                .lookback(lookback)
                .limit(Integer.MAX_VALUE).build();

        DependencyLinker linksBuilder = new DependencyLinker();
        for (Collection<Span> trace : getTraces(request)) {
            linksBuilder.putTrace(trace);
        }
        return linksBuilder.link();
    }

    private int nextStorageIdx() {
        int nextInstIdx = currentStorageIdx + 1;
        if (nextInstIdx == totalStorageInst) {
            nextInstIdx = 0;
        }
        return nextInstIdx;
    }
}
