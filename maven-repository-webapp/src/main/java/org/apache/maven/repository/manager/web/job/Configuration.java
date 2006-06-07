package org.apache.maven.repository.manager.web.job;

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

import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.repository.layout.LegacyRepositoryLayout;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;

import java.util.Properties;

/**
 * This class contains the configuration values to be used by the scheduler
 *
 * @todo should not need to be initializable [!] Should have individual configuration items, and they could well be configured on the job itself, not in this class
 */
public class Configuration
    implements Initializable
{

    private Properties props;

    /**
     * @throws InitializationException
     */
    public void initialize()
        throws InitializationException
    {
    }

    /**
     * Set the properties object
     *
     * @param properties
     */
    public void setProperties( Properties properties )
    {
        this.props = properties;
    }

    /**
     * Returns the properties object
     *
     * @return a Properties object that contains the configuration values
     */
    public Properties getProperties()
    {
        return props;
    }

    public ArtifactRepositoryLayout getLayout()
    {
        // TODO: lookup from map [!]
        ArtifactRepositoryLayout layout;
        if ( "legacy".equals( props.getProperty( "layout" ) ) )
        {
            layout = new LegacyRepositoryLayout();
        }
        else
        {
            layout = new DefaultRepositoryLayout();
        }
        return layout;
    }

    public String getIndexDirectory()
    {
        return props.getProperty( "index.path" );
    }

    public String getRepositoryDirectory()
    {
        String repositoryDir = "";
        if ( "default".equals( props.getProperty( "layout" ) ) )
        {
            repositoryDir = props.getProperty( "default.repository.dir" );
        }
        else if ( "legacy".equals( props.getProperty( "layout" ) ) )
        {
            repositoryDir = props.getProperty( "legacy.repository.dir" );
        }
        return repositoryDir;
    }
}
