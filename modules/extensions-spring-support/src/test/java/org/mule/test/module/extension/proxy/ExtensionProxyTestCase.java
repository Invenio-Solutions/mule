/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.proxy;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.client.HttpClient;
import org.mule.runtime.http.api.client.HttpClientConfiguration;
import org.mule.runtime.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import javax.inject.Inject;

import org.junit.Test;

public class ExtensionProxyTestCase extends AbstractExtensionFunctionalTestCase {

  @Inject
  private HttpService httpService;

  private HttpClient httpClient;

  @Override
  protected String[] getConfigFiles() {
    return new String[]{};
  }

  @Test
  public void createConfigAndExecute() throws Exception {
    String requestBody = "{\n" +
        "\t\"extensionName\": \"petstore\",\n" +
        "\t\"componentName\": \"config\",\n" +
        "\t\"parameters\": {\n" +
        "\t\t\"name\": \"myMagicConfig\",\n" +
        "\t\t\"pets\": [\"Lassie\", \"Sapo Pepe\", \"My mother's parrot\"]\n" +
        "\t},\n" +
        "\t\"connection\": {\n" +
        "\t\t\"componentName\": \"connection\",\n" +
        "\t\t\"parameters\": {\n" +
        "\t\t\t\"username\": \"john\",\n" +
        "\t\t\t\"password\": \"doe\"\n" +
        "\t\t}\n" +
        "\t}\n" +
        "}";

    HttpResponse response = httpClient.send(HttpRequest.builder()
        .uri("http://0.0.0.0:8080/extension/config")
        .method("POST")
        .entity(new ByteArrayHttpEntity(requestBody.getBytes()))
        .build());

    assertThat(response.getStatusCode(), is(200));
    String responseBody = IOUtils.toString(response.getEntity().getContent());
    System.out.println(responseBody);

    invokeOperation();
  }

  private void invokeOperation() throws Exception {
    String requestBody = "{\n" +
        "\t\"extensionName\": \"petstore\",\n" +
        "\t\"configRef\": \"myMagicConfig\",\n" +
        "\t\"operation\": \"getPets\",\n" +
        "\t\"parameters\": {\n" +
        "\t\t\"ownerName\": \"john\"\n" +
        "\t}\n" +
        "}\n";

    HttpResponse response = httpClient.send(HttpRequest.builder()
        .uri("http://0.0.0.0:8080/extension/execution")
        .method("POST")
        .entity(new ByteArrayHttpEntity(requestBody.getBytes()))
        .build());

    assertThat(response.getStatusCode(), is(200));
    String responseBody = IOUtils.toString(response.getEntity().getContent());
    System.out.println(responseBody);
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
