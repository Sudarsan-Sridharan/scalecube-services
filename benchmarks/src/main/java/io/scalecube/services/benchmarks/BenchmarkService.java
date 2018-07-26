package io.scalecube.services.benchmarks;

import io.scalecube.services.annotations.Service;
import io.scalecube.services.annotations.ServiceMethod;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public interface BenchmarkService {

  BenchmarkMessage MESSAGE = new BenchmarkMessage("benchmarkMessage");

  @ServiceMethod
  Mono<Void> fireAndForget0();

  @ServiceMethod
  Mono<Void> oneWay(BenchmarkMessage request);

  @ServiceMethod
  Mono<BenchmarkMessage> requestOne(BenchmarkMessage request);

  @ServiceMethod
  Flux<BenchmarkMessage> requestMany(BenchmarkMessage request);
}
