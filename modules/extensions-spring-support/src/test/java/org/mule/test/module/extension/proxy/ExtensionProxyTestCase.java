/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.proxy;

import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.client.HttpClient;
import org.mule.runtime.http.api.client.HttpClientConfiguration;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import javax.inject.Inject;

import org.junit.Test;

public class ExtensionProxyTestCase extends AbstractExtensionFunctionalTestCase {

//  @Inject
//  private ExtensionHttpProxy proxy;

  @Inject
  private HttpService httpService;

  private HttpClient httpClient;

  @Override
  protected String getConfigFile() {
    return "heisenberg-config.xml";
  }

  @Test
  public void proxy() throws Exception {
    HttpResponse response = httpClient.send(HttpRequest.builder()
        .uri("http://0.0.0.0:8080/extension/heisenberg")
        .method("POST")
        .build());

    response.toString();
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();

    httpClient = httpService.getClientFactory().create(new HttpClientConfiguration.Builder()
        .setName("proxyClient")
        .build());

    httpClient.start();
  }

  @Override
  protected void doTearDown() throws Exception {
    super.doTearDown();

    if (httpClient != null) {
      httpClient.stop();
    }
  }
}
