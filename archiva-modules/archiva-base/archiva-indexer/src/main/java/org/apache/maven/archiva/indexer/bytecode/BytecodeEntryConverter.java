package org.apache.maven.archiva.indexer.bytecode;

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
import org.apache.maven.archiva.model.platform.JavaArtifactHelper;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Converter for Bytecode records and documents. 
 *
 * @version $Id$
 */
public class BytecodeEntryConverter implements LuceneEntryConverter
{

    public Document convert( LuceneRepositoryContentRecord record )
    {
        if ( !( record instanceof BytecodeRecord ) )
        {
            throw new ClassCastException( "Unable to convert type " + record.getClass().getName() + " to "
                            + BytecodeRecord.class.getName() + "." );
        }

        BytecodeRecord bytecode = (BytecodeRecord) record;

        LuceneDocumentMaker doc = new LuceneDocumentMaker( bytecode );

        // Artifact Reference
        doc.addFieldTokenized( ArtifactKeys.GROUPID, bytecode.getArtifact().getGroupId() );
        doc.addFieldExact( ArtifactKeys.GROUPID_EXACT, bytecode.getArtifact().getGroupId() );
        doc.addFieldTokenized( ArtifactKeys.ARTIFACTID, bytecode.getArtifact().getArtifactId() );
        doc.addFieldExact( ArtifactKeys.ARTIFACTID_EXACT, bytecode.getArtifact().getArtifactId() );
        doc.addFieldTokenized( ArtifactKeys.VERSION, bytecode.getArtifact().getVersion() );
        doc.addFieldExact( ArtifactKeys.VERSION_EXACT, bytecode.getArtifact().getVersion() );
        doc.addFieldTokenized( ArtifactKeys.TYPE, bytecode.getArtifact().getType() );
        doc.addFieldUntokenized( ArtifactKeys.CLASSIFIER, bytecode.getArtifact().getClassifier() );

        // Bytecode Specifics
        doc.addFieldExact( BytecodeKeys.JDK, JavaArtifactHelper.getJavaDetails( bytecode.getArtifact() ).getJdk() );
        doc.addFieldTokenized( BytecodeKeys.CLASSES, bytecode.getClasses() );
        doc.addFieldTokenized( BytecodeKeys.METHODS, bytecode.getMethods() );
        doc.addFieldTokenized( BytecodeKeys.FILES, bytecode.getFiles() );

        return doc.getDocument();
    }

    public LuceneRepositoryContentRecord convert( Document document ) throws ParseException
    {
        BytecodeRecord record = new BytecodeRecord();

        record.setRepositoryId( document.get( LuceneDocumentMaker.REPOSITORY_ID ) );
        
        // Artifact Reference
        String groupId = document.get( ArtifactKeys.GROUPID );
        String artifactId = document.get( ArtifactKeys.ARTIFACTID );
        String version = document.get( ArtifactKeys.VERSION );
        String classifier = document.get( ArtifactKeys.CLASSIFIER );
        String type = document.get( ArtifactKeys.TYPE );

        ArchivaArtifact artifact = new ArchivaArtifact( groupId, artifactId, version, classifier, type );
        record.setArtifact( artifact );

        // Bytecode Specifics
        JavaArtifactHelper.getJavaDetails( record.getArtifact() ).setJdk( document.get( BytecodeKeys.JDK ) );
        record.setClasses( getList( document, BytecodeKeys.CLASSES ) );
        record.setMethods( getList( document, BytecodeKeys.METHODS ) );
        record.setFiles( getList( document, BytecodeKeys.FILES ) );

        return record;
    }

    public List getList( Document document, String key )
    {
        String rawlist = document.get( key );

        if ( rawlist == null )
        {
            return null;
        }

        List ret = new ArrayList();
        ret.addAll( Arrays.asList( rawlist.split( "\n" ) ) );

        return ret;
    }
}
