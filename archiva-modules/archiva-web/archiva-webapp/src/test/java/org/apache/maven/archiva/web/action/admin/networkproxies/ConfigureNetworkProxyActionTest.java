package org.apache.maven.archiva.web.action.admin.networkproxies;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.opensymphony.xwork2.ObjectFactory;
import com.opensymphony.xwork2.validator.ActionValidatorManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.opensymphony.xwork2.validator.DefaultActionValidatorManager;
import junit.framework.TestCase;
import org.apache.maven.archiva.configuration.NetworkProxyConfiguration;
import org.apache.maven.archiva.web.action.admin.repositories.DefaultActionValidatorManagerFactory;
import org.apache.maven.archiva.web.validator.utils.ValidatorUtil;

public class ConfigureNetworkProxyActionTest extends TestCase
{
    private static final String EMPTY_STRING = "";

    private static final String VALIDATION_CONTEXT = "saveNetworkProxy";

    // valid inputs
    private static final String PROXY_ID_VALID_INPUT = "abcXYZ0129._-";

    private static final String PROXY_PROTOCOL_VALID_INPUT = "-abcXYZ0129./:\\";

    private static final String PROXY_HOST_VALID_INPUT = "abcXYZ0129._/\\~:?!&=-";

    private static final int PROXY_PORT_VALID_INPUT = 8080;

    private static final String PROXY_USERNAME_VALID_INPUT = "abcXYZ0129.@/_-\\";

    // invalid inputs
    private static final String PROXY_ID_INVALID_INPUT = "<> \\/~+[ ]'\"";

    private static final String PROXY_PROTOCOL_INVALID_INPUT = "<> ~+[ ]'\"";

    private static final String PROXY_HOST_INVALID_INPUT = "<> ~+[ ]'\"";

    private static final int PROXY_PORT_INVALID_INPUT = 0;

    private static final String PROXY_USERNAME_INVALID_INPUT = "<> ~+[ ]'\"";

    // testing requisite
    private ConfigureNetworkProxyAction configureNetworkProxyAction;

    private ActionValidatorManager actionValidatorManager;
    
    @Override
    public void setUp()
        throws Exception
    {
        configureNetworkProxyAction = new ConfigureNetworkProxyAction();

        DefaultActionValidatorManagerFactory factory = new DefaultActionValidatorManagerFactory();

        actionValidatorManager = factory.createDefaultActionValidatorManager();
    }

    public void testStruts2ValidationFrameworkWithNullInputs() throws Exception
    {
        // prep
        NetworkProxyConfiguration networkProxyConfiguration = createNetworkProxyConfiguration(null, null, null, null);
        configureNetworkProxyAction.setProxy(networkProxyConfiguration);

        // test
        actionValidatorManager.validate(configureNetworkProxyAction, VALIDATION_CONTEXT);

        // verify
        assertTrue(configureNetworkProxyAction.hasFieldErrors());

        Map<String, List<String>> fieldErrors = configureNetworkProxyAction.getFieldErrors();

        // make an expected field error object
        Map<String, List<String>> expectedFieldErrors = new HashMap<String, List<String>>();

        // populate
        List<String> expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add("You must enter an identifier.");
        expectedFieldErrors.put("proxy.id", expectedErrorMessages);

        expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add("You must enter a protocol.");
        expectedFieldErrors.put("proxy.protocol", expectedErrorMessages);

        expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add("You must enter a host.");
        expectedFieldErrors.put("proxy.host", expectedErrorMessages);

        ValidatorUtil.assertFieldErrors(expectedFieldErrors, fieldErrors);
    }

    public void testStruts2ValidationFrameworkWithBlankInputs() throws Exception
    {
        // prep
        NetworkProxyConfiguration networkProxyConfiguration = createNetworkProxyConfiguration(EMPTY_STRING, EMPTY_STRING, EMPTY_STRING, EMPTY_STRING);
        configureNetworkProxyAction.setProxy(networkProxyConfiguration);

        // test
        actionValidatorManager.validate(configureNetworkProxyAction, VALIDATION_CONTEXT);

        // verify
        assertTrue(configureNetworkProxyAction.hasFieldErrors());

        Map<String, List<String>> fieldErrors = configureNetworkProxyAction.getFieldErrors();

        // make an expected field error object
        Map<String, List<String>> expectedFieldErrors = new HashMap<String, List<String>>();

        // populate
        List<String> expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add("You must enter an identifier.");
        expectedFieldErrors.put("proxy.id", expectedErrorMessages);

        expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add("You must enter a protocol.");
        expectedFieldErrors.put("proxy.protocol", expectedErrorMessages);

        expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add("You must enter a host.");
        expectedFieldErrors.put("proxy.host", expectedErrorMessages);

        ValidatorUtil.assertFieldErrors(expectedFieldErrors, fieldErrors);
    }

    public void testStruts2ValidationFrameworkWithInvalidInputs() throws Exception
    {
        // prep
        NetworkProxyConfiguration networkProxyConfiguration = createNetworkProxyConfiguration(PROXY_ID_INVALID_INPUT, PROXY_HOST_INVALID_INPUT, PROXY_PORT_INVALID_INPUT, PROXY_PROTOCOL_INVALID_INPUT, PROXY_USERNAME_INVALID_INPUT);
        configureNetworkProxyAction.setProxy(networkProxyConfiguration);

        // test
        actionValidatorManager.validate(configureNetworkProxyAction, VALIDATION_CONTEXT);

        // verify
        assertTrue(configureNetworkProxyAction.hasFieldErrors());

        Map<String, List<String>> fieldErrors = configureNetworkProxyAction.getFieldErrors();

        // make an expected field error object
        Map<String, List<String>> expectedFieldErrors = new HashMap<String, List<String>>();

        // populate
        List<String> expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add("Proxy id must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-).");
        expectedFieldErrors.put("proxy.id", expectedErrorMessages);

        expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add("Protocol must only contain alphanumeric characters, forward-slashes(/), back-slashes(\\), dots(.), colons(:), and dashes(-).");
        expectedFieldErrors.put("proxy.protocol", expectedErrorMessages);

        expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add("Host must only contain alphanumeric characters, equals(=), question-marks(?), exclamation-points(!), ampersands(&), forward-slashes(/), back-slashes(\\), underscores(_), dots(.), colons(:), tildes(~), and dashes(-).");
        expectedFieldErrors.put("proxy.host", expectedErrorMessages);

        expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add("Port needs to be larger than 1");
        expectedFieldErrors.put("proxy.port", expectedErrorMessages);

        expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add("Username must only contain alphanumeric characters, at's(@), forward-slashes(/), back-slashes(\\), underscores(_), dots(.), and dashes(-).");
        expectedFieldErrors.put("proxy.username", expectedErrorMessages);

        ValidatorUtil.assertFieldErrors(expectedFieldErrors, fieldErrors);
    }

    public void testStruts2ValidationFrameworkWithValidInputs() throws Exception
    {
        // prep
        NetworkProxyConfiguration networkProxyConfiguration = createNetworkProxyConfiguration(PROXY_ID_VALID_INPUT, PROXY_HOST_VALID_INPUT, PROXY_PORT_VALID_INPUT, PROXY_PROTOCOL_VALID_INPUT, PROXY_USERNAME_VALID_INPUT);
        configureNetworkProxyAction.setProxy(networkProxyConfiguration);

        // test
        actionValidatorManager.validate(configureNetworkProxyAction, VALIDATION_CONTEXT);

        // verify
        assertFalse(configureNetworkProxyAction.hasFieldErrors());
    }

    private NetworkProxyConfiguration createNetworkProxyConfiguration(String id, String host, int port, String protocol, String username)
    {
        NetworkProxyConfiguration networkProxyConfiguration = new NetworkProxyConfiguration();
        networkProxyConfiguration.setId(id);
        networkProxyConfiguration.setHost(host);
        networkProxyConfiguration.setPort(port);
        networkProxyConfiguration.setProtocol(protocol);
        networkProxyConfiguration.setUsername(username);
        return networkProxyConfiguration;
    }

    // over-loaded
    // for simulating empty/null form purposes; excluding primitive data-typed values
    private NetworkProxyConfiguration createNetworkProxyConfiguration(String id, String host, String protocol, String username)
    {
        NetworkProxyConfiguration networkProxyConfiguration = new NetworkProxyConfiguration();
        networkProxyConfiguration.setId(id);
        networkProxyConfiguration.setHost(host);
        networkProxyConfiguration.setProtocol(protocol);
        networkProxyConfiguration.setUsername(username);
        return networkProxyConfiguration;
    }
}
