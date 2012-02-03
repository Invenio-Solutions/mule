package org.mule.module.cxf.wssec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;

import java.io.IOException;
import java.util.Map;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.xml.ws.soap.SOAPFaultException;

import org.apache.ws.security.WSPasswordCallback;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class SAMLValidatorTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "org/mule/module/cxf/wssec/saml-validator-conf.xml";
    }

    @Test
    public void testSAMLUnsignedAssertion() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage("me", (Map<String,Object>)null, muleContext);
        MuleClient client = new MuleClient(muleContext);
        MuleMessage received = client.send("vm://greetMe", request);

        assertNotNull(received);
        assertEquals("Hello me", received.getPayloadAsString());
    }

    @Test
    public void testSAMLSignedAssertion() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage("me", (Map<String,Object>)null, muleContext);
        MuleClient client = new MuleClient(muleContext);
        MuleMessage received = client.send("vm://greetMeSigned", request);

        assertNotNull(received);
        assertEquals("Hello me", received.getPayloadAsString());
    }

    public static class PasswordCallbackHandler implements CallbackHandler
    {
        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException
        {
            WSPasswordCallback pc = (WSPasswordCallback) callbacks[0];

            pc.setPassword("keyStorePassword");
        }


    }

}
