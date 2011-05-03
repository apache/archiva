package org.apache.maven.archiva.web.action;

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
import com.opensymphony.xwork2.validator.ActionValidatorManagerFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import junit.framework.TestCase;
import org.apache.maven.archiva.web.validator.utils.ValidatorUtil;

public class DeleteArtifactActionTest extends TestCase
{
    private static final String EMPTY_STRING = "";

    // valid inputs
    private static final String GROUP_ID_VALID_INPUT = "abcXYZ0129._-";

    private static final String ARTIFACT_ID_VALID_INPUT = "abcXYZ0129._-";

    private static final String VERSION_VALID_INPUT = "1.2.3";

    private static final String REPOSITORY_ID_VALID_INPUT = "abcXYZ0129._-";

    // invalid inputs
    private static final String GROUP_ID_INVALID_INPUT = "<> \\/~+[ ]'\"";

    private static final String ARTIFACT_ID_INVALID_INPUT = "<> \\/~+[ ]'\"";

    private static final String VERSION_INVALID_INPUT = "<>";

    private static final String REPOSITORY_ID_INVALID_INPUT = "<> \\/~+[ ]'\"";

    // testing requisite
    private DeleteArtifactAction deleteArtifactAction;

    private ActionValidatorManager actionValidatorManager;

    @Override
    public void setUp() throws Exception
    {
        deleteArtifactAction = new DeleteArtifactAction();
        ObjectFactory.setObjectFactory(new ObjectFactory());
        actionValidatorManager = ActionValidatorManagerFactory.getInstance();
    }

    public void testStruts2ValidationFrameworkWithNullInputs() throws Exception
    {
        // prep
        populateDeleteArtifactActionFields(deleteArtifactAction, null, null, null, null);

        // test
        actionValidatorManager.validate(deleteArtifactAction, EMPTY_STRING);

        // verify
        assertTrue(deleteArtifactAction.hasFieldErrors());

        Map<String, List<String>> fieldErrors = deleteArtifactAction.getFieldErrors();

        // make an expected field error object
        Map<String, List<String>> expectedFieldErrors = new HashMap<String, List<String>>();

        // populate
        List<String> expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add("You must enter a groupId.");
        expectedFieldErrors.put("groupId", expectedErrorMessages);

        expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add("You must enter an artifactId.");
        expectedFieldErrors.put("artifactId", expectedErrorMessages);

        expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add("You must enter a version.");
        expectedFieldErrors.put("version", expectedErrorMessages);

        // repositoryId is not required.

        ValidatorUtil.assertFieldErrors(expectedFieldErrors, fieldErrors);
    }

    public void testStruts2ValidationFrameworkWithBlankInputs() throws Exception
    {
        // prep
        populateDeleteArtifactActionFields(deleteArtifactAction, EMPTY_STRING, EMPTY_STRING, EMPTY_STRING, EMPTY_STRING);

        // test
        actionValidatorManager.validate(deleteArtifactAction, EMPTY_STRING);

        // verify
        assertTrue(deleteArtifactAction.hasFieldErrors());

        Map<String, List<String>> fieldErrors = deleteArtifactAction.getFieldErrors();

        // make an expected field error object
        Map<String, List<String>> expectedFieldErrors = new HashMap<String, List<String>>();

        // populate
        List<String> expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add("You must enter a groupId.");
        expectedFieldErrors.put("groupId", expectedErrorMessages);

        expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add("You must enter an artifactId.");
        expectedFieldErrors.put("artifactId", expectedErrorMessages);

        expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add("You must enter a version.");
        expectedFieldErrors.put("version", expectedErrorMessages);

        // repositoryId is not required.

        ValidatorUtil.assertFieldErrors(expectedFieldErrors, fieldErrors);
    }

    public void testStruts2ValidationFrameworkWithInvalidInputs() throws Exception
    {
        // prep
        populateDeleteArtifactActionFields(deleteArtifactAction, GROUP_ID_INVALID_INPUT, ARTIFACT_ID_INVALID_INPUT, VERSION_INVALID_INPUT, REPOSITORY_ID_INVALID_INPUT);

        // test
        actionValidatorManager.validate(deleteArtifactAction, EMPTY_STRING);

        // verify
        assertTrue(deleteArtifactAction.hasFieldErrors());

        Map<String, List<String>> fieldErrors = deleteArtifactAction.getFieldErrors();

        // make an expected field error object
        Map<String, List<String>> expectedFieldErrors = new HashMap<String, List<String>>();

        // populate
        List<String> expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add("Group id must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-).");
        expectedFieldErrors.put("groupId", expectedErrorMessages);

        expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add("Artifact id must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-).");
        expectedFieldErrors.put("artifactId", expectedErrorMessages);

        expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add("Repository id must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-).");
        expectedFieldErrors.put("repositoryId", expectedErrorMessages);

        // version has its validation in the validate() method of the action class.

        ValidatorUtil.assertFieldErrors(expectedFieldErrors, fieldErrors);
    }

    public void testStruts2ValidationFrameworkWithValidInputs() throws Exception
    {
        // prep
        populateDeleteArtifactActionFields(deleteArtifactAction, GROUP_ID_VALID_INPUT, ARTIFACT_ID_VALID_INPUT, VERSION_VALID_INPUT, REPOSITORY_ID_VALID_INPUT);

        // test
        actionValidatorManager.validate(deleteArtifactAction, EMPTY_STRING);

        // verify
        assertFalse(deleteArtifactAction.hasFieldErrors());
    }

    private void populateDeleteArtifactActionFields(DeleteArtifactAction deleteArtifactAction, String groupId, String artifactId, String version, String repositoryId)
    {
        deleteArtifactAction.setGroupId(groupId);
        deleteArtifactAction.setArtifactId(artifactId);
        deleteArtifactAction.setVersion(version);
        deleteArtifactAction.setRepositoryId(repositoryId);
    }
}
