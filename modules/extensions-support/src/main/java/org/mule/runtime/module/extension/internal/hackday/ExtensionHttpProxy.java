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
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.DataTypeParamsBuilder;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.extension.api.client.DefaultOperationParameters;
import org.mule.runtime.extension.api.client.DefaultOperationParametersBuilder;
import org.mule.runtime.extension.api.client.ExtensionsClient;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.runtime.http.api.domain.entity.HttpEntity;
import org.mule.runtime.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;
import org.mule.runtime.http.api.server.HttpServer;
import org.mule.runtime.http.api.server.HttpServerConfiguration;
import org.mule.runtime.http.api.server.RequestHandlerManager;
import org.mule.runtime.http.api.server.async.HttpResponseReadyCallback;
import org.mule.runtime.http.api.server.async.ResponseStatusCallback;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

public class ExtensionHttpProxy implements Startable, Stoppable {

  private static final DataType JSON_STREAM_TYPE = DataType.builder()
      .type(InputStream.class)
      .mediaType(APPLICATION_JSON)
      .build();

  private static final ResponseStatusCallback NULL_STATUS_CALLBACK = new ResponseStatusCallback() {
    @Override
    public void responseSendFailure(Throwable throwable) {

    }

    @Override
    public void responseSendSuccessfully() {

    }
  };

  @Inject
  private HttpService httpService;

  @Inject
  private ExtensionsClient extensionsClient;

  @Inject
  private ExpressionLanguage expressionLanguage;

  private HttpServer httpServer;
  private RequestHandlerManager requestHandlerManager;

  @Override
  public void start() throws MuleException {
    httpServer = httpService.getServerFactory().create(new HttpServerConfiguration.Builder()
        .setName("extensionProxy")
        .setHost("0.0.0.0")
        .setPort(8080)
        .build());

    try {
      httpServer.start();
    } catch (IOException e) {
      throw new DefaultMuleException(e.getMessage(), e);
    }

    requestHandlerManager = httpServer.addRequestHandler("/extension/*", (request, callback) -> handleRequest(request.getRequest(), callback));
  }

  private void handleRequest(HttpRequest httpRequest, HttpResponseReadyCallback responseCallback) {
    try {
      ExtensionExecutionRequest extensionRequest = parseRequest(httpRequest);
      DefaultOperationParametersBuilder builder = DefaultOperationParameters.builder()
          .configName(extensionRequest.getConfigRef());

      extensionRequest.getParameters().forEach(builder::addParameter);

      Result result = extensionsClient.execute(getExtensionName(httpRequest),
          extensionRequest.getOperation(),
          builder.build());

      TypedValue response = serializeResult(result);

      responseCallback.responseReady(HttpResponse.builder()
          .statusCode(200)
          .entity(toHttpEntity(response.getValue()))
          .build(), streamingCallback(response));
    } catch (Exception e) {
      responseCallback.responseReady(HttpResponse.builder()
          .statusCode(500)
          .reasonPhrase(e.getMessage())
          .build(), NULL_STATUS_CALLBACK);
    }
  }

  private ResponseStatusCallback streamingCallback(TypedValue typedValue) {
    Object value = typedValue.getValue();
    Runnable task;
    if (value instanceof CursorProvider) {
      task = () -> {
        ((CursorProvider<?>) value).close();
        ((CursorProvider<?>) value).releaseResources();
      };
    }  else if (value instanceof Closeable) {
      task = () -> IOUtils.closeQuietly((Closeable) value);
    } else {
      task = () -> {};
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

  private HttpEntity toHttpEntity(Object value) {
    if (value instanceof CursorStreamProvider) {
      return new InputStreamHttpEntity(((CursorStreamProvider) value).openCursor());
    } else if (value instanceof InputStream) {
      return new InputStreamHttpEntity((InputStream) value);
    } else if (value instanceof String) {
      return new ByteArrayHttpEntity(((String) value).getBytes());
    } else {
      throw new IllegalArgumentException("unsupported value type " + value.getClass());
    }
  }

  private TypedValue<?> serializeResult(Result result) {
    BindingContext bindingContext = BindingContext.builder()
        .addBinding("payload", new TypedValue(result.getOutput(), toDataType(result.getOutput(), result.getMediaType())))
        .addBinding("attributes", new TypedValue(result.getAttributes(), toDataType(result.getOutput(), result.getMediaType())))
        .build();

    Map<String, TypedValue> responseMap = new LinkedHashMap<>();

    responseMap.put("payload", expressionLanguage.evaluate("#[payload]", JSON_STREAM_TYPE, bindingContext));
    responseMap.put("attributes", expressionLanguage.evaluate("#[attributes]", JSON_STREAM_TYPE, bindingContext));

    bindingContext = BindingContext.builder()
        .addBinding("payload", new TypedValue(responseMap, fromType(Map.class)))
        .build();

    return expressionLanguage.evaluate("#[payload]", JSON_STREAM_TYPE, bindingContext);
  }


  private DataType toDataType(Object value, Optional<MediaType> mediaType) {
    DataTypeParamsBuilder builder = DataType.builder()
        .type(value.getClass());

    mediaType.ifPresent(builder::mediaType);

    return builder.build();
  }

  private ExtensionExecutionRequest parseRequest(HttpRequest request) {
    try (InputStream entity = request.getEntity().getContent()) {
      BindingContext bindingContext = BindingContext.builder()
          .addBinding("payload", new TypedValue(entity, JSON_STREAM_TYPE))
          .build();

      return (ExtensionExecutionRequest) expressionLanguage.evaluate("#[payload]",
          fromType(ExtensionExecutionRequest.class), bindingContext).getValue();
    } catch (IOException e) {
      throw new MuleRuntimeException(e);
    }
  }

  private String getExtensionName(HttpRequest request) {
    String[] path = request.getPath().split("/");
    return path[path.length - 1];
  }

  @Override
  public void stop() throws MuleException {
    requestHandlerManager.stop();
    requestHandlerManager = null;

    httpServer.stop();
    httpServer = null;
  }
}
