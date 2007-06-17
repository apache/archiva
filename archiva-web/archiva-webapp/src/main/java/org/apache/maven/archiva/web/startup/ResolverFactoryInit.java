package org.apache.maven.archiva.web.startup;

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

import org.apache.maven.archiva.database.project.ProjectModelToDatabaseListener;
import org.apache.maven.archiva.repository.project.ProjectModelResolver;
import org.apache.maven.archiva.repository.project.ProjectModelResolverFactory;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;

/**
 * ResolverFactoryInit - Initialize the Resolver Factory, and hook it up to
 * the database.
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component 
 *              role="org.apache.maven.archiva.web.startup.ResolverFactoryInit"
 *              role-hint="default"
 */
public class ResolverFactoryInit
    extends AbstractLogEnabled
    implements Initializable
{
    /**
     * @plexus.requirement role-hint="database"
     */
    private ProjectModelResolver databaseResolver;
    
    /**
     * @plexus.requirement 
     *          role="org.apache.maven.archiva.repository.project.resolvers.ProjectModelResolutionListener"
     *          role-hint="model-to-db"
     */
    private ProjectModelToDatabaseListener modelToDbListener;

    /**
     * The resolver factorying being initialized.
     * 
     * @plexus.requirement
     */
    private ProjectModelResolverFactory resolverFactory;
    
    public void initialize()
        throws InitializationException
    {
        if ( !resolverFactory.getCurrentResolverStack().hasResolver( databaseResolver ) )
        {
            resolverFactory.getCurrentResolverStack().prependProjectModelResolver( databaseResolver );
        }
        resolverFactory.getCurrentResolverStack().addListener( modelToDbListener );
    }
}
