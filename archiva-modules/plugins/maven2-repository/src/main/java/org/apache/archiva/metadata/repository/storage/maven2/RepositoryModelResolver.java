package org.apache.archiva.metadata.repository.storage.maven2;

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

import java.io.File;

import org.apache.archiva.metadata.repository.storage.RepositoryPathTranslator;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.resolution.InvalidRepositoryException;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;

public class RepositoryModelResolver
    implements ModelResolver
{
    private File basedir;

    private RepositoryPathTranslator pathTranslator;

    public RepositoryModelResolver( File basedir, RepositoryPathTranslator pathTranslator )
    {
        this.basedir = basedir;

        this.pathTranslator = pathTranslator;
    }

    public ModelSource resolveModel( String groupId, String artifactId, String version )
        throws UnresolvableModelException
    {
        String filename = artifactId + "-" + version + ".pom";
        // TODO: we need to convert 1.0-20091120.112233-1 type paths to baseVersion for the below call - add a test
        return new FileModelSource( pathTranslator.toFile( basedir, groupId, artifactId, version, filename ) );
    }

    public void addRepository( Repository repository )
        throws InvalidRepositoryException
    {
        // we just ignore repositories outside of the current one for now
        // TODO: it'd be nice to look them up from Archiva's set, but we want to do that by URL / mapping, not just the
        //       ID since they will rarely match
    }

    public ModelResolver newCopy()
    {
        return new RepositoryModelResolver( basedir, pathTranslator );
    }
}
