/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.request.client;


import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.MessageExchangePattern.ONE_WAY;
import static org.mule.MessageExchangePattern.REQUEST_RESPONSE;
import static org.mule.module.http.api.HttpConstants.Methods.POST;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import static org.mule.module.http.api.requester.HttpStreamingType.ALWAYS;

import org.mule.api.MuleContext;
import org.mule.api.client.SimpleOptionsBuilder;
import org.mule.api.processor.MessageProcessor;
import org.mule.module.http.api.client.HttpRequestOptions;
import org.mule.module.http.api.requester.HttpStreamingType;
import org.mule.module.http.internal.request.DefaultHttpRequesterConfig;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;

@SmallTest
public class HttpConnectorMessageProcessorProviderTestCase extends AbstractMuleTestCase
{

    private static final String PATH_URL = "http://localhost:8080/path";
    private static final String ANOTHER_PATH = "http://localhost:8080/another";


    private MuleContext mockMuleContext = mock(MuleContext.class, Answers.RETURNS_DEEP_STUBS.get());
    private DefaultHttpRequesterConfig mockRequestConfig  = mock(DefaultHttpRequesterConfig.class, Answers.RETURNS_DEEP_STUBS.get());

    private final HttpConnectorMessageProcessorProvider httpConnectorMessageProcessorProvider = new HttpConnectorMessageProcessorProvider();

    @Before
    public void setUpTest()
    {
        when(mockMuleContext.getRegistry().get(HttpRequesterBuilder.DEFAULT_HTTP_REQUEST_CONFIG_NAME)).thenReturn(mockRequestConfig);
        httpConnectorMessageProcessorProvider.setMuleContext(mockMuleContext);
    }

    @Test
    public void sameConfigReturnsSameInstanceUsingGenericOptions() throws Exception
    {
        final MessageProcessor messageProcessor = httpConnectorMessageProcessorProvider.getMessageProcessor(PATH_URL, SimpleOptionsBuilder.newOptions().build(), REQUEST_RESPONSE);
        assertThat(httpConnectorMessageProcessorProvider.getMessageProcessor(PATH_URL, SimpleOptionsBuilder.newOptions().build(), REQUEST_RESPONSE), is(messageProcessor));
    }

    @Test
    public void sameConfigReturnsSameInstanceUsingDifferentGenericOptions() throws Exception
    {
        final MessageProcessor messageProcessor = httpConnectorMessageProcessorProvider.getMessageProcessor(PATH_URL, SimpleOptionsBuilder.newOptions().build(), REQUEST_RESPONSE);
        assertThat(httpConnectorMessageProcessorProvider.getMessageProcessor(PATH_URL, SimpleOptionsBuilder.newOptions().responseTimeout(1000).build(), REQUEST_RESPONSE), not(is(messageProcessor)));
    }

    @Test
    public void sameConfigReturnsSameInstanceUsingDifferentHttpOptions() throws Exception
    {
        final MessageProcessor messageProcessor = httpConnectorMessageProcessorProvider.getMessageProcessor(PATH_URL, newOptions().requestStreamingMode(ALWAYS).build(), REQUEST_RESPONSE);
        assertThat(httpConnectorMessageProcessorProvider.getMessageProcessor(PATH_URL, newOptions().disableParseResponse().build(), REQUEST_RESPONSE), not(is(messageProcessor)));
    }

    @Test
    public void sameConfigReturnsSameInstanceUsingCompleteHttpOptions() throws Exception
    {
        HttpRequestOptions requestOptions = createFullHttpRequestOptions();
        final MessageProcessor messageProcessor = httpConnectorMessageProcessorProvider.getMessageProcessor(PATH_URL, requestOptions, REQUEST_RESPONSE);
        assertThat(httpConnectorMessageProcessorProvider.getMessageProcessor(PATH_URL, requestOptions, REQUEST_RESPONSE), is(messageProcessor));
    }

    @Test
    public void differentPathReturnsDifferentOperations() throws Exception
    {
        final MessageProcessor messageProcessor = httpConnectorMessageProcessorProvider.getMessageProcessor(PATH_URL, newOptions().build(), ONE_WAY);
        assertThat(httpConnectorMessageProcessorProvider.getMessageProcessor(ANOTHER_PATH, newOptions().build(), ONE_WAY), not(is(messageProcessor)));
    }

    @Test
    public void differentExchangePatternsReturnsDifferentOperations() throws Exception
    {
        final MessageProcessor messageProcessor = httpConnectorMessageProcessorProvider.getMessageProcessor(PATH_URL, newOptions().build(), ONE_WAY);
        assertThat(httpConnectorMessageProcessorProvider.getMessageProcessor(PATH_URL, newOptions().build(), REQUEST_RESPONSE), not(is(messageProcessor)));
    }

    @Test
    public void disposeInvalidatesCache() throws Exception
    {
        final MessageProcessor messageProcessor = httpConnectorMessageProcessorProvider.getMessageProcessor(PATH_URL, SimpleOptionsBuilder.newOptions().build(), REQUEST_RESPONSE);
        httpConnectorMessageProcessorProvider.dispose();
        assertThat(httpConnectorMessageProcessorProvider.getMessageProcessor(PATH_URL, SimpleOptionsBuilder.newOptions().build(), REQUEST_RESPONSE), not(is(messageProcessor)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void doNotAllowNullUrl() throws Exception
    {
        httpConnectorMessageProcessorProvider.getMessageProcessor(null, newOptions().build(), ONE_WAY);
    }

    @Test(expected = IllegalArgumentException.class)
    public void doNotAllowNullOptions() throws Exception
    {
        httpConnectorMessageProcessorProvider.getMessageProcessor(PATH_URL, null, ONE_WAY);
    }

    @Test(expected = IllegalArgumentException.class)
    public void doNotAllowNullExchangePattern() throws Exception
    {
        httpConnectorMessageProcessorProvider.getMessageProcessor(PATH_URL, newOptions().build(), null);
    }

    private HttpRequestOptions createFullHttpRequestOptions()
    {
        return newOptions().requestStreamingMode(ALWAYS).disableFollowsRedirect().disableParseResponse().disableStatusCodeValidation().method(POST.name()).requestConfig(mockRequestConfig).responseTimeout(1000).build();
    }

}
