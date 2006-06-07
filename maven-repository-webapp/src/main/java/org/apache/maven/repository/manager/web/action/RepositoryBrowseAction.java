package org.apache.maven.repository.manager.web.action;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
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

import com.opensymphony.xwork.Action;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.repository.discovery.ArtifactDiscoverer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * TODO: Description.
 *
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="org.apache.maven.repository.manager.web.action.RepositoryBrowseAction"
 */
public class RepositoryBrowseAction
    implements Action
{
    /**
     * @plexus.requirement role-hint="org.apache.maven.repository.discovery.DefaultArtifactDiscoverer"
     */
    private ArtifactDiscoverer discoverer;

    /**
     * @plexus.requirement
     */
    private ArtifactRepositoryFactory repositoryFactory;

    /**
     * @plexus.requirement role-hint="default"
     */
    private ArtifactRepositoryLayout layout;

    private String group;

    private Map artifactMap;

    private String folder;

    private int idx;

    public String execute()
    {
        // TODO! fix hardcoded path
        String path = "E:/jeprox/maven-repository-manager/trunk/maven-repository-discovery/src/test/repository";

        ArtifactRepository repository =
            repositoryFactory.createArtifactRepository( "discoveryRepo", "file://" + path, layout, null, null );

        List artifacts = discoverer.discoverArtifacts( repository, null, true );

        Iterator iterator = artifacts.iterator();

        artifactMap = new TreeMap();

        while ( iterator.hasNext() )
        {
            Artifact artifact = (Artifact) iterator.next();

            String groupId = artifact.getGroupId();

            String key = groupId.replace( '.', '/' ) + "/" + artifact.getArtifactId() + "/" + artifact.getVersion();

            List artifactList;

            if ( artifactMap.containsKey( key ) )
            {
                artifactList = (List) artifactMap.get( key );
            }
            else
            {
                artifactList = new ArrayList();
            }

            artifactList.add( artifact );

            Collections.sort( artifactList );

            artifactMap.put( key, artifactList );
        }

        //set the index for folder level to be displayed
        idx = 1;

        folder = "";

        return SUCCESS;
    }

    // TODO! is this method needed?
    public String doEdit()
    {
        idx = idx + 1;

        //set folder to "" if we are at the root directory
        if ( idx == 1 )
        {
            folder = "";
        }

        return SUCCESS;
    }

    public Map getArtifactMap()
    {
        return artifactMap;
    }

    public String getGroup()
    {
        return group;
    }

    public void setGroup( String group )
    {
        this.group = group;
    }

    public String getFolder()
    {
        return folder;
    }

    public void setFolder( String folder )
    {
        this.folder = folder;
    }

    public int getIdx()
    {
        return idx;
    }

    public void setIdx( int index )
    {
        this.idx = index;
    }

}
