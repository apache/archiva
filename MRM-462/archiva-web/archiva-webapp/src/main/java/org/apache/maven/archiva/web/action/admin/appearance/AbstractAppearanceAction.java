package org.apache.maven.archiva.web.action.admin.appearance;

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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.codehaus.plexus.xwork.action.PlexusActionSupport;

import java.io.File;
import java.util.Map;

/**
 * AbstractAppearanceAction 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public abstract class AbstractAppearanceAction
    extends PlexusActionSupport
{
    /**
     * @plexus.requirement role="org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout"
     */
    private Map repositoryLayouts;

    /**
     * @plexus.requirement
     */
    private ArtifactRepositoryFactory repoFactory;

    protected ArtifactRepository createLocalRepository()
    {
        String id = "archiva-local-repo";
        String layout = "default";
        String directory = System.getProperty( "user.home" ) + "/.m2/archiva";

        ArtifactRepositoryLayout repositoryLayout = (ArtifactRepositoryLayout) repositoryLayouts.get( layout );
        File repository = new File( directory );
        repository.mkdirs();

        String repoDir = repository.toURI().toString();
        //workaround for spaces non converted by PathUtils in wagon
        //TODO: remove it when PathUtils will be fixed
        if ( repoDir.indexOf( "%20" ) >= 0 )
        {
            repoDir = StringUtils.replace( repoDir, "%20", " " );
        }

        return repoFactory.createArtifactRepository( id, repoDir, repositoryLayout, null, null );
    }
}
