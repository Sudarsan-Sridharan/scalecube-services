package io.scalecube.services;

import io.scalecube.cluster.ClusterConfig;
import io.scalecube.cluster.ClusterConfig.Builder;
import io.scalecube.services.a.b.testing.CanaryService;
import io.scalecube.services.a.b.testing.CanaryTestingRouter;
import io.scalecube.services.a.b.testing.GreetingServiceImplA;
import io.scalecube.services.a.b.testing.GreetingServiceImplB;
import io.scalecube.testlib.BaseTest;
import org.junit.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RemoteServiceTest extends BaseTest {

  private static AtomicInteger port = new AtomicInteger(4000);

  @Test
  public void test_remote_service_tags() throws InterruptedException, ExecutionException {
    Microservices gateway = Microservices.builder()
        .port(port.incrementAndGet())
        .build();

    Microservices services1 = Microservices.builder()
        .port(port.incrementAndGet())
        .seeds(gateway.cluster().address())
        .services().service(new GreetingServiceImplA()).tag("Weight", "0.3").add()
        .build()
        .build();

    Microservices services2 = Microservices.builder()
        .port(port.incrementAndGet())
        .seeds(gateway.cluster().address())
        .services().service(new GreetingServiceImplB()).tag("Weight", "0.7").add()
        .build()
        .build();

    CanaryService service = gateway.call()
        .router(gateway.router(CanaryTestingRouter.class))
        .api(CanaryService.class);

    Util.sleep(1000);

    AtomicInteger count = new AtomicInteger(0);
    AtomicInteger responses = new AtomicInteger(0);
    CountDownLatch timeLatch = new CountDownLatch(1);
    for (int i = 0; i < 100; i++) {
      service.greeting("joe").whenComplete((success, error) -> {
        responses.incrementAndGet();
        if (success.startsWith("B")) {
          count.incrementAndGet();
          if ((responses.get() == 100) && (60 < count.get() && count.get() < 80)) {
            timeLatch.countDown();
          }
        }
      });
    }

    services2.shutdown().get();
    services1.shutdown().get();
    gateway.shutdown().get();
  }

  @Test
  public void test_remote_greeting_request_completes_before_timeout() throws Exception {
    Duration duration = Duration.ofSeconds(1);

    // Create microservices instance.
    Microservices gateway = Microservices.builder()
        .port(port.incrementAndGet())
        .build();

    Microservices node2 = Microservices.builder()
        .seeds(gateway.cluster().address())
        .services(new GreetingServiceImpl())
        .build();

    GreetingService service = gateway.call()
        .timeout(Duration.ofSeconds(3))
        .api(GreetingService.class);

    // call the service.
    CompletableFuture<GreetingResponse> result = service.greetingRequestTimeout(new GreetingRequest("joe", duration));

    CountDownLatch timeLatch = new CountDownLatch(1);
    result.whenComplete((success, error) -> {
      if (error == null) {
        // print the greeting.
        System.out.println("1. greeting_request_completes_before_timeout : " + success.getResult());
        assertTrue(success.getResult().equals(" hello to: joe"));
        timeLatch.countDown();
      } else {
        System.out.println("1. FAILED! - greeting_request_completes_before_timeout reached timeout: " + error);
        assertTrue(error.toString(), false);
        timeLatch.countDown();
      }
    });

    assertTrue(await(timeLatch, 10, TimeUnit.SECONDS));
    node2.shutdown().get();
    gateway.shutdown().get();

  }

  @Test
  public void test_remote_void_greeting() throws InterruptedException, ExecutionException {
    // Create microservices instance.
    Microservices gateway = Microservices.builder()
        .port(port.incrementAndGet())
        .build();

    Microservices node1 = Microservices.builder()
        .seeds(gateway.cluster().address())
        .services(new GreetingServiceImpl())
        .build();

    GreetingService service = gateway.call()
        .timeout(Duration.ofSeconds(3))
        .api(GreetingService.class);

    // call the service.
    service.greetingVoid(new GreetingRequest("joe"));

    // send and forget so we have no way to know what happen
    // but at least we didn't get exception :)
    assertTrue(true);
    System.out.println("test_remote_void_greeting done.");

    Thread.sleep(1000);

    gateway.shutdown().get();
    node1.shutdown().get();
  }

  @Test
  public void test_remote_async_greeting_return_string() throws Exception {
    // Create microservices cluster.
    Microservices provider = Microservices.builder()
        .port(port.incrementAndGet())
        .services(new GreetingServiceImpl())
        .build();

    // Create microservices cluster.
    Microservices consumer = Microservices.builder()
        .port(port.incrementAndGet())
        .seeds(provider.cluster().address())
        .build();

    // get a proxy to the service api.
    GreetingService service = createProxy(consumer);

    // call the service.
    CompletableFuture<String> future = service.greeting("joe");
    AtomicReference<String> string = new AtomicReference<String>("");
    CountDownLatch timeLatch = new CountDownLatch(1);
    future.whenComplete((result, ex) -> {
      if (ex == null) {
        // print the greeting.
        string.set(result);
        System.out.println("4. remote_async_greeting_return_string :" + result);
      } else {
        // print the greeting.
        System.out.println(ex);
      }
      timeLatch.countDown();
    });
    await(timeLatch, 100, TimeUnit.MILLISECONDS);
    assertTrue(string.get().equals(" hello to: joe"));
    assertTrue(timeLatch.getCount() == 0);
    provider.shutdown().get();
    consumer.shutdown().get();
  }

  @Test
  public void test_remote_async_greeting_no_params() throws Exception {
    // Create microservices cluster.
    Microservices provider = Microservices.builder()
        .port(port.incrementAndGet())
        .services(new GreetingServiceImpl())
        .build();

    // Create microservices cluster.
    Microservices consumer = Microservices.builder()
        .port(port.incrementAndGet())
        .seeds(provider.cluster().address())
        .build();

    // get a proxy to the service api.
    GreetingService service = createProxy(consumer);

    // call the service.
    CompletableFuture<String> future = service.greetingNoParams();

    CountDownLatch timeLatch = new CountDownLatch(1);
    future.whenComplete((result, ex) -> {
      if (ex == null) {
        // print the greeting.
        System.out.println("test_remote_async_greeting_no_params :" + result);
        assertTrue(result.equals("hello unknown"));
      } else {
        // print the greeting.
        System.out.println(ex);
      }
      timeLatch.countDown();
    });

    assertTrue(await(timeLatch, 1, TimeUnit.SECONDS));
    provider.shutdown().get();
    consumer.shutdown().get();
  }

  @Test
  public void test_remote_async_greeting_return_GreetingResponse() throws Exception {
    // Create microservices cluster.
    Microservices provider = Microservices.builder()
        .port(port.incrementAndGet())
        .services(new GreetingServiceImpl())
        .build();

    // Create microservices cluster.
    Microservices consumer = Microservices.builder()
        .port(port.incrementAndGet())
        .seeds(provider.cluster().address())
        .build();

    // get a proxy to the service api.
    GreetingService service = createProxy(consumer);

    // call the service.
    CompletableFuture<GreetingResponse> future = service.greetingRequest(new GreetingRequest("joe"));

    CountDownLatch timeLatch = new CountDownLatch(1);
    future.whenComplete((result, ex) -> {
      if (ex == null) {
        // print the greeting.
        System.out.println("6. remote_async_greeting_return_GreetingResponse :" + result.getResult());
        // print the greeting.
        assertTrue(result.getResult().equals(" hello to: joe"));
      } else {
        // print the greeting.
        System.out.println(ex);
      }
      timeLatch.countDown();
    });

    assertTrue(await(timeLatch, 1, TimeUnit.SECONDS));
    provider.shutdown().get();
    consumer.shutdown().get();
  }


  @Test
  public void test_remote_greeting_request_timeout_expires() throws InterruptedException, ExecutionException {
    // Create microservices cluster.
    Microservices provider = Microservices.builder()
        .port(port.incrementAndGet())
        .services(new GreetingServiceImpl())
        .build();

    // Create microservices cluster.
    Microservices consumer = Microservices.builder()
        .port(port.incrementAndGet())
        .seeds(provider.cluster().address())
        .build();

    // get a proxy to the service api.
    GreetingService service = createProxy(consumer, Duration.ofSeconds(1));

    // call the service.
    CompletableFuture<GreetingResponse> result =
        service.greetingRequestTimeout(new GreetingRequest("joe", Duration.ofSeconds(4)));

    CountDownLatch timeLatch = new CountDownLatch(1);

    result.whenComplete((success, error) -> {
      if (error != null) {
        // print the greeting.
        System.out.println("8. remote_greeting_request_timeout_expires : " + error);
        assertTrue(error instanceof TimeoutException);
        timeLatch.countDown();
      }
    });

    try {
      await(timeLatch, 10, TimeUnit.SECONDS);
    } catch (Exception ex) {
      fail();
    }
    provider.shutdown().get();
    consumer.shutdown().get();
  }


  @Test
  public void test_remote_async_greeting_return_Message() throws Exception {
    // Create microservices cluster.
    Microservices provider = Microservices.builder()
        .port(port.incrementAndGet())
        .services(new GreetingServiceImpl())
        .build();

    // Create microservices cluster.
    Microservices consumer = Microservices.builder()
        .port(port.incrementAndGet())
        .seeds(provider.cluster().address())
        .build();

    // get a proxy to the service api.
    GreetingService service = createProxy(consumer);

    // call the service.
    CompletableFuture<GreetingResponse> future = service.greetingRequest(new GreetingRequest("joe"));

    CountDownLatch timeLatch = new CountDownLatch(1);
    future.whenComplete((result, ex) -> {
      if (ex == null) {
        // print the greeting.
        System.out.println("10. remote_async_greeting_return_Message :" + result);
        // print the greeting.
        assertTrue(result.getResult().equals(" hello to: joe"));
      } else {
        // print the greeting.
        System.out.println("10 failed: " + ex);
        assertTrue(result.getResult().equals(" hello to: joe"));
      }
      timeLatch.countDown();
    });

    assertTrue(await(timeLatch, 20, TimeUnit.SECONDS));
    assertTrue(timeLatch.getCount() == 0);
    consumer.shutdown().get();
    provider.shutdown().get();
  }

  @Test
  public void test_remote_round_robin_selection_logic() throws Exception {
    Microservices gateway = createSeed();

    // Create microservices instance cluster.
    Microservices provider1 = Microservices.builder()
        .seeds(gateway.cluster().address())
        .port(port.incrementAndGet())
        .services(new GreetingServiceImpl())
        .build();

    // Create microservices instance cluster.
    Microservices provider2 = Microservices.builder()
        .seeds(gateway.cluster().address())
        .port(port.incrementAndGet())
        .services(new GreetingServiceImpl())
        .build();

    GreetingService service = createProxy(gateway);

    CompletableFuture<GreetingResponse> result1 = service.greetingRequest(new GreetingRequest("joe"));
    CompletableFuture<GreetingResponse> result2 = service.greetingRequest(new GreetingRequest("joe"));

    CompletableFuture<Void> combined = CompletableFuture.allOf(result1, result2);
    CountDownLatch timeLatch = new CountDownLatch(1);
    combined.whenComplete((v, x) -> {
      try {
        // print the greeting.
        System.out.println("11. round_robin_selection_logic :" + result1.get());
        System.out.println("11. round_robin_selection_logic :" + result2.get());
        GreetingResponse response1 = result1.get();
        GreetingResponse response2 = result2.get();
        boolean success = !response1.sender().equals(response2.sender());

        assertTrue(success);
      } catch (Throwable e) {
        assertTrue(false);
      }
      timeLatch.countDown();
    });

    assertTrue(await(timeLatch, 2, TimeUnit.SECONDS));
    assertTrue(timeLatch.getCount() == 0);
    provider2.shutdown().get();
    provider1.shutdown().get();
    gateway.shutdown().get();
  }

  @Test
  public void test_remote_async_greeting_return_string_service_not_found_error_case() throws Exception {
    Microservices gateway = createSeed();

    // Create microservices instance cluster.
    Microservices provider1 = createProvider(gateway);

    GreetingService service = createProxy(gateway);
    CountDownLatch timeLatch = new CountDownLatch(1);
    try {
      service.greeting("hello");
    } catch (Exception ex) {
      assertTrue(ex.getMessage().equals("No reachable member with such service: greeting"));
      timeLatch.countDown();
    }

    assertTrue(await(timeLatch, 1, TimeUnit.SECONDS));
    gateway.shutdown();
    provider1.shutdown();
  }



  @Test
  public void test_remote_serviceA_calls_serviceB_using_setter() throws InterruptedException, ExecutionException {

    Microservices gateway = createSeed();

    CoarseGrainedServiceImpl coarseGrained = new CoarseGrainedServiceImpl();

    GreetingServiceImpl greeting = new GreetingServiceImpl();

    // Create microservices instance cluster.
    Microservices provider = Microservices.builder()
        .seeds(gateway.cluster().address())
        .port(port.incrementAndGet())
        .services(greeting, coarseGrained) // add service a and b
        .build();

    // Get a proxy to the service api.
    CoarseGrainedService service = gateway.call().api(CoarseGrainedService.class);
    CountDownLatch countLatch = new CountDownLatch(1);
    CompletableFuture<String> future = service.callGreeting("joe");
    future.whenComplete((success, error) -> {
      if (error == null) {
        assertTrue(success.equals(" hello to: joe"));
        countLatch.countDown();
      }
    });

    countLatch.await(5, TimeUnit.SECONDS);
    assertTrue(countLatch.getCount() == 0);
    gateway.shutdown().get();
    provider.shutdown().get();

  }

  @Test
  public void test_remote_serviceA_calls_serviceB() throws InterruptedException, ExecutionException {

    Microservices gateway = createSeed();

    // getting proxy from any node at any given time.
    CoarseGrainedServiceImpl another = new CoarseGrainedServiceImpl();

    GreetingServiceImpl greeting = new GreetingServiceImpl();

    // Create microservices instance cluster.
    Microservices provider = Microservices.builder()
        .seeds(gateway.cluster().address())
        .port(port.incrementAndGet())
        .services(greeting, another) // add service a and b
        .build();

    // Get a proxy to the service api.
    CoarseGrainedService service = gateway.call().api(CoarseGrainedService.class);
    CountDownLatch countLatch = new CountDownLatch(1);
    CompletableFuture<String> future = service.callGreeting("joe");
    future.whenComplete((success, error) -> {
      if (error == null) {
        assertTrue(success.equals(" hello to: joe"));
        countLatch.countDown();
      }
    });

    countLatch.await(5, TimeUnit.SECONDS);
    assertTrue(countLatch.getCount() == 0);
    gateway.shutdown().get();
    provider.shutdown().get();

  }

  @Test
  public void test_remote_serviceA_calls_serviceB_with_timeout() throws InterruptedException, ExecutionException {
    CountDownLatch countLatch = new CountDownLatch(1);
    Microservices gateway = createSeed();

    // getting proxy from any node at any given time.
    CoarseGrainedServiceImpl another = new CoarseGrainedServiceImpl();

    GreetingServiceImpl greeting = new GreetingServiceImpl();

    // Create microservices instance cluster.
    Microservices provider = Microservices.builder()
        .seeds(gateway.cluster().address())
        .port(port.incrementAndGet())
        .services(greeting, another) // add service a and b
        .build();

    // Get a proxy to the service api.
    CoarseGrainedService service =
        gateway.call().timeout(Duration.ofSeconds(1)).api(CoarseGrainedService.class);
    service.callGreetingTimeout("joe")
        .whenComplete((success, error) -> {
          if (error != null) {
            assertTrue(error instanceof TimeoutException);
            System.out.println("CoarseGrainedService.callGreetingTimeout: " + (error instanceof TimeoutException));
            countLatch.countDown();
          }
        });

    countLatch.await(5, TimeUnit.SECONDS);
    assertTrue(countLatch.getCount() == 0);
    gateway.shutdown().get();
    provider.shutdown().get();

  }

  @Test
  public void test_remote_serviceA_calls_serviceB_with_dispatcher() throws InterruptedException, ExecutionException {
    CountDownLatch countLatch = new CountDownLatch(1);
    Microservices gateway = createSeed();

    // getting proxy from any node at any given time.
    CoarseGrainedServiceImpl another = new CoarseGrainedServiceImpl();

    GreetingServiceImpl greeting = new GreetingServiceImpl();

    // Create microservices instance cluster.
    Microservices provider = Microservices.builder()
        .seeds(gateway.cluster().address())
        .port(port.incrementAndGet())
        .services(greeting, another) // add service a and b
        .build();

    // Get a proxy to the service api.
    CoarseGrainedService service =
        gateway.call().timeout(Duration.ofSeconds(30)).api(CoarseGrainedService.class);
    service.callGreetingWithDispatcher("joe")
        .whenComplete((success, error) -> {
          if (error == null) {
            assertEquals(success, " hello to: joe");
            countLatch.countDown();
          } else {
            System.out.println("failed with error: " + error);
          }
        });

    countLatch.await(5, TimeUnit.SECONDS);
    assertTrue(countLatch.getCount() == 0);
    gateway.shutdown().get();
    provider.shutdown().get();

  }

  @Test
  public void test_services_contribute_to_cluster_metadata() {
    Map<String, String> metadata = new HashMap<>();
    metadata.put("HOSTNAME", "host1");
    Builder clusterConfig = ClusterConfig.builder().metadata(metadata);
    Microservices ms = Microservices.builder()
        .clusterConfig(clusterConfig)
        .services(new GreetingServiceImpl()).build();

    assertTrue(ms.cluster().member().metadata().containsKey("HOSTNAME"));
    assertTrue(ServiceInfo.from(ms.cluster().member().metadata()
        .entrySet().stream()
        .filter(item -> item.getValue().equals("service"))
        .findFirst().get().getKey())
        .getServiceName().equals("io.scalecube.services.GreetingService"));
  }

  private GreetingService createProxy(Microservices gateway) {
    return gateway.call()
        .timeout(Duration.ofSeconds(30))
        .api(GreetingService.class); // create proxy for GreetingService API

  }

  private GreetingService createProxy(Microservices micro, Duration duration) {
    return micro.call()
        .timeout(duration)
        .api(GreetingService.class); // create proxy for GreetingService API

  }

  private Microservices createProvider(Microservices gateway) {
    return Microservices.builder()
        .seeds(gateway.cluster().address())
        .port(port.incrementAndGet())
        .build();
  }

  private Microservices createSeed() {
    return Microservices.builder()
        .port(port.incrementAndGet())
        .build();
  }

  private boolean await(CountDownLatch timeLatch, long timeout, TimeUnit timeUnit) throws Exception {
    return timeLatch.await(timeout, timeUnit);
  }
}
