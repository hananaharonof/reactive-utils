package org.hananaharonof.ratelimiter;

import java.util.concurrent.atomic.AtomicInteger;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;

/**
 * A utility class that acts as a rate limiter for a Flux stream. The limiter ensures a
 * dynamic limit on the number of in-flight items for a given Flux. The limit is determined
 * by configuration and the increase/decrease API. The user of this class is responsible to
 * call endScope() for every item produced on the inbound Flux upon its processing completions
 *
 * @author haharonof (on 23/05/2018).
 */

public class DynamicRateLimiter<T> {

  private DynamicRateLimiterConfiguration configuration;
  private AtomicInteger currentMaxInFlight;
  private AtomicInteger inFlight;
  private Subscription subscription;
  private Flux<T> outbound;

  public DynamicRateLimiter(
      Flux<T> inbound,
      DynamicRateLimiterConfiguration dynamicRateLimiterConfiguration) {

    this.configuration = dynamicRateLimiterConfiguration;
    this.currentMaxInFlight = new AtomicInteger(
        dynamicRateLimiterConfiguration.getInitialMaxInFlight());
    this.inFlight = new AtomicInteger(0);
    this.outbound = bind(inbound);
  }

  /**
   * Outbound of the limiter. Items will be published on this Flux according to the in-flight limit.
   *
   * @return Outbound flux for this limiter.
   */
  public Flux<T> outbound() {
    return outbound;
  }

  /**
   * Signals the limiter that an item produced on this limiter outbound has completed its
   * processing and a new item can be published.
   */
  public synchronized void endScope() {
    int inFlight = this.inFlight.get();
    if (inFlight > 0) {
      inFlight = this.inFlight.decrementAndGet();
    }

    int requestSize = currentMaxInFlight.get() - inFlight;

    if (requestSize > 0) {
      subscription.request(requestSize);
      this.inFlight.addAndGet(requestSize);
    }
  }

  /**
   * Increase this limiter bound in a fixed size set in configuration.
   *
   * @return true if increase occurred, false if reached max bound set by configuration.
   */
  public synchronized boolean incrementLimit() {
    int maxInFlight = currentMaxInFlight.get();
    int increment = Math.min(configuration.getUpperMaxInFlight(),
        maxInFlight + configuration.getInFlightIncrement());

    if (increment != maxInFlight) {
      currentMaxInFlight.set(increment);
      return true;
    }

    return false;
  }

  /**
   * Decrement this limiter bound by a fixed factor size set in configuration.
   *
   * @return true if decrement occurred, false if reached lower bound set by configuration.
   */
  public synchronized boolean decrementLimit() {
    int maxInFlight = currentMaxInFlight.get();
    int decrement = Math.max(configuration.getLowerMaxInFlight(),
        maxInFlight / configuration.getInFlightDecrementFactor());

    if (decrement != maxInFlight) {
      currentMaxInFlight.set(decrement);
      return true;
    }

    return false;
  }

  private Flux<T> bind(Flux<T> inbound) {
    return Flux.create(sink -> {
      inbound.subscribe(new Subscriber<T>() {
        @Override
        public void onSubscribe(Subscription subscription) {
          DynamicRateLimiter.this.subscription = subscription;
          int maxInFlight = currentMaxInFlight.get();
          subscription.request(maxInFlight);
          inFlight.addAndGet(maxInFlight);
        }

        @Override
        public void onNext(T nextItem) {
          sink.next(nextItem);
        }

        @Override
        public void onError(Throwable throwable) {
          sink.error(throwable);
        }

        @Override
        public void onComplete() {
          sink.complete();
        }
      });
    });
  }


  public synchronized int getLimit() {
    return currentMaxInFlight.get();
  }

  public synchronized int getActive() {
    return inFlight.get();
  }
}
