package org.apache.maven.archiva.web.action.admin.legacy;

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

import com.opensymphony.xwork2.validator.ActionValidatorManager;
import junit.framework.TestCase;
import org.apache.archiva.admin.model.admin.LegacyArtifactPath;
import org.apache.archiva.web.validator.utils.ValidatorUtil;
import org.apache.maven.archiva.web.action.admin.repositories.DefaultActionValidatorManagerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddLegacyArtifactPathActionTest
    extends TestCase
{
    private static final String EMPTY_STRING = "";

    // valid inputs
    private static final String LEGACY_ARTIFACT_PATH_PATH_VALID_INPUT = "-abcXYZ0129._/\\";

    private static final String GROUP_ID_VALID_INPUT = "abcXYZ0129._-";

    private static final String ARTIFACT_ID_VALID_INPUT = "abcXYZ0129._-";

    private static final String VERSION_VALID_INPUT = "abcXYZ0129._-";

    private static final String CLASSIFIER_VALID_INPUT = "abcXYZ0129._-";

    private static final String TYPE_VALID_INPUT = "abcXYZ0129._-";

    // invalid inputs
    private static final String LEGACY_ARTIFACT_PATH_PATH_INVALID_INPUT = "<> ~+[ ]'\"";

    private static final String GROUP_ID_INVALID_INPUT = "<> \\/~+[ ]'\"";

    private static final String ARTIFACT_ID_INVALID_INPUT = "<> \\/~+[ ]'\"";

    private static final String VERSION_INVALID_INPUT = "<> \\/~+[ ]'\"";

    private static final String CLASSIFIER_INVALID_INPUT = "<> \\/~+[ ]'\"";

    private static final String TYPE_INVALID_INPUT = "<> \\/~+[ ]'\"";

    // testing requisite
    private AddLegacyArtifactPathAction addLegacyArtifactPathAction;

    private ActionValidatorManager actionValidatorManager;

    @Override
    public void setUp()
        throws Exception
    {
        addLegacyArtifactPathAction = new AddLegacyArtifactPathAction();

        DefaultActionValidatorManagerFactory factory = new DefaultActionValidatorManagerFactory();

        actionValidatorManager = factory.createDefaultActionValidatorManager();
    }

    public void testStruts2ValidationFrameworkWithNullInputs()
        throws Exception
    {
        // prep
        LegacyArtifactPath legacyArtifactPath = createLegacyArtifactPath( null );
        populateAddLegacyArtifactPathActionFields( addLegacyArtifactPathAction, legacyArtifactPath, null, null, null,
                                                   null, null );

        // test
        actionValidatorManager.validate( addLegacyArtifactPathAction, EMPTY_STRING );

        // verify
        assertTrue( addLegacyArtifactPathAction.hasFieldErrors() );

        Map<String, List<String>> fieldErrors = addLegacyArtifactPathAction.getFieldErrors();

        // make an expected field error object
        Map<String, List<String>> expectedFieldErrors = new HashMap<String, List<String>>();

        // populate
        List<String> expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add( "You must enter a legacy path." );
        expectedFieldErrors.put( "legacyArtifactPath.path", expectedErrorMessages );

        expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add( "You must enter a groupId." );
        expectedFieldErrors.put( "groupId", expectedErrorMessages );

        expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add( "You must enter an artifactId." );
        expectedFieldErrors.put( "artifactId", expectedErrorMessages );

        expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add( "You must enter a version." );
        expectedFieldErrors.put( "version", expectedErrorMessages );

        expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add( "You must enter a type." );
        expectedFieldErrors.put( "type", expectedErrorMessages );

        ValidatorUtil.assertFieldErrors( expectedFieldErrors, fieldErrors );
    }

    public void testStruts2ValidationFrameworkWithBlankInputs()
        throws Exception
    {
        // prep
        LegacyArtifactPath legacyArtifactPath = createLegacyArtifactPath( EMPTY_STRING );
        populateAddLegacyArtifactPathActionFields( addLegacyArtifactPathAction, legacyArtifactPath, EMPTY_STRING,
                                                   EMPTY_STRING, EMPTY_STRING, EMPTY_STRING, EMPTY_STRING );

        // test
        actionValidatorManager.validate( addLegacyArtifactPathAction, EMPTY_STRING );

        // verify
        assertTrue( addLegacyArtifactPathAction.hasFieldErrors() );

        Map<String, List<String>> fieldErrors = addLegacyArtifactPathAction.getFieldErrors();

        // make an expected field error object
        Map<String, List<String>> expectedFieldErrors = new HashMap<String, List<String>>();

        // populate
        List<String> expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add( "You must enter a legacy path." );
        expectedFieldErrors.put( "legacyArtifactPath.path", expectedErrorMessages );

        expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add( "You must enter a groupId." );
        expectedFieldErrors.put( "groupId", expectedErrorMessages );

        expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add( "You must enter an artifactId." );
        expectedFieldErrors.put( "artifactId", expectedErrorMessages );

        expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add( "You must enter a version." );
        expectedFieldErrors.put( "version", expectedErrorMessages );

        expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add( "You must enter a type." );
        expectedFieldErrors.put( "type", expectedErrorMessages );

        ValidatorUtil.assertFieldErrors( expectedFieldErrors, fieldErrors );
    }

    public void testStruts2ValidationFrameworkWithInvalidInputs()
        throws Exception
    {
        // prep
        LegacyArtifactPath legacyArtifactPath = createLegacyArtifactPath( LEGACY_ARTIFACT_PATH_PATH_INVALID_INPUT );
        populateAddLegacyArtifactPathActionFields( addLegacyArtifactPathAction, legacyArtifactPath,
                                                   GROUP_ID_INVALID_INPUT, ARTIFACT_ID_INVALID_INPUT,
                                                   VERSION_INVALID_INPUT, CLASSIFIER_INVALID_INPUT,
                                                   TYPE_INVALID_INPUT );

        // test
        actionValidatorManager.validate( addLegacyArtifactPathAction, EMPTY_STRING );

        // verify
        assertTrue( addLegacyArtifactPathAction.hasFieldErrors() );

        Map<String, List<String>> fieldErrors = addLegacyArtifactPathAction.getFieldErrors();

        // make an expected field error object
        Map<String, List<String>> expectedFieldErrors = new HashMap<String, List<String>>();

        // populate
        List<String> expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add(
            "Legacy path must only contain alphanumeric characters, forward-slashes(/), back-slashes(\\), underscores(_), dots(.), and dashes(-)." );
        expectedFieldErrors.put( "legacyArtifactPath.path", expectedErrorMessages );

        expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add(
            "Group id must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
        expectedFieldErrors.put( "groupId", expectedErrorMessages );

        expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add(
            "Artifact id must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
        expectedFieldErrors.put( "artifactId", expectedErrorMessages );

        expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add(
            "Version must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
        expectedFieldErrors.put( "version", expectedErrorMessages );

        expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add(
            "Classifier must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
        expectedFieldErrors.put( "classifier", expectedErrorMessages );

        expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add(
            "Type must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
        expectedFieldErrors.put( "type", expectedErrorMessages );

        ValidatorUtil.assertFieldErrors( expectedFieldErrors, fieldErrors );
    }

    public void testStruts2ValidationFrameworkWithValidInputs()
        throws Exception
    {
        // prep
        LegacyArtifactPath legacyArtifactPath = createLegacyArtifactPath( LEGACY_ARTIFACT_PATH_PATH_VALID_INPUT );
        populateAddLegacyArtifactPathActionFields( addLegacyArtifactPathAction, legacyArtifactPath,
                                                   GROUP_ID_VALID_INPUT, ARTIFACT_ID_VALID_INPUT, VERSION_VALID_INPUT,
                                                   CLASSIFIER_VALID_INPUT, TYPE_VALID_INPUT );

        // test
        actionValidatorManager.validate( addLegacyArtifactPathAction, EMPTY_STRING );

        // verify
        assertFalse( addLegacyArtifactPathAction.hasFieldErrors() );
    }

    private LegacyArtifactPath createLegacyArtifactPath( String path )
    {
        LegacyArtifactPath legacyArtifactPath = new LegacyArtifactPath();
        legacyArtifactPath.setPath( path );
        return legacyArtifactPath;
    }

    private void populateAddLegacyArtifactPathActionFields( AddLegacyArtifactPathAction addLegacyArtifactPathAction,
                                                            LegacyArtifactPath legacyArtifactPath, String groupId,
                                                            String artifactId, String version, String classifier,
                                                            String type )
    {
        addLegacyArtifactPathAction.setLegacyArtifactPath( legacyArtifactPath );
        addLegacyArtifactPathAction.setGroupId( groupId );
        addLegacyArtifactPathAction.setArtifactId( artifactId );
        addLegacyArtifactPathAction.setVersion( version );
        addLegacyArtifactPathAction.setClassifier( classifier );
        addLegacyArtifactPathAction.setType( type );
    }
}
