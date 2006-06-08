package org.apache.maven.repository.indexing;

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

import org.apache.lucene.analysis.Analyzer;
import org.apache.maven.artifact.repository.ArtifactRepository;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * @author Edwin Punzalan
 */
public interface RepositoryIndex
{
    String POM = "POM";

    String METADATA = "METADATA";

    String ARTIFACT = "ARTIFACT";

    String FLD_ID = "id";

    String FLD_NAME = "name";

    String FLD_DOCTYPE = "doctype";

    String FLD_GROUPID = "groupId";

    String FLD_ARTIFACTID = "artifactId";

    String FLD_VERSION = "version";

    String FLD_PACKAGING = "packaging";

    String FLD_SHA1 = "sha1";

    String FLD_MD5 = "md5";

    String FLD_LASTUPDATE = "last update";

    String FLD_PLUGINPREFIX = "plugin prefix";

    String FLD_CLASSES = "class";

    String FLD_PACKAGES = "package";

    String FLD_FILES = "file";

    String FLD_LICENSE_URLS = "license url";

    String FLD_DEPENDENCIES = "dependency";

    String FLD_PLUGINS_BUILD = "build plugin";

    String FLD_PLUGINS_REPORT = "report plugin";

    String FLD_PLUGINS_ALL = "plugins_all";

    String[] FIELDS = {FLD_ID, FLD_NAME, FLD_DOCTYPE, FLD_GROUPID, FLD_ARTIFACTID, FLD_VERSION, FLD_PACKAGING, FLD_SHA1,
        FLD_MD5, FLD_LASTUPDATE, FLD_PLUGINPREFIX, FLD_CLASSES, FLD_PACKAGES, FLD_FILES, FLD_LICENSE_URLS,
        FLD_DEPENDENCIES, FLD_PLUGINS_BUILD, FLD_PLUGINS_REPORT, FLD_PLUGINS_ALL};

    List KEYWORD_FIELDS = Arrays.asList( new String[]{FLD_ID, FLD_PACKAGING, FLD_LICENSE_URLS, FLD_DEPENDENCIES,
        FLD_PLUGINS_BUILD, FLD_PLUGINS_REPORT, FLD_PLUGINS_ALL} );

    String[] MODEL_FIELDS = {FLD_PACKAGING, FLD_LICENSE_URLS, FLD_DEPENDENCIES, FLD_PLUGINS_BUILD, FLD_PLUGINS_REPORT};

    /**
     * Method used to query the index status
     *
     * @return true if the index is open.
     */
    boolean isOpen();

    /**
     * Method to close open streams to the index directory
     */
    void close()
        throws RepositoryIndexException;

    ArtifactRepository getRepository();

    /**
     * Method to encapsulate the optimize() method for lucene
     */
    void optimize()
        throws RepositoryIndexException;

    /**
     * Method to retrieve the lucene analyzer object used in creating the document fields for this index
     *
     * @return lucene Analyzer object used in creating the index fields
     */
    Analyzer getAnalyzer();

    /**
     * Method to retrieve the path where the index is made available
     *
     * @return the path where the index resides
     */
    File getIndexPath();

    /**
     * Tests an index field if it is a keyword field
     *
     * @param field the name of the index field to test
     * @return true if the index field passed is a keyword, otherwise its false
     */
    boolean isKeywordField( String field );
}
