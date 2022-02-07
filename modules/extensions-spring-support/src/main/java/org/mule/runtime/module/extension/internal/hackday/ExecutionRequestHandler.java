/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.hackday;

import static org.mule.runtime.api.metadata.DataType.fromType;

import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.ExpressionLanguage;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.extension.api.client.DefaultOperationParameters;
import org.mule.runtime.extension.api.client.DefaultOperationParametersBuilder;
import org.mule.runtime.extension.api.client.ExtensionsClient;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.runtime.http.api.domain.entity.HttpEntity;
import org.mule.runtime.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;
import org.mule.runtime.http.api.domain.request.HttpRequestContext;
import org.mule.runtime.http.api.server.async.HttpResponseReadyCallback;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

public class ExecutionRequestHandler extends AbstractExtensionRequestHandler {

  private final ExtensionsClient extensionsClient;

  public ExecutionRequestHandler(ExtensionsClient extensionsClient, ExpressionLanguage expressionLanguage) {
    super(expressionLanguage);
    this.extensionsClient = extensionsClient;
  }

  @Override
  protected void doHandle(HttpRequestContext ctx, HttpResponseReadyCallback responseCallback) throws Exception{
    final HttpRequest httpRequest = ctx.getRequest();
    ExecutionRequest executionRequest = parseRequest(httpRequest, ExecutionRequest.class);

    String configRef = executionRequest.getConfigRef();

    DefaultOperationParametersBuilder builder = DefaultOperationParameters.builder()
        .configName(configRef);

    executionRequest.getParameters().forEach(builder::addParameter);

    Result result = extensionsClient.execute(executionRequest.getExtensionName(),
        executionRequest.getOperation(),
        builder.build());

    TypedValue response = serializeResult(result);

    responseCallback.responseReady(HttpResponse.builder()
        .statusCode(200)
        .entity(toHttpEntity(response.getValue()))
        .build(), streamingCallback(response));
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
}
