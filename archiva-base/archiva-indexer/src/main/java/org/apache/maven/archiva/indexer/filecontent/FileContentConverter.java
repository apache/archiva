package org.apache.maven.archiva.indexer.filecontent;

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

import org.apache.lucene.document.Document;
import org.apache.maven.archiva.indexer.lucene.LuceneDocumentMaker;
import org.apache.maven.archiva.indexer.lucene.LuceneEntryConverter;
import org.apache.maven.archiva.indexer.lucene.LuceneRepositoryContentRecord;

import java.text.ParseException;

/**
 * FileContentConverter 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class FileContentConverter
    implements LuceneEntryConverter
{

    public Document convert( LuceneRepositoryContentRecord record )
    {
        if ( !( record instanceof FileContentRecord ) )
        {
            throw new ClassCastException( "Unable to convert type " + record.getClass().getName() + " to "
                + FileContentRecord.class.getName() + "." );
        }

        FileContentRecord filecontent = (FileContentRecord) record;

        LuceneDocumentMaker doc = new LuceneDocumentMaker( filecontent );

        doc.addFieldTokenized( FileContentKeys.FILENAME, filecontent.getFilename() );
        doc.addFieldTokenized( FileContentKeys.CONTENT, filecontent.getContents() );

        return doc.getDocument();
    }

    public LuceneRepositoryContentRecord convert( Document document )
        throws ParseException
    {
        FileContentRecord record = new FileContentRecord();

        record.setRepositoryId( document.get( LuceneDocumentMaker.REPOSITORY_ID ) );
        record.setFilename( document.get( FileContentKeys.FILENAME ) );
        record.setContents( document.get( FileContentKeys.CONTENT ) );

        return record;
    }

}
