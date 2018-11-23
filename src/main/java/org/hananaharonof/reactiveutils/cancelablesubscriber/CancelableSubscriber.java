package org.hananaharonof.reactiveutils.cancelablesubscriber;

import java.util.concurrent.atomic.AtomicBoolean;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * A reactive subscriber with cancellation API
 *
 * @author haharonof (on 23/11/2018).
 */
public class CancelableSubscriber<T> implements Subscriber<T> {

  private Subscription subscription;
  private AtomicBoolean completed;

  public CancelableSubscriber() {
    this.completed = new AtomicBoolean(false);
  }

  public void cancel() {
    this.subscription.cancel();
    this.completed.set(true);
  }

  @Override
  public void onSubscribe(Subscription subscription) {
    this.subscription = subscription;
    subscription.request(1);
  }

  @Override
  public void onNext(T t) {
    this.subscription.request(1);
  }

  @Override
  public void onError(Throwable throwable) {

  }

  @Override
  public void onComplete() {
    this.completed.set(true);
  }
}

