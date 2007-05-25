package org.apache.maven.archiva.indexer.hashcodes;

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
import org.apache.maven.archiva.indexer.ArtifactKeys;
import org.apache.maven.archiva.indexer.lucene.LuceneDocumentMaker;
import org.apache.maven.archiva.indexer.lucene.LuceneEntryConverter;
import org.apache.maven.archiva.indexer.lucene.LuceneRepositoryContentRecord;
import org.apache.maven.archiva.model.ArchivaArtifact;

import java.text.ParseException;

/**
 * Converter for Hashcode records and documents. 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class HashcodesEntryConverter implements LuceneEntryConverter
{

    public Document convert( LuceneRepositoryContentRecord record )
    {
        if ( !( record instanceof HashcodesRecord ) )
        {
            throw new ClassCastException( "Unable to convert type " + record.getClass().getName() + " to "
                            + HashcodesRecord.class.getName() + "." );
        }

        HashcodesRecord hashcodes = (HashcodesRecord) record;

        LuceneDocumentMaker doc = new LuceneDocumentMaker( hashcodes );
        
        // Artifact Reference
        doc.addFieldTokenized( ArtifactKeys.GROUPID, hashcodes.getArtifact().getGroupId() );
        doc.addFieldExact( ArtifactKeys.GROUPID_EXACT, hashcodes.getArtifact().getGroupId() );
        doc.addFieldTokenized( ArtifactKeys.ARTIFACTID, hashcodes.getArtifact().getArtifactId() );
        doc.addFieldExact( ArtifactKeys.ARTIFACTID_EXACT, hashcodes.getArtifact().getArtifactId() );
        doc.addFieldTokenized( ArtifactKeys.VERSION, hashcodes.getArtifact().getVersion() );
        doc.addFieldExact( ArtifactKeys.VERSION_EXACT, hashcodes.getArtifact().getVersion() );
        doc.addFieldTokenized( ArtifactKeys.TYPE, hashcodes.getArtifact().getType() );
        doc.addFieldUntokenized( ArtifactKeys.CLASSIFIER, hashcodes.getArtifact().getClassifier() );

        // Hashcode Specifics 
        doc.addFieldUntokenized( HashcodesKeys.MD5, hashcodes.getArtifact().getModel().getChecksumMD5() );
        doc.addFieldUntokenized( HashcodesKeys.SHA1, hashcodes.getArtifact().getModel().getChecksumSHA1() );

        return doc.getDocument();
    }

    public LuceneRepositoryContentRecord convert( Document document ) throws ParseException
    {
        HashcodesRecord record = new HashcodesRecord();
        
        record.setRepositoryId( document.get( LuceneDocumentMaker.REPOSITORY_ID ) );

        // Artifact Reference
        String groupId = document.get( ArtifactKeys.GROUPID );
        String artifactId = document.get( ArtifactKeys.ARTIFACTID );
        String version = document.get( ArtifactKeys.VERSION );
        String classifier = document.get( ArtifactKeys.CLASSIFIER );
        String type = document.get( ArtifactKeys.TYPE );

        ArchivaArtifact artifact = new ArchivaArtifact( groupId, artifactId, version, classifier, type );
        record.setArtifact( artifact );

        // Hashcode Specifics
        record.getArtifact().getModel().setChecksumMD5( document.get( HashcodesKeys.MD5 ) );
        record.getArtifact().getModel().setChecksumSHA1( document.get( HashcodesKeys.SHA1 ) );

        return record;
    }
}
