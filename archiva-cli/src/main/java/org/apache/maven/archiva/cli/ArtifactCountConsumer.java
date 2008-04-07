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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer;

/**
 * ArtifactCountConsumer 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer"
 *                   role-hint="count-artifacts"
 *                   instantiation-strategy="per-lookup"
 */
public class ArtifactCountConsumer
    extends AbstractProgressConsumer
    implements KnownRepositoryContentConsumer
{
    /**
     * @plexus.configuration default-value="count-artifacts"
     */
    private String id;

    /**
     * @plexus.configuration default-value="Count Artifacts"
     */
    private String description;

    private List includes;

    public ArtifactCountConsumer()
    {
        // TODO: shouldn't this use filetypes?
        includes = new ArrayList();
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

    public List getExcludes()
    {
        return null;
    }

    public List getIncludes()
    {
        return includes;
    }

}
