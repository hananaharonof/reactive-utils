package org.hananaharonof.reactiveutils.ratelimiter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author haharonof (on 23/11/2018).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DynamicRateLimiterConfiguration {

  private int initialMaxInFlight;
  private int upperMaxInFlight;
  private int lowerMaxInFlight;
  private int inFlightIncrement;
  private int inFlightDecrementFactor;
}

