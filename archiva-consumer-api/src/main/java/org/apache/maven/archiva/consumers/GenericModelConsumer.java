package org.apache.maven.archiva.consumers;

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

import org.apache.maven.archiva.common.utils.BaseFile;
import org.apache.maven.archiva.repository.consumer.Consumer;
import org.apache.maven.archiva.repository.consumer.ConsumerException;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

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
    extends AbstractConsumer
    implements Consumer
{
    public abstract void processModel( Model model, BaseFile file );

    private static final List includePatterns;

    static
    {
        includePatterns = new ArrayList();
        includePatterns.add( "**/*.pom" );
    }

    public List getIncludePatterns()
    {
        return includePatterns;
    }

    public boolean isEnabled()
    {
        return true;
    }

    public void processFile( BaseFile file )
        throws ConsumerException
    {
        Model model = buildModel( file );
        processModel( model, file );
    }

    private Model buildModel( BaseFile file )
        throws ConsumerException
    {
        Model model;
        Reader reader = null;
        try
        {
            reader = new FileReader( file );
            MavenXpp3Reader modelReader = new MavenXpp3Reader();

            model = modelReader.read( reader );
        }
        catch ( XmlPullParserException e )
        {
            throw new ConsumerException( file, "Error parsing metadata file: " + e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new ConsumerException( file, "Error reading metadata file: " + e.getMessage(), e );
        }
        finally
        {
            IOUtil.close( reader );
        }

        return model;
    }
}
