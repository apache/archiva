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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.opensymphony.xwork2.Validateable;
import com.opensymphony.xwork2.Preparable;
import org.apache.archiva.audit.Auditable;
import org.apache.archiva.stagerepository.merge.Maven2RepositoryMerger;
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;

import java.io.File;
import java.util.List;

/**
 * @plexus.component role="com.opensymphony.xwork2.Action" role-hint="mergeAction" instantiation-strategy="per-lookup"
 */
public class MergeAction
    extends PlexusActionSupport
    implements Validateable, Preparable, Auditable

{
    /**
     * @plexus.requirement role="org.apache.archiva.stagerepository.merge.RepositoryMerger" role-hint="maven2"
     */
    private Maven2RepositoryMerger repositoryMerger;

    /**
     * @plexus.requirement role-hint="default"
     */
    private ArchivaConfiguration configuration;

    private ManagedRepositoryConfiguration repository;

    private String repoid;

    private String targetRepoId;

    private final String action = "merge";

    List<ArtifactMetadata> conflictList;

    public String doMerge()
    {
        targetRepoId = repoid + "-stage";
        Configuration config = configuration.getConfiguration();
        ManagedRepositoryConfiguration targetRepoConfig = config.findManagedRepositoryById( targetRepoId );

        if ( targetRepoConfig != null )
        {

            try
            {
                repositoryMerger.merge( repoid, targetRepoId );
            }
            catch ( Exception e )
            {
                return ERROR;
            }
            return SUCCESS;
        }
        else
        {
            return ERROR;
        }
    }

    public String mergeWithOutConlficts()
    {

        targetRepoId = repoid + "-stage";

        try
        {
            conflictList = repositoryMerger.mergeWithOutConflictArtifacts( repoid, targetRepoId );
        }
        catch ( Exception e )
        {
            return ERROR;
        }

        return SUCCESS;
    }

    public void prepare()
        throws Exception
    {
        this.repository = new ManagedRepositoryConfiguration();
    }

    public ManagedRepositoryConfiguration getRepository()
    {
        return repository;
    }

    public void setRepository( ManagedRepositoryConfiguration repository )
    {
        this.repository = repository;
    }

    public String getRepoid()
    {
        return repoid;
    }

    public void setRepoid( String repoid )
    {
        this.repoid = repoid;
    }
}
