package org.apache.maven.archiva.model;

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

import org.codehaus.plexus.spring.PlexusInSpringTestCase;

/**
 * ArchivaModelClonerTest
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class ArchivaModelClonerTest
    extends PlexusInSpringTestCase
{
    public void testCloneProjectModelWithParent()
    {
        ArchivaProjectModel actualModel = new ArchivaProjectModel();
        actualModel.setGroupId( null );
        actualModel.setArtifactId( "archiva-common" );
        actualModel.setVersion( null );
        actualModel.setParentProject( new VersionedReference() );
        actualModel.getParentProject().setGroupId( "org.apache.maven.archiva" );
        actualModel.getParentProject().setArtifactId( "archiva-parent" );
        actualModel.getParentProject().setVersion( "1.0" );

        ArchivaProjectModel clonedModel = ArchivaModelCloner.clone( actualModel );

        // Should not be the same object (in memory)
        assertNotSame( clonedModel, actualModel );

        // Should be equal in value.
        assertEquals( clonedModel, actualModel );

        // Test specific fields.
        assertNull( "Group Id", clonedModel.getGroupId() );
        assertNull( "Version", clonedModel.getVersion() );
        assertNotNull( "Parent Reference", clonedModel.getParentProject() );
        assertEquals( "Parent Group Id", "org.apache.maven.archiva", clonedModel.getParentProject().getGroupId() );
        assertEquals( "Parent Artifact Id", "archiva-parent", clonedModel.getParentProject().getArtifactId() );
        assertEquals( "Parent Version", "1.0", clonedModel.getParentProject().getVersion() );
    }
}
