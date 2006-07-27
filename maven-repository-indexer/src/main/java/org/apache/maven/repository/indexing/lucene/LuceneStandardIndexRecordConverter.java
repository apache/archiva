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
import org.codehaus.plexus.util.StringUtils;

/**
 * Convert the standard index record to a Lucene document.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @todo use enum for field names
 */
public class LuceneStandardIndexRecordConverter
    implements LuceneIndexRecordConverter
{
    private static final String FLD_FILENAME = "filename";

    private static final String FLD_GROUPID = "groupId";

    private static final String FLD_ARTIFACTID = "artifactId";

    private static final String FLD_VERSION = "version";

    private static final String FLD_TYPE = "type";

    private static final String FLD_CLASSIFIER = "classifier";

    private static final String FLD_PACKAGING = "packaging";

    private static final String FLD_REPOSITORY = "repo";

    private static final String FLD_LAST_MODIFIED = "lastModified";

    private static final String FLD_FILE_SIZE = "fileSize";

    private static final String FLD_MD5 = "md5";

    private static final String FLD_SHA1 = "sha1";

    private static final String FLD_CLASSES = "classes";

    private static final String FLD_PLUGINPREFIX = "pluginPrefix";

    private static final String FLD_FILES = "files";

    private static final String FLD_INCEPTION_YEAR = "inceptionYear";

    private static final String FLD_PROJECT_NAME = "projectName";

    private static final String FLD_PROJECT_DESCRIPTION = "projectDesc";

    public Document convert( RepositoryIndexRecord record )
    {
        StandardArtifactIndexRecord standardIndexRecord = (StandardArtifactIndexRecord) record;

        Document document = new Document();
        addTokenizedField( document, FLD_FILENAME, standardIndexRecord.getFilename() );
        addTokenizedField( document, FLD_GROUPID, standardIndexRecord.getGroupId() );
        addTokenizedField( document, FLD_ARTIFACTID, standardIndexRecord.getArtifactId() );
        addTokenizedField( document, FLD_VERSION, standardIndexRecord.getVersion() );
        addUntokenizedField( document, FLD_TYPE, standardIndexRecord.getType() );
        addTokenizedField( document, FLD_CLASSIFIER, standardIndexRecord.getClassifier() );
        addUntokenizedField( document, FLD_PACKAGING, standardIndexRecord.getPackaging() );
        addTokenizedField( document, FLD_REPOSITORY, standardIndexRecord.getRepository() );
        addUntokenizedField( document, FLD_LAST_MODIFIED, DateTools.timeToString( standardIndexRecord.getLastModified(),
                                                                                  DateTools.Resolution.SECOND ) );
        addUntokenizedField( document, FLD_FILE_SIZE, NumberTools.longToString( standardIndexRecord.getSize() ) );
        addUntokenizedField( document, FLD_MD5, standardIndexRecord.getMd5Checksum() );
        addUntokenizedField( document, FLD_SHA1, standardIndexRecord.getSha1Checksum() );
        if ( standardIndexRecord.getClasses() != null )
        {
            addTokenizedField( document, FLD_CLASSES,
                               StringUtils.join( standardIndexRecord.getClasses().iterator(), "\n" ) );
        }
        if ( standardIndexRecord.getFiles() != null )
        {
            addTokenizedField( document, FLD_FILES,
                               StringUtils.join( standardIndexRecord.getFiles().iterator(), "\n" ) );
        }
        addTokenizedField( document, FLD_PLUGINPREFIX, standardIndexRecord.getPluginPrefix() );
        addUntokenizedField( document, FLD_INCEPTION_YEAR, standardIndexRecord.getInceptionYear() );
        addTokenizedField( document, FLD_PROJECT_NAME, standardIndexRecord.getProjectName() );
        addTokenizedField( document, FLD_PROJECT_DESCRIPTION, standardIndexRecord.getProjectDescription() );
/* TODO: add later
        document.add( Field.Keyword( FLD_LICENSE_URLS, "" ) );
        document.add( Field.Keyword( FLD_DEPENDENCIES, "" ) );
        document.add( Field.Keyword( FLD_PLUGINS_REPORT, "" ) );
        document.add( Field.Keyword( FLD_PLUGINS_BUILD, "" ) );
*/

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
