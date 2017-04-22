///**
// * Copyright 2015-2017 The OpenZipkin Authors
// *
// * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
// * in compliance with the License. You may obtain a copy of the License at
// *
// * http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software distributed under the License
// * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
// * or implied. See the License for the specific language governing permissions and limitations under
// * the License.
// */
//package com.example.bak;
//
//import zipkin.DependencyLink;
//import zipkin.Span;
//import zipkin.internal.DependencyLinker;
//import zipkin.storage.InMemorySpanStore;
//import zipkin.storage.QueryRequest;
//import zipkin.storage.SpanStore;
//import zipkin.storage.StorageAdapters;
//
//import java.lang.reflect.Field;
//import java.lang.reflect.InvocationTargetException;
//import java.lang.reflect.Method;
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.Collections;
//import java.util.HashSet;
//import java.util.List;
//
//import static zipkin.internal.GroupByTraceId.TRACE_DESCENDING;
//
//public final class LimitedInMemorySpanStore implements SpanStore {
//
////  private List<SpanStoreWrapper> wrapper = new ArrayList<>();
//  private List<InMemorySpanStore> memStores = new ArrayList<>();
//  private List<StorageAdapters.SpanConsumer> memStoresSan = new ArrayList<>();
//  private int totalStorageInst = 3;
//  private int maxStoragePerInst = 2;
//  private volatile int currentStorageIdx = 0;
//
//  Field spanConsumerField = InMemorySpanStore.class.getDeclaredField("spanConsumer");
//  Field acceptedSpanCountField = InMemorySpanStore.class.getDeclaredField("acceptedSpanCount");
//  Method clearMethod = InMemorySpanStore.class.getDeclaredMethod("clear", new Class[] {});
//
//  LimitedInMemorySpanStore() throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException {
//    spanConsumerField.setAccessible(true);
//    acceptedSpanCountField.setAccessible(true);
//    clearMethod.setAccessible(true);
//
//    for (int i = 0; i < totalStorageInst; i++) {
//      InMemorySpanStore inMemorySpanStore = new InMemorySpanStore();
//      memStores.add(inMemorySpanStore);
//
////      Field spanConsumerField = InMemorySpanStore.class.getDeclaredField("spanConsumer");
//      StorageAdapters.SpanConsumer spanConsumer = (StorageAdapters.SpanConsumer) spanConsumerField.get(inMemorySpanStore);
//
////      Field acceptedSpanCountField = InMemorySpanStore.class.getDeclaredField("acceptedSpanCount");
//      int spanCount = (int) acceptedSpanCountField.get(inMemorySpanStore);
//
////      Class[] argTypes = new Class[] {};
////      Method clearMethod = InMemorySpanStore.class.getDeclaredMethod("clear", argTypes);
////      clearMethod.invoke(inMemorySpanStore, null);
//
////      wrapper.add(new SpanStoreWrapper(inMemorySpanStore, spanConsumer, spanCount, m));
//    }
//  }
//
//  final StorageAdapters.SpanConsumer spanConsumer = new StorageAdapters.SpanConsumer() {
//    @Override public void accept(List<Span> spans) {
//      synchronized (LimitedInMemorySpanStore.this) {
////        SpanStoreWrapper storeWrapper = wrapper.get(currentStorageIdx);
////        InMemorySpanStore activeInst = storeWrapper.getInMemorySpanStore();
//        InMemorySpanStore activeInst = memStores.get(currentStorageIdx);
//
////        activeInst.spanConsumer.accept(spans);
////        if (activeInst.acceptedSpanCount > maxStoragePerInst) {
//
//
//        StorageAdapters.SpanConsumer spanConsumer = null;
//        try {
//          spanConsumer = (StorageAdapters.SpanConsumer) spanConsumerField.get(activeInst);
//          int spanCount = (int) acceptedSpanCountField.get(activeInst);
//
//          spanConsumer.accept(spans);
//          if (spanCount > maxStoragePerInst) {
//            currentStorageIdx = nextStorageIdx();
//  //          memStores.get(currentStorageIdx).clear();
//  //          wrapper.get(currentStorageIdx).getInMemorySpanStore().clear;
//            clearMethod.invoke(memStores.get(currentStorageIdx), null);
//            System.out.println("CLEARING NEXT DATA STORE");
//          }
//        } catch (IllegalAccessException | InvocationTargetException e) {
//          e.printStackTrace();
//          throw new RuntimeException(e);
//        }
//      }
//    }
//
//    @Override public String toString() {
//      return "LimitedInMemorySpanConsumer";
//    }
//  };
//
//  @Override
//  public List<List<Span>> getTraces(QueryRequest request) {
//    List<List<Span>> result = new ArrayList<>();
//    for (InMemorySpanStore memStore : memStores) {
//      result.addAll(memStore.getTraces(request));
//    }
//
//    Collections.sort(result, TRACE_DESCENDING);
//    return result;
//  }
//
//  @Override
//  public List<Span> getTrace(long traceIdHigh, long traceIdLow) {
//    for (InMemorySpanStore memStore : memStores) {
//      List<Span> trace = memStore.getTrace(traceIdHigh, traceIdLow);
//      if (trace != null) {
//        return trace;
//      }
//    }
//    return null;
//  }
//
//  @Override
//  public List<Span> getRawTrace(long traceIdHigh, long traceIdLow) {
//    for (InMemorySpanStore memStore : memStores) {
//      List<Span> trace = memStore.getRawTrace(traceIdHigh, traceIdLow);
//      if (trace != null) {
//        return trace;
//      }
//    }
//    return null;
//  }
//
//  @Override public List<Span> getTrace(long traceId) {
//    return getTrace(0L, traceId);
//  }
//
//  @Override public List<Span> getRawTrace(long traceId) {
//    return getRawTrace(0L, traceId);
//  }
//
//  @Override
//  public List<String> getServiceNames() {
//    HashSet<String> tmpResult = new HashSet<>();
//    for (InMemorySpanStore memStore : memStores) {
//      tmpResult.addAll(memStore.getServiceNames());
//    }
//    ArrayList<String> result = new ArrayList<>(tmpResult);
//    Collections.sort(result);
//    return result;
//  }
//
//  @Override
//  public List<String> getSpanNames(String serviceName) {
//    HashSet<String> tmpResult = new HashSet<>();
//    for (InMemorySpanStore memStore : memStores) {
//      tmpResult.addAll(memStore.getSpanNames(serviceName));
//    }
//    ArrayList<String> result = new ArrayList<>(tmpResult);
//    Collections.sort(result);
//    return result;
//  }
//
//  @Override
//  public List<DependencyLink> getDependencies(long endTs, Long lookback) {
//    QueryRequest request = QueryRequest.builder()
//            .endTs(endTs)
//            .lookback(lookback)
//            .limit(Integer.MAX_VALUE).build();
//
//    DependencyLinker linksBuilder = new DependencyLinker();
//    for (Collection<Span> trace : getTraces(request)) {
//      linksBuilder.putTrace(trace);
//    }
//    return linksBuilder.link();
//  }
//
//  private int nextStorageIdx() {
//    int nextInstIdx =  currentStorageIdx + 1;
//    if (nextInstIdx == totalStorageInst) {
//      nextInstIdx = 0;
//    }
//    return nextInstIdx;
//  }
//
////  int calcAcceptedSpanCount() {
////    int acceptedSpanCount = 0;
////    for (InMemorySpanStore memStore : memStores) {
////      acceptedSpanCount = acceptedSpanCount + memStore.acceptedSpanCount;
////    }
////    return acceptedSpanCount;
////  }
////
////  protected void clear() {
////    for (InMemorySpanStore memStore : memStores) {
////      memStore.clear();
////    }
////  }
//
////  class SpanStoreWrapper {
////    private InMemorySpanStore inMemorySpanStore;
////    private StorageAdapters.SpanConsumer spanConsumer;
////    private int spanCount;
////
////    public SpanStoreWrapper(InMemorySpanStore inMemorySpanStore, StorageAdapters.SpanConsumer spanConsumer, int spanCount) {
////      this.inMemorySpanStore = inMemorySpanStore;
////      this.spanConsumer = spanConsumer;
////      this.spanCount = spanCount;
////    }
////
////    public InMemorySpanStore getInMemorySpanStore() {
////      return inMemorySpanStore;
////    }
////
////    public StorageAdapters.SpanConsumer getSpanConsumer() {
////      return spanConsumer;
////    }
////
////    public int getSpanCount() {
////      return spanCount;
////    }
////  }
//}
