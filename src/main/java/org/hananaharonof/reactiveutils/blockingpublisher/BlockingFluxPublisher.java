package org.hananaharonof.reactiveutils.blockingpublisher;

import java.util.concurrent.Semaphore;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

/**
 * A utility class that creates a Flux which blocks (no busy wait) items consumption according to
 * downstream consumers demand.
 *
 * @author haharonof (on 23/11/2018).
 */
public class BlockingFluxPublisher<T> {

  private Flux<T> flux;
  private FluxSink<T> sink;
  private Semaphore semaphore = new Semaphore(1);

  public BlockingFluxPublisher(){
    this.flux = Flux.create(sink->{
      this.sink = sink;
      sink.onRequest(l->{
        semaphore.release((int)l);
      });
    });
  }

  /**
   * Publish next item of this Flux if downstream consumers requests,
   * o.w. blocks (no-busy) until a demand request arrives.
   *
   * @param nextItem - item to publish on this Flux
   */
  public void next(T nextItem){
    semaphore.acquireUninterruptibly();
    sink.next(nextItem);
  }

  /**
   * Returns this Flux.
   *
   * @return this Flux.
   */
  public Flux<T> getFlux() {
    return flux;
  }

}