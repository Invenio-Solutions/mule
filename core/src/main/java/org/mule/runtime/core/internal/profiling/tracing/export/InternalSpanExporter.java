/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.export;

import org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpan;

/**
 * An exporter for {@link InternalSpanExporter}
 *
 * @since 4.5.0
 */
public interface InternalSpanExporter {

  /**
   * Exports {@link InternalSpan}'s.
   *
   * @param internalSpan the {@link InternalSpan} to export
   */
  void export(InternalSpan internalSpan);

  /**
   * @param internalSpanExporterVisitor the visitor
   * @param <T>                         the result type
   * @return the result of visit
   */
  <T> T visit(InternalSpanExporterVisitor<T> internalSpanExporterVisitor);
}
