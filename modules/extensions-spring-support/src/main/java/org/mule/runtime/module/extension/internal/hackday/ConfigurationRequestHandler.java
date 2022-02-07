/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.hackday;

import static java.lang.String.format;

import org.mule.runtime.api.el.ExpressionLanguage;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;
import org.mule.runtime.http.api.domain.request.HttpRequestContext;
import org.mule.runtime.http.api.server.async.HttpResponseReadyCallback;
import org.mule.runtime.module.extension.internal.config.dsl.config.ConfigurationProviderObjectFactory;
import org.mule.runtime.module.extension.internal.config.dsl.connection.ConnectionProviderObjectFactory;
import org.mule.runtime.module.extension.internal.runtime.resolver.ConnectionProviderResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticValueResolver;

import java.util.HashMap;
import java.util.Map;

public class ConfigurationRequestHandler extends AbstractExtensionRequestHandler {

  private final ExtensionManager extensionManager;
  private final ExpressionManager expressionManager;
  private final MuleContext muleContext;

  public ConfigurationRequestHandler(ExpressionLanguage expressionLanguage,
                                     ExtensionManager extensionManager,
                                     ExpressionManager expressionManager,
                                     MuleContext muleContext) {
    super(expressionLanguage);
    this.extensionManager = extensionManager;
    this.muleContext = muleContext;
    this.expressionManager = expressionManager;
  }

  @Override
  protected void doHandle(HttpRequestContext ctx, HttpResponseReadyCallback responseCallback) throws Exception {
    final HttpRequest httpRequest = ctx.getRequest();
    final ConfigurationDescriptor configDescriptor = parseRequest(httpRequest, ConfigurationDescriptor.class);

    ExtensionModel extensionModel = getExtensionModel(configDescriptor);
    ConfigurationModel configurationModel = getConfigurationModel(extensionModel, configDescriptor);

    ConfigurationProviderObjectFactory factory = new ConfigurationProviderObjectFactory(
        extensionModel,
        configurationModel,
        muleContext);

    factory.setParameters(getParametersMap(configDescriptor));

    ConnectionProviderResolver connectionProviderResolver = getConnectionProviderResolver(extensionModel, configurationModel, configDescriptor);
    if (connectionProviderResolver != null) {
      factory.setConnectionProviderResolver(connectionProviderResolver);
    }

    ConfigurationProvider configurationProvider = factory.getObject();

    extensionManager.registerConfigurationProvider(configurationProvider);

    responseCallback.responseReady(HttpResponse.builder()
        .statusCode(200)
        .entity(new ByteArrayHttpEntity("Config created".getBytes()))
        .build(), NULL_STATUS_CALLBACK);

  }

  private ConnectionProviderResolver getConnectionProviderResolver(ExtensionModel extensionModel,
                                                                   ConfigurationModel configurationModel,
                                                                   ConfigurationDescriptor configDescriptor) {
    return configDescriptor.getConnection()
        .map(connection -> {
          ConnectionProviderModel model = getConnectionProviderModel(configurationModel, connection);
          ConnectionProviderObjectFactory factory = new ConnectionProviderObjectFactory(model,
              extensionModel,
              null,
              null,
              null,
              muleContext);

          factory.setParameters(getParametersMap(connection));

          try {
            return factory.getObject();
          } catch (Exception e) {
            throw new MuleRuntimeException(e);
          }
        })
        .orElse(null);
  }

  private Map<String, Object> getParametersMap(ComponentDescriptor descriptor) {
    Map<String, Object> parameters = new HashMap<>();
    descriptor.getParameters().forEach((key, value) -> parameters.put(key, new StaticValueResolver<>(value)));
    return parameters;
  }

  private ExtensionModel getExtensionModel(ConfigurationDescriptor descriptor) {
    return extensionManager.getExtension(descriptor.getExtensionName())
        .orElseThrow(() -> new IllegalArgumentException(format("Extension '%s' not found", descriptor.getExtensionName())));
  }

  private ConfigurationModel getConfigurationModel(ExtensionModel extensionModel, ConfigurationDescriptor descriptor) {
    return extensionModel.getConfigurationModel(descriptor.getComponentName())
        .orElseThrow(() -> new IllegalArgumentException(format("Configuration '%s' not found in Extension '%s'",
            descriptor.getComponentName(), extensionModel.getName())));
  }

  private ConnectionProviderModel getConnectionProviderModel(ConfigurationModel configModel, ComponentDescriptor descriptor) {
    return configModel.getConnectionProviderModel(descriptor.getComponentName())
        .orElseThrow(() -> new IllegalArgumentException(format("ConnectionProvider '%s' not found in config '%s'",
            descriptor.getComponentName(), configModel.getName())));
  }
}
