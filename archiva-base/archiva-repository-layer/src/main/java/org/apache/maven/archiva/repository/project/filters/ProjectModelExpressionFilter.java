package org.apache.maven.archiva.repository.project.filters;

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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.model.ArchivaModelCloner;
import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.model.Dependency;
import org.apache.maven.archiva.repository.project.ProjectModelException;
import org.apache.maven.archiva.repository.project.ProjectModelFilter;
import org.codehaus.plexus.evaluator.DefaultExpressionEvaluator;
import org.codehaus.plexus.evaluator.EvaluatorException;
import org.codehaus.plexus.evaluator.ExpressionEvaluator;
import org.codehaus.plexus.evaluator.sources.PropertiesExpressionSource;
import org.codehaus.plexus.evaluator.sources.SystemPropertyExpressionSource;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * ProjectModelExpressionFilter 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * @plexus.component role="org.apache.maven.archiva.repository.project.ProjectModelFilter"
 *                   role-hint="expression" 
 *                   instantiation-strategy="per-lookup"
 */
public class ProjectModelExpressionFilter
    implements ProjectModelFilter
{
    private ExpressionEvaluator evaluator = new DefaultExpressionEvaluator();

    /**
     * Find and Evaluate the Expressions present in the model.
     * 
     * @param model the model to correct.
     */
    public ArchivaProjectModel filter( final ArchivaProjectModel model )
        throws ProjectModelException
    {
        Properties props = new Properties();

        if ( model.getProperties() != null )
        {
            props.putAll( model.getProperties() );
        }

        props.setProperty( "pom.artifactId", model.getArtifactId() );

        props.setProperty( "pom.groupId", StringUtils.defaultString( model.getGroupId() ) );
        props.setProperty( "pom.version", StringUtils.defaultString( model.getVersion() ) );

        PropertiesExpressionSource propsSource = new PropertiesExpressionSource();
        propsSource.setProperties( props );
        evaluator.addExpressionSource( propsSource );
        evaluator.addExpressionSource( new SystemPropertyExpressionSource() );

        ArchivaProjectModel ret = ArchivaModelCloner.clone( model );

        try
        {
            ret.setVersion( evaluator.expand( ret.getVersion() ) );
            ret.setGroupId( evaluator.expand( ret.getGroupId() ) );

            if ( CollectionUtils.isNotEmpty( ret.getDependencies() ) )
            {
                evaluateExpressionsInDependencyList( evaluator, ret.getDependencies() );
            }

            evaluateExpressionsInDependencyList( evaluator, ret.getDependencyManagement() );
        }
        catch ( EvaluatorException e )
        {
            throw new ProjectModelException( "Unable to evaluate expression in model: " + e.getMessage(), e );
        }

        return ret;
    }

    private static void evaluateExpressionsInDependencyList( ExpressionEvaluator evaluator, List dependencies )
        throws EvaluatorException
    {
        if ( dependencies == null )
        {
            return;
        }

        Iterator it = dependencies.iterator();
        while ( it.hasNext() )
        {
            Dependency dependency = (Dependency) it.next();
            dependency.setGroupId( evaluator.expand( dependency.getGroupId() ) );
            dependency.setVersion( evaluator.expand( dependency.getVersion() ) );
        }
    }
}
