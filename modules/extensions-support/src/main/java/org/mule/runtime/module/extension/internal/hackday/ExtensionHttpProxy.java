/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.hackday;

import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;
import org.mule.runtime.http.api.server.HttpServer;
import org.mule.runtime.http.api.server.HttpServerConfiguration;
import org.mule.runtime.http.api.server.RequestHandlerManager;
import org.mule.runtime.http.api.server.async.HttpResponseReadyCallback;
import org.mule.runtime.http.api.server.async.ResponseStatusCallback;

import java.io.IOException;

import javax.inject.Inject;

public class ExtensionHttpProxy implements Startable, Stoppable {

  @Inject
  private ExtensionManager extensionManager;

  @Inject
  private HttpService httpService;

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

  private void handleRequest(HttpRequest request, HttpResponseReadyCallback responseCallback) {
    request.getPath();
    responseCallback.responseReady(HttpResponse.builder()
        .statusCode(200)
        .entity(new ByteArrayHttpEntity("dummy".getBytes()))
        .build(), new ResponseStatusCallback() {
      @Override
      public void responseSendFailure(Throwable throwable) {

      }

      @Override
      public void responseSendSuccessfully() {

      }
    });
  }

  @Override
  public void stop() throws MuleException {
    requestHandlerManager.stop();
    requestHandlerManager = null;

    httpServer.stop();
    httpServer = null;
  }
}
