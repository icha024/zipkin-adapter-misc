package com.clianz.zipkin;

import com.clianz.zipkin.inmemory.DbProvider;
import com.cloudant.client.api.views.Key;
import com.cloudant.client.api.views.ViewResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import zipkin.DependencyLink;
import zipkin.Span;
import zipkin.internal.CorrectForClockSkew;
import zipkin.internal.GroupByTraceId;
import zipkin.internal.MergeById;
import zipkin.storage.QueryRequest;
import zipkin.storage.SpanStore;
import zipkin.storage.StorageAdapters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static zipkin.internal.GroupByTraceId.TRACE_DESCENDING;

//@Component
@RestController
public final class CouchDbSpanStore implements SpanStore {

    Logger log = LoggerFactory.getLogger(this.getClass());

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    DbProvider.CouchDbProvider dbProvider;

    final StorageAdapters.SpanConsumer spanConsumer = new StorageAdapters.SpanConsumer() {
        @Override
        public void accept(List<Span> spans) {
            dbProvider.getDb().bulk(spans);

//            synchronized (LimitedInMemorySpanStore.this) {
//                InMemorySpanStore activeInst = memStores.get(currentStorageIdx);
//                activeInst.spanConsumer.accept(spans);
//                if (activeInst.acceptedSpanCount > maxStoragePerInst) {
//                    currentStorageIdx = nextStorageIdx();
//                    memStores.get(currentStorageIdx).clear();
//                    // System.out.println("CLEARING NEXT DATA STORE");
//                }
//            }
        }

        @Override
        public String toString() {
            return "CouchDbSpanConsumer";
        }
    };

    @Override
    public List<List<Span>> getTraces(QueryRequest request) {
        try {
            ViewResponse<Key.ComplexKey, Object> searchByTime = dbProvider.getDb()
                    .getViewRequestBuilder("search", "traceid-by-time")
                    .newRequest(Key.Type.COMPLEX, Object.class).groupLevel(1)
//                    .startKey(new Key.complex())
                    .limit(request.limit).build().getResponse();
            List<String> traceIds = searchByTime.getKeys().stream().map(complexKey -> {
                String keyJsonStr = complexKey.toJson();
                String[] strArr = new String[0];
                try {
                    strArr = objectMapper.readValue(keyJsonStr, String[].class);
                    if (strArr.length > 0) {
                        return strArr[0];
                    }
                } catch (IOException e) {
                    log.error("can not parse arr", e);
                }
                return "";
            }).collect(Collectors.toList());
            log.info("Got traceIds: {}", traceIds);
            List<Span> spans = dbProvider.getDb()
                    .getViewRequestBuilder("search", "span-by-traceid")
                    .newRequest(Key.Type.STRING, Span.class).descending(true).reduce(false)
                    .build().getResponse().getValues();

            List<List<Span>> result = new ArrayList<>();
            // FIXME: non-strict id match only
            for (List<Span> next : GroupByTraceId.apply(spans, false, true)) {
                if (request.test(next)) {
                    result.add(next);
                }
            }
            result.sort(TRACE_DESCENDING);
            log.info("getTraces result: {}", result);
            return result;
        } catch (IOException e) {
            log.error("Error getting traces", e);
        }
        return null;
    }

    @Override
    public List<Span> getTrace(long traceId) {
        return getTrace(0L, traceId);
    }

    @Override
    public List<Span> getTrace(long traceIdHigh, long traceIdLow) {
        List<Span> result = getRawTrace(traceIdHigh, traceIdLow);
        if (result == null) return null;
        return CorrectForClockSkew.apply(MergeById.apply(result));
    }

    @Override
    public List<Span> getRawTrace(long traceId) {
        return getRawTrace(0L, traceId);
    }

    @Override
    public List<Span> getRawTrace(long traceIdHigh, long traceId) {
//        List<Span> spans = (List<Span>) traceIdToSpans.get(traceId);
//        if (spans == null || spans.isEmpty()) return null;
//        if (!strictTraceId) return sortedList(spans);
//
//        List<Span> filtered = new ArrayList<>(spans);
//        Iterator<Span> iterator = filtered.iterator();
//        while (iterator.hasNext()) {
//            if (iterator.next().traceIdHigh != traceIdHigh) {
//                iterator.remove();
//            }
//        }
//        return filtered.isEmpty() ? null : filtered;
        return null;
    }

    @Override
    public List<String> getServiceNames() {
        try {
            List<String> serviceNames = dbProvider.getDb()
                    .getViewRequestBuilder("search", "service-names")
                    .newRequest(Key.Type.STRING, Object.class).group(true)
                    .build().getResponse().getKeys();
            log.info("Service name: {}", serviceNames);
            return serviceNames;
        } catch (IOException e) {
            log.error("Error getServiceNames", e);
        }
        return null;
    }

    @Override
    public List<String> getSpanNames(String serviceName) {
        try {
            List<String> spanNames = dbProvider.getDb()
                    .getViewRequestBuilder("search", "service-span-names")
                    .newRequest(Key.Type.COMPLEX, Object.class).group(true)
                    .build().getResponse().getKeys().stream().map(complexKey -> {
                        String ckJsonStr = complexKey.toJson();
                        log.info("complexKey: {}", complexKey.toJson());
                        try {
                            String[] strArr = objectMapper.readValue(ckJsonStr, String[].class);
                            return strArr;
                        } catch (IOException e) {
                            log.error("Error getSpanNames", e);
                        }
                        return new String[]{};
                    }).filter(keyArr -> keyArr.length > 1 && keyArr[0].equals(serviceName))
                    .map(str -> str[1]).collect(Collectors.toList());
            log.info("Span name: {}", spanNames);
            return spanNames;
        } catch (IOException e) {
            log.error("Error getSpanNames", e);
        }
        return null;
    }

    @Override
    public List<DependencyLink> getDependencies(long endTs, Long lookback) {
        return null;
    }

    // DEBUG:
    @GetMapping("/getTraces")
    public String debugGetTraces() {
        getTraces(QueryRequest.builder().limit(100).build());
        return "pong";
    }

    @GetMapping("/getServiceNames")
    public String debugGetServiceNames() {
        return getServiceNames().toString();
    }

    @GetMapping("/getSpanNames")
    public String debugGetSpanNames() {
        return getSpanNames("testsleuthzipkin").toString();
    }
}
