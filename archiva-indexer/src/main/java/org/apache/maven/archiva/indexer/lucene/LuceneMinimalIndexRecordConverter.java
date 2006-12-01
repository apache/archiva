package org.apache.maven.archiva.indexer.lucene;

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
import org.apache.maven.archiva.indexer.record.MinimalArtifactIndexRecord;
import org.apache.maven.archiva.indexer.record.MinimalIndexRecordFields;
import org.apache.maven.archiva.indexer.record.RepositoryIndexRecord;
import org.apache.commons.lang.StringUtils;

import java.text.ParseException;
import java.util.Arrays;

/**
 * Convert the minimal index record to a Lucene document.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class LuceneMinimalIndexRecordConverter
    implements LuceneIndexRecordConverter
{
    public Document convert( RepositoryIndexRecord record )
    {
        MinimalArtifactIndexRecord rec = (MinimalArtifactIndexRecord) record;

        Document document = new Document();
        addTokenizedField( document, MinimalIndexRecordFields.FILENAME, rec.getFilename() );
        addUntokenizedField( document, MinimalIndexRecordFields.LAST_MODIFIED,
                             DateTools.timeToString( rec.getLastModified(), DateTools.Resolution.SECOND ) );
        addUntokenizedField( document, MinimalIndexRecordFields.FILE_SIZE, NumberTools.longToString( rec.getSize() ) );
        addUntokenizedField( document, MinimalIndexRecordFields.MD5, rec.getMd5Checksum() );
        addTokenizedField( document, MinimalIndexRecordFields.CLASSES,
                           StringUtils.join( rec.getClasses().iterator(), "\n" ) );

        return document;
    }

    public RepositoryIndexRecord convert( Document document )
        throws ParseException
    {
        MinimalArtifactIndexRecord record = new MinimalArtifactIndexRecord();

        record.setFilename( document.get( MinimalIndexRecordFields.FILENAME ) );
        record.setLastModified( DateTools.stringToTime( document.get( MinimalIndexRecordFields.LAST_MODIFIED ) ) );
        record.setSize( NumberTools.stringToLong( document.get( MinimalIndexRecordFields.FILE_SIZE ) ) );
        record.setMd5Checksum( document.get( MinimalIndexRecordFields.MD5 ) );
        record.setClasses( Arrays.asList( document.get( MinimalIndexRecordFields.CLASSES ).split( "\n" ) ) );

        return record;
    }

    private static void addUntokenizedField( Document document, String name, String value )
    {
        if ( value != null )
        {
            document.add( new Field( name, value, Field.Store.YES, Field.Index.UN_TOKENIZED ) );
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
