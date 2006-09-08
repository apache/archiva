package org.apache.maven.archiva.reporting;

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

import org.apache.maven.archiva.reporting.model.io.xpp3.ReportingXpp3Reader;
import org.apache.maven.archiva.reporting.model.io.xpp3.ReportingXpp3Writer;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Load and store the reports. No synchronization is used, but it is unnecessary as the old object
 * can continue to be used.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @todo would be great for plexus to do this for us - so the configuration would be a component itself rather than this store
 * @todo support other implementations than XML file
 * @plexus.component
 */
public class DefaultReportingStore
    extends AbstractLogEnabled
    implements ReportingStore
{
    /**
     * The cached reports for given repositories.
     */
    private Map/*<ArtifactRepository,ReportingDatabase>*/ reports = new HashMap();

    public ReportingDatabase getReportsFromStore( ArtifactRepository repository )
        throws ReportingStoreException
    {
        ReportingDatabase database = (ReportingDatabase) reports.get( repository );

        if ( database == null )
        {
            ReportingXpp3Reader reader = new ReportingXpp3Reader();

            File file = new File( repository.getBasedir(), "report-database.xml" );

            FileReader fileReader = null;
            try
            {
                fileReader = new FileReader( file );
            }
            catch ( FileNotFoundException e )
            {
                database = new ReportingDatabase( repository );
            }

            if ( database == null )
            {
                getLogger().info( "Reading report database from " + file );
                try
                {
                    database = new ReportingDatabase( reader.read( fileReader, false ), repository );
                }
                catch ( IOException e )
                {
                    throw new ReportingStoreException( e.getMessage(), e );
                }
                catch ( XmlPullParserException e )
                {
                    throw new ReportingStoreException( e.getMessage(), e );
                }
                finally
                {
                    IOUtil.close( fileReader );
                }
            }

            reports.put( repository, database );
        }
        return database;
    }

    public void storeReports( ReportingDatabase database, ArtifactRepository repository )
        throws ReportingStoreException
    {
        ReportingXpp3Writer writer = new ReportingXpp3Writer();

        File file = new File( repository.getBasedir(), "report-database.xml" );
        getLogger().info( "Writing reports to " + file );
        FileWriter fileWriter = null;
        try
        {
            file.getParentFile().mkdirs();

            fileWriter = new FileWriter( file );
            writer.write( fileWriter, database.getReporting() );
        }
        catch ( IOException e )
        {
            throw new ReportingStoreException( e.getMessage(), e );
        }
        finally
        {
            IOUtil.close( fileWriter );
        }
    }
}
