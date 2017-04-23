package com.clianz.zipkin;

import com.clianz.zipkin.inmemory.DbProvider;
import com.cloudant.client.api.views.Key;
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
import java.util.List;
import java.util.stream.Collectors;

import static zipkin.internal.GroupByTraceId.TRACE_DESCENDING;

//@Component
@RestController
public final class CouchDbSpanStore implements SpanStore {

    private static final Logger log = LoggerFactory.getLogger(CouchDbSpanStore.class);

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private DbProvider.CouchDbProvider dbProvider;

    final StorageAdapters.SpanConsumer spanConsumer = new StorageAdapters.SpanConsumer() {
        @Override
        public void accept(List<Span> spans) {
            dbProvider.getDb()
                    .bulk(spans);
        }

        @Override
        public String toString() {
            return "CouchDbSpanConsumer";
        }
    };

    @Override
    public List<List<Span>> getTraces(QueryRequest request) {
        try {
            List<Key.ComplexKey> searchByTime = dbProvider.getDb()
                    .getViewRequestBuilder("search", "traceid-by-time")
                    .newRequest(Key.Type.COMPLEX, Object.class)
                    .groupLevel(1)
                    .endKey(Key.complex(request.endTs * 1000))
                    .descending(true)
                    .limit(request.limit)
                    .build()
                    .getResponse()
                    .getKeys();
            log.debug("Got searchByTime: {}", searchByTime);

            List<Long> traceIds = searchByTime.stream()
                    .map(complexKey -> complexKeyToArray(complexKey, Long[].class))
                    .filter(s -> s.length > 0)
                    .map(strings -> strings[0])
                    .collect(Collectors.toList());
            log.debug("Got traceIds: {}", traceIds);

            List<Span> spans = dbProvider.getDb()
                    .getViewRequestBuilder("search", "span-by-traceid")
                    .newRequest(Key.Type.NUMBER, Span.class)
                    .keys(traceIds.toArray(new Long[traceIds.size()]))
                    .descending(true)
                    .reduce(false)
                    .build()
                    .getResponse()
                    .getValues();

            List<List<Span>> result = new ArrayList<>();
            // FIXME: non-strict id match only
            for (List<Span> next : GroupByTraceId.apply(spans, false, true)) {
                if (request.test(next)) {
                    result.add(next);
                }
            }
            result.sort(TRACE_DESCENDING);
            log.debug("getTraces result: {}", result);
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
        try {
            return dbProvider.getDb()
                    .getViewRequestBuilder("search", "span-by-traceid")
                    .newRequest(Key.Type.NUMBER, Span.class)
                    .descending(false)
                    .keys(new Long[]{traceId})
                    .build()
                    .getResponse()
                    .getValues();
        } catch (IOException e) {
            log.error("Error getting trace", e);
        }
        return null;
    }

    @Override
    public List<String> getServiceNames() {
        try {
            List<String> serviceNames = dbProvider.getDb()
                    .getViewRequestBuilder("search", "service-names")
                    .newRequest(Key.Type.STRING, Object.class)
                    .group(true)
                    .build()
                    .getResponse()
                    .getKeys();
            log.debug("Service name: {}", serviceNames);
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
                    .newRequest(Key.Type.COMPLEX, Object.class)
                    .group(true)
                    .build()
                    .getResponse()
                    .getKeys()
                    .stream()
                    .map(complexKey -> complexKeyToArray(complexKey, String[].class))
                    .filter(keyArr -> keyArr.length > 1 && keyArr[0].equals(serviceName))
                    .map(str -> str[1])
                    .collect(Collectors.toList());
            log.debug("Span name: {}", spanNames);
            return spanNames;
        } catch (IOException e) {
            log.error("Error getSpanNames", e);
        }
        return null;
    }

    @Override
    public List<DependencyLink> getDependencies(long endTs, Long lookback) {
        log.warn("getDependencies not implemented");
        return null;
    }

    private <T> T[] complexKeyToArray(Key.ComplexKey ck, Class<T[]> valueType) {
        try {
            String keyJsonStr = ck.toJson();
            return objectMapper.readValue(keyJsonStr, valueType);
        } catch (IOException e) {
            log.error("Can not parse JSON complex key", e);
        }
        return (T[]) new Object[]{};
    }


    // DEBUG:
    @GetMapping("/getTraces")
    public String debugGetTraces() {
        List<List<Span>> traces = getTraces(QueryRequest.builder()
                .limit(100)
                .endTs(System.currentTimeMillis())
                .build());
        if (traces != null) {
            return traces.toString();
        }
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

    @GetMapping("/getRawTrace")
    public String debugGetRawTrace() {
        return getRawTrace(0, -8131454385781382000L).toString();
    }
}
