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
import org.apache.maven.repository.indexing.record.MinimalArtifactIndexRecord;
import org.apache.maven.repository.indexing.record.RepositoryIndexRecord;

/**
 * Convert the minimal index record to a Lucene document.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class LuceneMinimalIndexRecordConverter
    implements LuceneIndexRecordConverter
{
    private static final String FLD_FILENAME = "j";

    private static final String FLD_LAST_MODIFIED = "d";

    private static final String FLD_FILE_SIZE = "s";

    private static final String FLD_MD5 = "m";

    private static final String FLD_CLASSES = "c";

    public Document convert( RepositoryIndexRecord record )
    {
        MinimalArtifactIndexRecord standardIndexRecord = (MinimalArtifactIndexRecord) record;

        Document document = new Document();
        addTokenizedField( document, FLD_FILENAME, standardIndexRecord.getFilename() );
        addUntokenizedField( document, FLD_LAST_MODIFIED, DateTools.timeToString( standardIndexRecord.getLastModified(),
                                                                                  DateTools.Resolution.SECOND ) );
        addUntokenizedField( document, FLD_FILE_SIZE, NumberTools.longToString( standardIndexRecord.getSize() ) );
        addUntokenizedField( document, FLD_MD5, standardIndexRecord.getMd5Checksum() );
        addTokenizedField( document, FLD_CLASSES, standardIndexRecord.getClasses() );

        return document;
    }

    private static void addUntokenizedField( Document document, String name, String value )
    {
        if ( value != null )
        {
            document.add( new Field( name, value, Field.Store.YES, Field.Index.TOKENIZED ) );
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
