///**
// * Copyright 2015-2017 The OpenZipkin Authors
// * <p>
// * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
// * in compliance with the License. You may obtain a copy of the License at
// * <p>
// * http://www.apache.org/licenses/LICENSE-2.0
// * <p>
// * Unless required by applicable law or agreed to in writing, software distributed under the License
// * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
// * or implied. See the License for the specific language governing permissions and limitations under
// * the License.
// */
//package com.example.bak;
//
//import zipkin.storage.AsyncSpanConsumer;
//import zipkin.storage.AsyncSpanStore;
//import zipkin.storage.StorageAdapters;
//import zipkin.storage.StorageComponent;
//
//import static zipkin.storage.StorageAdapters.blockingToAsync;
//
///**
// * Test storage component that keeps all spans in memory, accepting them on the calling thread.
// */
//public final class LimitedInMemoryStorage implements StorageComponent {
//
////  public static Builder builder() {
////    return new Builder();
////  }
//
////  public static final class Builder implements StorageComponent.Builder {
////    boolean strictTraceId = true;
////
////    /** {@inheritDoc} */
////    @Override public Builder strictTraceId(boolean strictTraceId) {
////      this.strictTraceId = strictTraceId;
////      return this;
////    }
////
////    @Override
////    public LimitedInMemoryStorage build() {
////      return new LimitedInMemoryStorage(this);
////    }
////  }
//
//    final LimitedInMemorySpanStore spanStore;
//    final AsyncSpanStore asyncSpanStore;
//    final AsyncSpanConsumer asyncConsumer;
//
//    // Historical constructor
////  public LimitedInMemoryStorage() {
////    this(new Builder());
////  }
//
//    LimitedInMemoryStorage() {
//        try {
//            spanStore = new LimitedInMemorySpanStore();
//        } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException e) {
//            e.printStackTrace();
//            throw new RuntimeException("Can not access private field.", e);
//        }
//        asyncSpanStore = blockingToAsync(spanStore, Runnable::run);
//        asyncConsumer = blockingToAsync(spanStore.spanConsumer, Runnable::run);
//    }
//
//    @Override
//    public LimitedInMemorySpanStore spanStore() {
//        return spanStore;
//    }
//
//    @Override
//    public AsyncSpanStore asyncSpanStore() {
//        return asyncSpanStore;
//    }
//
//    public StorageAdapters.SpanConsumer spanConsumer() {
//        return spanStore.spanConsumer;
//    }
//
//    @Override
//    public AsyncSpanConsumer asyncSpanConsumer() {
//        return asyncConsumer;
//    }
//
////  public void clear() {
////    spanStore.clear();
////  }
//
////  public int acceptedSpanCount() {
//
////    return spanStore.acceptedSpanCount;
////  }
////  public int acceptedSpanCount() {
////  return spanStore.calcAcceptedSpanCount();
////}
//
//    @Override
//    public CheckResult check() {
//        return CheckResult.OK;
//    }
//
//    @Override
//    public void close() {
//    }
//}
