package io.scalecube.services.benchmarks;

import io.scalecube.services.Microservices;

import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

@State(Scope.Benchmark)
public class ServicesBenchmarksState {

  private Microservices seed;
  private Microservices node;
  private BenchmarkService service;

  @Setup
  public void setup() {
    seed = Microservices.builder().build().startAwait();
    node = Microservices.builder()
        .seeds(seed.cluster().address())
        .services(new BenchmarkServiceImpl())
        .build()
        .startAwait();
    System.err.println("Seed address: " + seed.cluster().address() +
        ", services address: " + node.serviceAddress() +
        ", seed serviceRegistry: " + seed.serviceRegistry().listServiceReferences());
    service = seed.call().create().api(BenchmarkService.class);
  }

  @TearDown
  public void tearDown() {
    if (node != null) {
      node.shutdown().block();
    }
    if (seed != null) {
      seed.shutdown().block();
    }
  }

  public Microservices seed() {
    return seed;
  }

  public Microservices node() {
    return node;
  }

  public BenchmarkService service() {
    return service;
  }
}
