package org.apache.maven.archiva.cli;

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

import org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * ArtifactCountConsumer
 *
 * @version $Id$
 * plexus.component role="org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer"
 * role-hint="count-artifacts"
 * instantiation-strategy="per-lookup"
 */
@Service("knownRepositoryContentConsumer#count-artifacts")
@Scope("prototype")
public class ArtifactCountConsumer
    extends AbstractProgressConsumer
    implements KnownRepositoryContentConsumer
{
    /**
     * plexus.configuration default-value="count-artifacts"
     */
    private String id = "count-artifacts";

    /**
     * plexus.configuration default-value="Count Artifacts"
     */
    private String description = "Count Artifacts";

    private List<String> includes;

    public ArtifactCountConsumer()
    {
        // TODO: shouldn't this use filetypes?
        includes = new ArrayList<String>();
        includes.add( "**/*.pom" );
        includes.add( "**/*.jar" );
        includes.add( "**/*.war" );
        includes.add( "**/*.ear" );
        includes.add( "**/*.sar" );
        includes.add( "**/*.car" );
        includes.add( "**/*.mar" );
        includes.add( "**/*.dtd" );
        includes.add( "**/*.tld" );
        includes.add( "**/*.gz" );
        includes.add( "**/*.bz2" );
        includes.add( "**/*.zip" );
    }

    public String getDescription()
    {
        return description;
    }

    public String getId()
    {
        return id;
    }

    public boolean isPermanent()
    {
        return false;
    }

    public List<String> getExcludes()
    {
        return null;
    }

    public List<String> getIncludes()
    {
        return includes;
    }

}
