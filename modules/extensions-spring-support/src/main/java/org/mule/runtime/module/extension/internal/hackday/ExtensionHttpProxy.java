/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.hackday;

import org.mule.runtime.api.el.ExpressionLanguage;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.extension.api.client.ExtensionsClient;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.server.HttpServer;
import org.mule.runtime.http.api.server.HttpServerConfiguration;
import org.mule.runtime.http.api.server.RequestHandlerManager;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

import java.io.IOException;

import javax.inject.Inject;

public class ExtensionHttpProxy implements Startable, Stoppable {

  @Inject
  private ExtensionManager extensionManager;

  @Inject
  private HttpService httpService;

  @Inject
  private ExtensionsClient extensionsClient;

  @Inject
  private ExpressionLanguage expressionLanguage;

  @Inject
  private ExpressionManager expressionManager;

  @Inject
  private ReflectionCache reflectionCache;

  @Inject
  private MuleContext muleContext;

  private HttpServer httpServer;
  private RequestHandlerManager executionHandlerManager;
  private RequestHandlerManager configHandlerManager;

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

    executionHandlerManager = httpServer.addRequestHandler("/extension/execution", new ExecutionRequestHandler(extensionsClient, expressionLanguage));
    configHandlerManager = httpServer.addRequestHandler("/extension/config",
        new ConfigurationRequestHandler(expressionLanguage, extensionManager, expressionManager, muleContext));
  }

  @Override
  public void stop() throws MuleException {
    stop(executionHandlerManager);
    executionHandlerManager = null;

    stop(configHandlerManager);
    configHandlerManager = null;

    if (httpServer != null) {
      httpServer.stop();
      httpServer = null;
    }
  }

  private void stop(RequestHandlerManager handler) {
    if (handler != null) {
      handler.stop();
    }
  }
}
