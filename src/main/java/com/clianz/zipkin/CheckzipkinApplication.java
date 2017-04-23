package com.clianz.zipkin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import zipkin.Span;
import zipkin.server.EnableZipkinServer;
import zipkin.storage.QueryRequest;
import zipkin.storage.SpanStore;

import java.util.List;

@SpringBootApplication
@EnableZipkinServer
@RestController
public class CheckzipkinApplication {

    @Autowired
    SpanStore spanStore;

    public static void main(String[] args) {
        SpringApplication.run(CheckzipkinApplication.class, args);
    }

    // Debug helper
    @GetMapping("/getTraces")
    public String debugGetTraces() {
        List<List<Span>> traces = spanStore.getTraces(QueryRequest.builder()
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
        return spanStore.getServiceNames()
                .toString();
    }

    @GetMapping("/getSpanNames")
    public String debugGetSpanNames() {
        return spanStore.getSpanNames("testsleuthzipkin")
                .toString();
    }

    @GetMapping("/getRawTrace")
    public String debugGetRawTrace() {
        return spanStore.getRawTrace(0, -8131454385781382000L)
                .toString();
    }
}
