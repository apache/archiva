package org.apache.maven.repository.reporting;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0

 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.codehaus.plexus.PlexusTestCase;

public class BadMetadataReportProcessorTest extends PlexusTestCase
{
    protected ArtifactFactory artifactFactory;
    private BadMetadataReportProcessor badMetadataReportProcessor;

    public BadMetadataReportProcessorTest(String testName)
    {
        super(testName);
    }

    protected void setUp() throws Exception
    {
        artifactFactory = (ArtifactFactory) getContainer().lookup( ArtifactFactory.ROLE );
        
        badMetadataReportProcessor = new TestBadMetadataReportProcessor( artifactFactory, 
                                                                         new DefaultRepositoryQueryLayer() );
    }
    
    protected RepositoryQueryLayer getRepositoryQueryLayer( List returnValues ) throws NoSuchMethodException
    {
        GenericMockObject mockObject = new GenericMockObject();
        Method method = RepositoryQueryLayer.class.getMethod( "containsArtifact", null );
        mockObject.setExpectedReturns( method, returnValues );
        RepositoryQueryLayer queryLayer = (RepositoryQueryLayer) Proxy.newProxyInstance( this.getClassLoader(), 
                                                                    new Class[] { RepositoryQueryLayer.class },
                                                                    new GenericMockObject() );
        return queryLayer;
    }

    protected void tearDown() throws Exception
    {
        release( artifactFactory );
    }

    public void testProcessMetadata()
    {
    }

    public void testCheckPluginMetadata()
    {
    }

    public void testCheckSnapshotMetadata()
    {
    }

    public void testCheckMetadataVersions()
    {
    }

    public void testCheckRepositoryVersions()
    {
    }
    
}
