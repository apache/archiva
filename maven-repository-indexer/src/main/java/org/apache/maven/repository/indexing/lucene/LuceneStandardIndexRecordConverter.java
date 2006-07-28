package org.apache.maven.repository.indexing.lucene;

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

import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumberTools;
import org.apache.maven.repository.indexing.record.RepositoryIndexRecord;
import org.apache.maven.repository.indexing.record.StandardArtifactIndexRecord;
import org.apache.maven.repository.indexing.record.StandardIndexRecordFields;
import org.codehaus.plexus.util.StringUtils;

import java.text.ParseException;
import java.util.Arrays;

/**
 * Convert the standard index record to a Lucene document.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class LuceneStandardIndexRecordConverter
    implements LuceneIndexRecordConverter
{
    public Document convert( RepositoryIndexRecord record )
    {
        StandardArtifactIndexRecord rec = (StandardArtifactIndexRecord) record;

        Document document = new Document();
        addTokenizedField( document, StandardIndexRecordFields.FILENAME, rec.getFilename() );
        addTokenizedField( document, StandardIndexRecordFields.GROUPID, rec.getGroupId() );
        addExactField( document, StandardIndexRecordFields.GROUPID_EXACT, rec.getGroupId() );
        addTokenizedField( document, StandardIndexRecordFields.ARTIFACTID, rec.getArtifactId() );
        addExactField( document, StandardIndexRecordFields.ARTIFACTID_EXACT, rec.getArtifactId() );
        addTokenizedField( document, StandardIndexRecordFields.VERSION, rec.getVersion() );
        addExactField( document, StandardIndexRecordFields.VERSION_EXACT, rec.getVersion() );
        addTokenizedField( document, StandardIndexRecordFields.BASE_VERSION, rec.getBaseVersion() );
        addExactField( document, StandardIndexRecordFields.BASE_VERSION_EXACT, rec.getBaseVersion() );
        addUntokenizedField( document, StandardIndexRecordFields.TYPE, rec.getType() );
        addTokenizedField( document, StandardIndexRecordFields.CLASSIFIER, rec.getClassifier() );
        addUntokenizedField( document, StandardIndexRecordFields.PACKAGING, rec.getPackaging() );
        addUntokenizedField( document, StandardIndexRecordFields.REPOSITORY, rec.getRepository() );
        addUntokenizedField( document, StandardIndexRecordFields.LAST_MODIFIED,
                             DateTools.timeToString( rec.getLastModified(), DateTools.Resolution.SECOND ) );
        addUntokenizedField( document, StandardIndexRecordFields.FILE_SIZE, NumberTools.longToString( rec.getSize() ) );
        addUntokenizedField( document, StandardIndexRecordFields.MD5, rec.getMd5Checksum() );
        addUntokenizedField( document, StandardIndexRecordFields.SHA1, rec.getSha1Checksum() );
        if ( rec.getClasses() != null )
        {
            addTokenizedField( document, StandardIndexRecordFields.CLASSES,
                               StringUtils.join( rec.getClasses().iterator(), "\n" ) );
        }
        if ( rec.getFiles() != null )
        {
            addTokenizedField( document, StandardIndexRecordFields.FILES,
                               StringUtils.join( rec.getFiles().iterator(), "\n" ) );
        }
        addUntokenizedField( document, StandardIndexRecordFields.PLUGIN_PREFIX, rec.getPluginPrefix() );
        addUntokenizedField( document, StandardIndexRecordFields.INCEPTION_YEAR, rec.getInceptionYear() );
        addTokenizedField( document, StandardIndexRecordFields.PROJECT_NAME, rec.getProjectName() );
        addTokenizedField( document, StandardIndexRecordFields.PROJECT_DESCRIPTION, rec.getProjectDescription() );
/* TODO: add later
        document.add( Field.Keyword( StandardIndexRecordFields.FLD_LICENSE_URLS, "" ) );
        document.add( Field.Keyword( StandardIndexRecordFields.FLD_DEPENDENCIES, "" ) );
        document.add( Field.Keyword( StandardIndexRecordFields.FLD_PLUGINS_REPORT, "" ) );
        document.add( Field.Keyword( StandardIndexRecordFields.FLD_PLUGINS_BUILD, "" ) );
*/

        return document;
    }

    public RepositoryIndexRecord convert( Document document )
        throws ParseException
    {
        StandardArtifactIndexRecord record = new StandardArtifactIndexRecord();

        record.setFilename( document.get( StandardIndexRecordFields.FILENAME ) );
        record.setGroupId( document.get( StandardIndexRecordFields.GROUPID ) );
        record.setArtifactId( document.get( StandardIndexRecordFields.ARTIFACTID ) );
        record.setVersion( document.get( StandardIndexRecordFields.VERSION ) );
        record.setBaseVersion( document.get( StandardIndexRecordFields.BASE_VERSION ) );
        record.setType( document.get( StandardIndexRecordFields.TYPE ) );
        record.setClassifier( document.get( StandardIndexRecordFields.CLASSIFIER ) );
        record.setPackaging( document.get( StandardIndexRecordFields.PACKAGING ) );
        record.setRepository( document.get( StandardIndexRecordFields.REPOSITORY ) );
        record.setLastModified( DateTools.stringToTime( document.get( StandardIndexRecordFields.LAST_MODIFIED ) ) );
        record.setSize( NumberTools.stringToLong( document.get( StandardIndexRecordFields.FILE_SIZE ) ) );
        record.setMd5Checksum( document.get( StandardIndexRecordFields.MD5 ) );
        record.setSha1Checksum( document.get( StandardIndexRecordFields.SHA1 ) );
        String classes = document.get( StandardIndexRecordFields.CLASSES );
        if ( classes != null )
        {
            record.setClasses( Arrays.asList( classes.split( "\n" ) ) );
        }
        String files = document.get( StandardIndexRecordFields.FILES );
        if ( files != null )
        {
            record.setFiles( Arrays.asList( files.split( "\n" ) ) );
        }
        record.setPluginPrefix( document.get( StandardIndexRecordFields.PLUGIN_PREFIX ) );
        record.setInceptionYear( document.get( StandardIndexRecordFields.INCEPTION_YEAR ) );
        record.setProjectName( document.get( StandardIndexRecordFields.PROJECT_NAME ) );
        record.setProjectDescription( document.get( StandardIndexRecordFields.PROJECT_DESCRIPTION ) );

        return record;
    }

    private static void addUntokenizedField( Document document, String name, String value )
    {
        if ( value != null )
        {
            document.add( new Field( name, value, Field.Store.YES, Field.Index.UN_TOKENIZED ) );
        }
    }

    private static void addExactField( Document document, String name, String value )
    {
        if ( value != null )
        {
            document.add( new Field( name, value, Field.Store.NO, Field.Index.UN_TOKENIZED ) );
        }
    }

    private static void addTokenizedField( Document document, String name, String value )
    {
        if ( value != null )
        {
            document.add( new Field( name, value, Field.Store.YES, Field.Index.TOKENIZED ) );
        }
    }
}
