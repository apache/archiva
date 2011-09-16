package org.apache.archiva.web.action.admin.appearance;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.opensymphony.xwork2.validator.ActionValidatorManager;
import org.apache.archiva.web.validator.utils.ValidatorUtil;
import org.apache.archiva.configuration.OrganisationInformation;
import org.apache.archiva.web.action.admin.repositories.DefaultActionValidatorManagerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 */
public class EditOrganizationInfoActionTest
    extends AbstractOrganizationInfoActionTest
{
    private static final String EMPTY_STRING = "";

    // valid inputs
    private static final String ORGANISATION_NAME_VALID_INPUT = "abcXYZ0129.   _/\\~   :?!&=-";

    private static final String ORGANISATION_URL_VALID_INPUT = "file://home/user/abcXYZ0129._/\\~:?!&=-<> ~+[ ]'\"";

    private static final String ORGANISATION_LOGO_VALID_INPUT = "file://home/user/abcXYZ0129._/\\~:?!&=-<> ~+[ ]'\"";

    // invalid inputs
    private static final String ORGANISATION_NAME_INVALID_INPUT = "<>~+[ ]'\"";

    private static final String ORGANISATION_URL_INVALID_INPUT = "/home/user/abcXYZ0129._/\\~:?!&=-<> ~+[ ]'\"";

    private static final String ORGANISATION_LOGO_INVALID_INPUT = "/home/user/abcXYZ0129._/\\~:?!&=-<> ~+[ ]'\"";

    // testing requisite
    private ActionValidatorManager actionValidatorManager;

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        DefaultActionValidatorManagerFactory factory = new DefaultActionValidatorManagerFactory();

        actionValidatorManager = factory.createDefaultActionValidatorManager();
    }

    public void testOrganisationInfoSaves()
        throws Exception
    {
        config.setOrganisationInfo( new OrganisationInformation() );
        OrganisationInformation orginfo = config.getOrganisationInfo();
        orginfo.setLogoLocation( "LOGO" );
        orginfo.setName( "NAME" );
        orginfo.setUrl( "URL" );

        configuration.save( config );

        reloadAction();

        action.prepare();

        assertEquals( "LOGO", action.getOrganisationLogo() );
        assertEquals( "NAME", action.getOrganisationName() );
        assertEquals( "URL", action.getOrganisationUrl() );

        action.setOrganisationLogo( "LOGO1" );
        action.setOrganisationName( "NAME1" );
        action.setOrganisationUrl( "URL1" );

        action.execute();

        orginfo = config.getOrganisationInfo();

        assertEquals( "LOGO1", orginfo.getLogoLocation() );
        assertEquals( "NAME1", orginfo.getName() );
        assertEquals( "URL1", orginfo.getUrl() );
    }

    public void testStruts2ValidationFrameworkWithNullInputs()
        throws Exception
    {
        // prep
        action = getAction();
        populateOrganisationValues( action, null, null, null );

        // test
        actionValidatorManager.validate( action, EMPTY_STRING );

        // verify
        assertTrue( action.hasFieldErrors() );

        Map<String, List<String>> fieldErrors = action.getFieldErrors();

        // make an expected field error object
        Map<String, List<String>> expectedFieldErrors = new HashMap<String, List<String>>();

        // populate
        List<String> expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add( "You must enter a name" );
        expectedFieldErrors.put( "organisationName", expectedErrorMessages );

        ValidatorUtil.assertFieldErrors( expectedFieldErrors, fieldErrors );
    }

    public void testStruts2ValidationFrameworkWithBlankInputs()
        throws Exception
    {
        // prep
        action = getAction();
        populateOrganisationValues( action, EMPTY_STRING, EMPTY_STRING, EMPTY_STRING );

        // test
        actionValidatorManager.validate( action, EMPTY_STRING );

        // verify
        assertTrue( action.hasFieldErrors() );

        Map<String, List<String>> fieldErrors = action.getFieldErrors();

        // make an expected field error object
        Map<String, List<String>> expectedFieldErrors = new HashMap<String, List<String>>();

        // populate
        List<String> expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add( "You must enter a name" );
        expectedFieldErrors.put( "organisationName", expectedErrorMessages );

        ValidatorUtil.assertFieldErrors( expectedFieldErrors, fieldErrors );
    }

    public void testStruts2ValidationFrameworkWithInvalidInputs()
        throws Exception
    {
        // prep
        action = getAction();
        populateOrganisationValues( action, ORGANISATION_NAME_INVALID_INPUT, ORGANISATION_URL_INVALID_INPUT,
                                    ORGANISATION_LOGO_INVALID_INPUT );

        // test
        actionValidatorManager.validate( action, EMPTY_STRING );

        // verify
        assertTrue( action.hasFieldErrors() );

        Map<String, List<String>> fieldErrors = action.getFieldErrors();

        // make an expected field error object
        Map<String, List<String>> expectedFieldErrors = new HashMap<String, List<String>>();

        // populate
        List<String> expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add(
            "Organisation name must only contain alphanumeric characters, white-spaces(' '), equals(=), question-marks(?), exclamation-points(!), ampersands(&), forward-slashes(/), back-slashes(\\), underscores(_), dots(.), colons(:), tildes(~), and dashes(-)." );
        expectedFieldErrors.put( "organisationName", expectedErrorMessages );

        expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add( "You must enter a URL." );
        expectedFieldErrors.put( "organisationUrl", expectedErrorMessages );

        expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add( "You must enter a URL for your logo." );
        expectedFieldErrors.put( "organisationLogo", expectedErrorMessages );

        ValidatorUtil.assertFieldErrors( expectedFieldErrors, fieldErrors );
    }

    public void testStruts2ValidationFrameworkWithValidInputs()
        throws Exception
    {
        // prep
        action = getAction();
        populateOrganisationValues( action, ORGANISATION_NAME_VALID_INPUT, ORGANISATION_URL_VALID_INPUT,
                                    ORGANISATION_LOGO_VALID_INPUT );

        // test
        actionValidatorManager.validate( action, EMPTY_STRING );

        // verify
        assertFalse( action.hasFieldErrors() );
    }

    private void populateOrganisationValues( AbstractAppearanceAction abstractAppearanceAction, String name, String url,
                                             String logo )
    {
        abstractAppearanceAction.setOrganisationName( name );
        abstractAppearanceAction.setOrganisationUrl( url );
        abstractAppearanceAction.setOrganisationLogo( logo );
    }

    @Override
    protected AbstractAppearanceAction getAction()
    {
        //return (EditOrganisationInfoAction) lookup( Action.class.getName(), "editOrganisationInfo" );
        return (EditOrganisationInfoAction) getActionProxy( "/admin/editAppearance.action" ).getAction();
    }
}
