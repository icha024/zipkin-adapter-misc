package com.clianz.zipkin.couchdb;

import com.cloudant.client.api.views.Key;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import zipkin.DependencyLink;
import zipkin.Span;
import zipkin.internal.CorrectForClockSkew;
import zipkin.internal.DependencyLinker;
import zipkin.internal.GroupByTraceId;
import zipkin.internal.MergeById;
import zipkin.storage.QueryRequest;
import zipkin.storage.SpanStore;
import zipkin.storage.StorageAdapters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static zipkin.internal.GroupByTraceId.TRACE_DESCENDING;

@Component
public final class CouchDbSpanStore implements SpanStore {

    private static final Logger log = LoggerFactory.getLogger(CouchDbSpanStore.class);
    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private CouchDbProvider.DbInstanceProvider dbProvider;

    private final StorageAdapters.SpanConsumer spanConsumer = new StorageAdapters.SpanConsumer() {
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
        log.debug("getTraces endTs: {}", request.endTs);
        try {
            List<Long> searchByTime = dbProvider.getDb()
                    .getViewRequestBuilder("search", "traceid-by-time")
                    .newRequest(Key.Type.NUMBER, Long.class)
                    .startKey(request.endTs * 1000)
                    .descending(true)
                    .limit(request.limit * 10) // Best effort, hopefully got enough trace.
                    .build()
                    .getResponse()
                    .getValues();
            log.debug("Got searchByTime: {}", searchByTime);

            Set<Long> traceIdsSet = new LinkedHashSet<>(searchByTime);
            Long[] traceIdsArr = traceIdsSet.toArray(new Long[traceIdsSet.size()]);
            if (traceIdsArr.length > request.limit) {
                traceIdsArr = Arrays.copyOf(traceIdsArr, request.limit);
            }
            if (log.isDebugEnabled()) {
                log.debug("Got traceIds: {}", Arrays.asList(traceIdsArr));
            }

            List<Span> spans = dbProvider.getDb()
                    .getViewRequestBuilder("search", "span-by-traceid")
                    .newRequest(Key.Type.NUMBER, Span.class)
                    .keys(traceIdsArr)
                    .build()
                    .getResponse()
                    .getValues();

            List<List<Span>> result = new ArrayList<>();
            // non-strict id match only
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
            return new ArrayList<>();
        }
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
                    .keys(new Long[]{traceId})
                    .build()
                    .getResponse()
                    .getValues();
        } catch (IOException e) {
            log.error("Error getting trace", e);
            return null;
        }
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
            return new ArrayList<>();
        }
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
            return new ArrayList<>();
        }
    }

    @Override
    public List<DependencyLink> getDependencies(long endTs, Long lookback) {
        log.debug("getDependencies endTs: {}, loopback", endTs, lookback);
        QueryRequest request = QueryRequest.builder()
                .endTs(endTs)
                .lookback(lookback)
                .limit(100)
                .build();

        DependencyLinker linksBuilder = new DependencyLinker();
        for (Collection<Span> trace : getTraces(request)) {
            linksBuilder.putTrace(trace);
        }
        return linksBuilder.link();
    }

    protected StorageAdapters.SpanConsumer getSpanConsumer() {
        return spanConsumer;
    }

    private <T> T[] complexKeyToArray(Key.ComplexKey ck, Class<T[]> valueType) {
        try {
            String keyJsonStr = ck.toJson();
            return objectMapper.readValue(keyJsonStr, valueType);
        } catch (IOException e) {
            log.error("Can not parse JSON complex key", e);
            return (T[]) new Object[]{};
        }
    }
}
