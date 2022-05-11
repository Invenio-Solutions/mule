/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.consumer.tracing.operations;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.profiling.tracing.Span;
import org.mule.runtime.api.profiling.type.context.ComponentProcessingStrategyProfilingEventContext;
import org.mule.runtime.api.profiling.type.context.SpanProfilingEventContext;
import org.mule.runtime.core.internal.profiling.consumer.tracing.span.SpanManager;

import static java.lang.System.currentTimeMillis;
import static org.mule.runtime.core.internal.profiling.consumer.tracing.operations.SpanUtils.getBuilder;

/**
 * A {@link SpanProfilingEventContext} that corresponds to a span profiling event.
 *
 * @since 4.5.0
 */
public class DefaultSpanProfilingEventContext implements SpanProfilingEventContext {

  private final ComponentProcessingStrategyProfilingEventContext eventContext;
  private final long triggerTimeStamp;
  private final SpanManager spanManager;

  public DefaultSpanProfilingEventContext(ComponentProcessingStrategyProfilingEventContext eventContext,
                                          SpanManager spanManager) {
    this.eventContext = eventContext;
    triggerTimeStamp = currentTimeMillis();
    this.spanManager = spanManager;
  }

  @Override
  public long getTriggerTimestamp() {
    return triggerTimeStamp;
  }

  @Override
  public Span getSpan() {
    ComponentLocation location = eventContext.getLocation().get();
    return getBuilder(location)
        .withSpanManager(spanManager)
        .withArtifactId(eventContext.getArtifactId())
        .withLocation(location)
        .withCorrelationId(eventContext.getCorrelationId())
        .build();
  }
}
