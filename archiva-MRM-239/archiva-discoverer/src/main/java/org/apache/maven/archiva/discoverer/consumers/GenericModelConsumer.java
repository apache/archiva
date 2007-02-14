package org.apache.maven.archiva.discoverer.consumers;

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

import org.apache.maven.archiva.discoverer.DiscovererConsumer;
import org.apache.maven.archiva.discoverer.DiscovererException;
import org.apache.maven.archiva.discoverer.PathUtil;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * GenericModelConsumer - consumer for pom files.
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public abstract class GenericModelConsumer
    extends AbstractDiscovererConsumer
    implements DiscovererConsumer
{
    public abstract void processModel( Model model, File file );

    private static final List includePatterns;

    static
    {
        includePatterns = new ArrayList();
        includePatterns.add( "**/*.pom" );
    }

    public GenericModelConsumer()
    {

    }

    public List getIncludePatterns()
    {
        return includePatterns;
    }

    public String getName()
    {
        return "MavenProject Consumer";
    }

    public boolean isEnabled()
    {
        return true;
    }

    public void processFile( File file )
        throws DiscovererException
    {
        String relpath = PathUtil.getRelative( repository.getBasedir(), file );
        Model model = buildModel( repository.getBasedir(), relpath );
        processModel( model, file );
    }

    private Model buildModel( String basedir, String modelpath )
        throws DiscovererException
    {
        Model model;
        File f = new File( basedir, modelpath );
        Reader reader = null;
        try
        {
            reader = new FileReader( f );
            MavenXpp3Reader modelReader = new MavenXpp3Reader();

            model = modelReader.read( reader );
        }
        catch ( XmlPullParserException e )
        {
            throw new DiscovererException( "Error parsing metadata file '" + f + "': " + e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new DiscovererException( "Error reading metadata file '" + f + "': " + e.getMessage(), e );
        }
        finally
        {
            IOUtil.close( reader );
        }

        return model;
    }
}
