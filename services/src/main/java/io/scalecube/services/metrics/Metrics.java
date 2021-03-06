package io.scalecube.services.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import java.lang.reflect.Method;

public class Metrics {

  private final MetricRegistry registry;

  public Meter getMeter(final String component, final String methodName, final String eventType) {
    return registry.meter(MetricRegistry.name(component, methodName, eventType));
  }

  public <T> Meter getMeter(
      final Class<T> component, final String methodName, final String eventType) {
    return getMeter(component.getName(), methodName, eventType);
  }

  public Timer getTimer(String component, String methodName) {
    return registry.timer(MetricRegistry.name(component, methodName));
  }

  public <T> Timer getTimer(Class<T> component, String methodName) {
    return getTimer(component.getName(), methodName);
  }

  public Counter getCounter(final String component, final String methodName) {
    return registry.counter(MetricRegistry.name(component, methodName));
  }

  public <T> Counter getCounter(final Class<T> component, final String methodName) {
    return getCounter(component.getName(), methodName);
  }

  /**
   * Register a Gauge and service registry.
   *
   * @param component name for the requested timer.
   * @param methodName for the requested timer.
   * @param gauge instance.
   * @return registered gauge in the service registry.
   */
  public <T> Gauge<T> register(
      final String component, final String methodName, final Gauge<T> gauge) {
    registry.register(
        MetricRegistry.name(component, methodName),
        new Gauge<T>() {
          @Override
          public T getValue() {
            return gauge.getValue();
          }
        });

    return gauge;
  }

  public <T, G> Gauge<G> register(Class<T> component, String methodName, Gauge<G> gauge) {
    return register(component.getName(), methodName, gauge);
  }

  public Histogram getHistogram(
      final String component, final String methodName, final boolean biased) {
    return registry.histogram(MetricRegistry.name(component, methodName));
  }

  public <T> Histogram getHistogram(
      final Class<T> component, final String methodName, final boolean biased) {
    return getHistogram(component.getName(), methodName, biased);
  }

  public Metrics(final MetricRegistry registry) {
    this.registry = registry;
  }

  /**
   * if metrics is not null returns a Timer instance for a given component and method name.
   *
   * @param metrics factory instance to get timer.
   * @param component name for the requested timer.
   * @param methodName for the requested timer.
   * @return timer instance.
   */
  public static Timer timer(Metrics metrics, String component, String methodName) {
    if (metrics != null) {
      return metrics.getTimer(component, methodName);
    } else {
      return null;
    }
  }

  /**
   * if metrics is not null returns a Counter instance for a given component and method name.
   *
   * @param metrics factory instance to get timer.
   * @param component name for the requested timer.
   * @param methodName for the requested timer.
   * @return counter instance.
   */
  public static Counter counter(Metrics metrics, String component, String methodName) {
    if (metrics != null) {
      return metrics.getCounter(component, methodName);
    } else {
      return null;
    }
  }

  /**
   * Reports an event to given <code>meter</code>.
   *
   * @param meter - meter that consumes na event.
   */
  public static void mark(Meter meter) {
    if (meter != null) {
      meter.mark();
    }
  }

  /**
   * Reports an event to <code>meter</code> described by given parameters.
   *
   * @param metrics - {@link Metrics} instance with {@link MetricRegistry} initialized.
   * @param component - part of metric description (calss name is used).
   * @param methodName - part of metric description.
   * @param eventType - part of metric description.
   */
  public static void mark(Metrics metrics, Class component, String methodName, String eventType) {
    mark(metrics, component.getName(), methodName, eventType);
  }

  /**
   * Reports an event to <code>meter</code> described by given parameters.
   *
   * @param metrics - {@link Metrics} instance with {@link MetricRegistry} initialized.
   * @param component - part of metric description.
   * @param methodName - part of metric description.
   * @param eventType - part of metric description.
   */
  public static void mark(Metrics metrics, String component, String methodName, String eventType) {
    if (metrics != null) {
      mark(metrics.getMeter(component, methodName, eventType));
    }
  }

  /**
   * Reports an event to <code>meter</code> described by given parameters.
   *
   * @param serviceInterface - part of metric description (class name is used).
   * @param metrics - {@link Metrics} instance with {@link MetricRegistry} initialized.
   * @param method - part of metric description (method name is used).
   * @param eventType - part of metric description.
   */
  public static void mark(
      Class<?> serviceInterface, Metrics metrics, Method method, String eventType) {
    if (metrics != null) {
      Meter meter = metrics.getMeter(serviceInterface, method.getName(), eventType);
      Metrics.mark(meter);
    }
  }

  /**
   * Start measuring time to be later reported to metrics.
   *
   * @param timer - instance of timer
   * @return timer context that might be stopped and therefore reported to metrics.
   */
  public static Context time(Timer timer) {
    if (timer != null) {
      return timer.time();
    }
    return null;
  }

  /**
   * Stop time measurement and report the interval to metrics.
   *
   * @param ctx - timer context to be stopped.
   */
  public static void stop(Context ctx) {
    if (ctx != null) {
      ctx.stop();
    }
  }

  /**
   * Increase counter.
   *
   * @param counter - counter to increase.
   */
  public static void inc(Counter counter) {
    if (counter != null) {
      counter.inc();
    }
  }

  /**
   * Decrease counter.
   *
   * @param counter - counter to decrease.
   */
  public static void dec(Counter counter) {
    if (counter != null) {
      counter.dec();
    }
  }
}
