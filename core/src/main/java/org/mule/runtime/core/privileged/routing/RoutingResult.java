/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.routing;


import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;

import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.exception.EventProcessingException;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * The result of routing an {@link CoreEvent} to {@code n} {@link MessageProcessorChain} routes, or {@code n} {@link CoreEvent}'s
 * to the same {@link MessageProcessorChain} route typically by using ForkJoinStrategy.
 * <p>
 * Results are indexed using the order of RoutingPair as defined by the router. With ScatterGatherRouter this is the order of
 * routes as defined in configuration.
 *
 * @since 4.0
 */
public final class RoutingResult {

  private final Map<String, Message> successfulRoutesResultMap;
  private final Map<String, Error> failedRoutesErrorMap;
  private Map<String, Pair<Error, EventProcessingException>> failedRoutesErrorExceptionMap;

  public RoutingResult(Map<String, Message> successfulRoutesResultMap, Map<String, Error> failedRoutesErrorMap) {
    this.successfulRoutesResultMap = unmodifiableMap(successfulRoutesResultMap);
    this.failedRoutesErrorMap = unmodifiableMap(failedRoutesErrorMap);
    this.failedRoutesErrorExceptionMap = emptyMap();
  }

  public static RoutingResult routingResultWithException(Map<String, Message> successfulRoutesResultMap,
                                                         Map<String, Pair<Error, EventProcessingException>> failedRoutesErrorExceptionMap) {
    RoutingResult routingResult = new RoutingResult(successfulRoutesResultMap, emptyMap());
    routingResult.setFailedRoutesErrorExceptionMap(failedRoutesErrorExceptionMap);
    return routingResult;
  }

  private void setFailedRoutesErrorExceptionMap(Map<String, Pair<Error, EventProcessingException>> failedRoutesErrorExceptionMap) {
    this.failedRoutesErrorExceptionMap = failedRoutesErrorExceptionMap;
  }

  public Map<String, Message> getResults() {
    return successfulRoutesResultMap;
  }

  public Map<String, Error> getFailures() {
    // return a map that is not empty using the return type Map<String, Error>
    if (!failedRoutesErrorMap.isEmpty()) {
      return failedRoutesErrorMap;
    }
    if (!failedRoutesErrorExceptionMap.isEmpty()) {
      return failedRoutesErrorExceptionMap.entrySet().stream()
          .collect(Collectors.toMap(Map.Entry::getKey, pair -> pair.getValue().getFirst()));
    }
    return failedRoutesErrorMap;
  }

  // todo: pair -> either
  public Map<String, Pair<Error, EventProcessingException>> getFailuresWithException() {
    return failedRoutesErrorExceptionMap;
  }

}
