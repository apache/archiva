//package org.apache.maven.archiva.consumers.lucene;
//
///*
// * Licensed to the Apache Software Foundation (ASF) under one
// * or more contributor license agreements.  See the NOTICE file
// * distributed with this work for additional information
// * regarding copyright ownership.  The ASF licenses this file
// * to you under the Apache License, Version 2.0 (the
// * "License"); you may not use this file except in compliance
// * with the License.  You may obtain a copy of the License at
// *
// *  http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing,
// * software distributed under the License is distributed on an
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// * KIND, either express or implied.  See the License for the
// * specific language governing permissions and limitations
// * under the License.
// */
//
//import org.apache.maven.archiva.database.updater.DatabaseCleanupConsumer;
//import org.apache.maven.archiva.model.ArchivaArtifact;
//import org.apache.maven.archiva.model.ArchivaArtifactModel;
//import org.codehaus.plexus.spring.PlexusInSpringTestCase;
//
///**
// * LuceneCleanupRemoveIndexedConsumerTest
// *
// * @version
// */
//public class LuceneCleanupRemoveIndexedConsumerTest
//    extends PlexusInSpringTestCase
//{
//    private DatabaseCleanupConsumer luceneCleanupRemoveIndexConsumer;
//
//    public void setUp()
//        throws Exception
//    {
//        super.setUp();
//
//        luceneCleanupRemoveIndexConsumer = (DatabaseCleanupConsumer)
//            lookup( DatabaseCleanupConsumer.class, "lucene-cleanup" );
//    }
//
//    public void testIfArtifactExists()
//        throws Exception
//    {
//        ArchivaArtifact artifact = createArtifact(
//              "org.apache.maven.archiva", "archiva-lucene-cleanup", "1.0", "jar" );
//
//        luceneCleanupRemoveIndexConsumer.processArchivaArtifact( artifact );
//    }
//
//    public void testIfArtifactDoesNotExist()
//        throws Exception
//    {
//        ArchivaArtifact artifact = createArtifact(
//              "org.apache.maven.archiva", "deleted-artifact", "1.0", "jar" );
//
//        luceneCleanupRemoveIndexConsumer.processArchivaArtifact( artifact );
//    }
//
//    private ArchivaArtifact createArtifact( String groupId, String artifactId, String version, String type )
//    {
//        ArchivaArtifactModel model = new ArchivaArtifactModel();
//        model.setGroupId( groupId );
//        model.setArtifactId( artifactId );
//        model.setVersion( version );
//        model.setType( type );
//        model.setRepositoryId( "test-repo" );
//
//        return new ArchivaArtifact( model );
//    }
//
//}
