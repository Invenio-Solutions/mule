/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.hackday;

import static org.mule.runtime.api.metadata.DataType.fromType;
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_JSON;

import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.ExpressionLanguage;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.DataTypeParamsBuilder;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;
import org.mule.runtime.http.api.domain.request.HttpRequestContext;
import org.mule.runtime.http.api.server.RequestHandler;
import org.mule.runtime.http.api.server.async.HttpResponseReadyCallback;
import org.mule.runtime.http.api.server.async.ResponseStatusCallback;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

abstract class AbstractExtensionRequestHandler implements RequestHandler {

  protected static final ResponseStatusCallback NULL_STATUS_CALLBACK = new ResponseStatusCallback() {
    @Override
    public void responseSendFailure(Throwable throwable) {

    }

    @Override
    public void responseSendSuccessfully() {

    }
  };

  protected static final DataType JSON_STREAM_TYPE = DataType.builder()
      .type(InputStream.class)
      .mediaType(APPLICATION_JSON)
      .build();


  protected ExpressionLanguage expressionLanguage;

  protected AbstractExtensionRequestHandler(ExpressionLanguage expressionLanguage) {
    this.expressionLanguage = expressionLanguage;
  }

  @Override
  public void handleRequest(HttpRequestContext requestContext, HttpResponseReadyCallback responseCallback) {
    try {
      doHandle(requestContext, responseCallback);
    } catch (Exception e) {
      sendExceptionResponse(responseCallback, e);
    }
  }

  protected abstract void doHandle(HttpRequestContext ctx, HttpResponseReadyCallback responseCallback) throws Exception;

  protected ResponseStatusCallback streamingCallback(TypedValue typedValue) {
    Object value = typedValue.getValue();
    Runnable task;
    if (value instanceof CursorProvider) {
      task = () -> {
        ((CursorProvider<?>) value).close();
        ((CursorProvider<?>) value).releaseResources();
      };
    } else if (value instanceof Closeable) {
      task = () -> IOUtils.closeQuietly((Closeable) value);
    } else {
      task = () -> {
      };
    }

    return new ResponseStatusCallback() {

      @Override
      public void responseSendFailure(Throwable throwable) {
        task.run();
      }

      @Override
      public void responseSendSuccessfully() {
        task.run();
      }
    };
  }

  protected <T> T parseRequest(HttpRequest request, Class<T> type) {
    try (InputStream entity = request.getEntity().getContent()) {
      BindingContext bindingContext = BindingContext.builder()
          .addBinding("payload", new TypedValue(entity, JSON_STREAM_TYPE))
          .build();

      return (T) expressionLanguage.evaluate("#[payload]", fromType(type), bindingContext).getValue();
    } catch (IOException e) {
      throw new MuleRuntimeException(e);
    }
  }

  private void sendExceptionResponse(HttpResponseReadyCallback responseCallback, Exception e) {
    responseCallback.responseReady(HttpResponse.builder()
        .statusCode(500)
        .reasonPhrase(e.getMessage())
        .build(), NULL_STATUS_CALLBACK);
  }

  protected DataType toDataType(Object value, Optional<MediaType> mediaType) {
    DataTypeParamsBuilder builder = DataType.builder()
        .type(value.getClass());

    mediaType.ifPresent(builder::mediaType);

    return builder.build();
  }

}
