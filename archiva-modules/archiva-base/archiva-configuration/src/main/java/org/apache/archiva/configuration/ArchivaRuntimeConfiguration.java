package org.apache.archiva.configuration;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 
 *         The runtime configuration.
 *       
 * 
 * @version $Revision$ $Date$
 */
@SuppressWarnings( "all" )
public class ArchivaRuntimeConfiguration
    implements java.io.Serializable
{

      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * the url failure cache configuration.
     */
    private CacheConfiguration urlFailureCacheConfiguration;

    /**
     * the FileLocking configuration.
     */
    private FileLockConfiguration fileLockConfiguration;

    /**
     * The base directory where the archiva data is stored. If not
     * set, the appserver.base is used.
     */
    private String dataDirectory;

    /**
     * The base directory for local storage of repository data. If
     * not set, it's ${dataDirectory}/repositories.
     */
    private String repositoryBaseDirectory;

    /**
     * The base directory for local storage of remote repository
     * data. If not set, it's ${dataDirectory}/remotes.
     */
    private String remoteRepositoryBaseDirectory;

    /**
     * The base directory for local storage of repository group files.
     * If not set, it's ${dataDirectory}/groups
     */
    private String repositoryGroupBaseDirectory;

    /**
     * The default language used for setting internationalized
     * strings.
     */
    private String defaultLanguage = "en-US";

    /**
     * Comma separated list of language patterns. Sorted by
     * priority descending. Used for display of internationalized
     * strings.
     */
    private String languageRange = "en,fr,de";

    /**
     * List of checksum types (algorithms) that should be applied to repository artifacts.
     */
    private List<String> checksumTypes = new ArrayList(Arrays.asList("MD5","SHA1","SHA256"));


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * Get the base directory where the archiva data is stored. If
     * not set, the appserver.base is used.
     * 
     * @return String
     */
    public String getDataDirectory()
    {
        return this.dataDirectory;
    } //-- String getDataDirectory()

    /**
     * Get the default language used for setting internationalized
     * strings.
     * 
     * @return String
     */
    public String getDefaultLanguage()
    {
        return this.defaultLanguage;
    } //-- String getDefaultLanguage()

    /**
     * Get the FileLocking configuration.
     * 
     * @return FileLockConfiguration
     */
    public FileLockConfiguration getFileLockConfiguration()
    {
        return this.fileLockConfiguration;
    } //-- FileLockConfiguration getFileLockConfiguration()

    /**
     * Get comma separated list of language patterns. Sorted by
     * priority descending. Used for display of internationalized
     * strings.
     * 
     * @return String
     */
    public String getLanguageRange()
    {
        return this.languageRange;
    } //-- String getLanguageRange()

    /**
     * Get the base directory for local storage of remote
     * repository data. If not set, it's ${dataDirectory}/remotes.
     * 
     * @return String
     */
    public String getRemoteRepositoryBaseDirectory()
    {
        return this.remoteRepositoryBaseDirectory;
    } //-- String getRemoteRepositoryBaseDirectory()

    /**
     * Get the base directory for local storage of repository data.
     * If not set, it's ${dataDirectory}/repositories.
     * 
     * @return String
     */
    public String getRepositoryBaseDirectory()
    {
        return this.repositoryBaseDirectory;
    } //-- String getRepositoryBaseDirectory()

    /**
     * Get the base directory for local storage of repository group data.
     * If not set it's ${dataDirectory}/groups
     *
     * @return The path to the directory. Either a absolute path, or a path
     * relative to ${dataDirectory}
     */
    public String getRepositoryGroupBaseDirectory() {
        return this.repositoryGroupBaseDirectory;
    }

    /**
     * Get the url failure cache configuration.
     * 
     * @return CacheConfiguration
     */
    public CacheConfiguration getUrlFailureCacheConfiguration()
    {
        return this.urlFailureCacheConfiguration;
    } //-- CacheConfiguration getUrlFailureCacheConfiguration()

    /**
     * Set the base directory where the archiva data is stored. If
     * not set, the appserver.base is used.
     * 
     * @param dataDirectory
     */
    public void setDataDirectory( String dataDirectory )
    {
        this.dataDirectory = dataDirectory;
    } //-- void setDataDirectory( String )

    /**
     * Set the default language used for setting internationalized
     * strings.
     * 
     * @param defaultLanguage
     */
    public void setDefaultLanguage( String defaultLanguage )
    {
        this.defaultLanguage = defaultLanguage;
    } //-- void setDefaultLanguage( String )

    /**
     * Set the FileLocking configuration.
     * 
     * @param fileLockConfiguration
     */
    public void setFileLockConfiguration( FileLockConfiguration fileLockConfiguration )
    {
        this.fileLockConfiguration = fileLockConfiguration;
    } //-- void setFileLockConfiguration( FileLockConfiguration )

    /**
     * Set comma separated list of language patterns. Sorted by
     * priority descending. Used for display of internationalized
     * strings.
     * 
     * @param languageRange
     */
    public void setLanguageRange( String languageRange )
    {
        this.languageRange = languageRange;
    } //-- void setLanguageRange( String )

    /**
     * Set the base directory for local storage of remote
     * repository data. If not set, it's ${dataDirectory}/remotes.
     * 
     * @param remoteRepositoryBaseDirectory
     */
    public void setRemoteRepositoryBaseDirectory( String remoteRepositoryBaseDirectory )
    {
        this.remoteRepositoryBaseDirectory = remoteRepositoryBaseDirectory;
    } //-- void setRemoteRepositoryBaseDirectory( String )

    /**
     * Set the base directory for local storage of repository data.
     * If not set, it's ${dataDirectory}/repositories.
     * 
     * @param repositoryBaseDirectory
     */
    public void setRepositoryBaseDirectory( String repositoryBaseDirectory )
    {
        this.repositoryBaseDirectory = repositoryBaseDirectory;
    } //-- void setRepositoryBaseDirectory( String )


    public void setRepositoryGroupBaseDirectory(String repositoryGroupBaseDirectory) {
        this.repositoryGroupBaseDirectory = repositoryGroupBaseDirectory;
    }

    /**
     * Set the url failure cache configuration.
     * 
     * @param urlFailureCacheConfiguration
     */
    public void setUrlFailureCacheConfiguration( CacheConfiguration urlFailureCacheConfiguration )
    {
        this.urlFailureCacheConfiguration = urlFailureCacheConfiguration;
    } //-- void setUrlFailureCacheConfiguration( CacheConfiguration )


    /**
     * Returns the list of checksum types to generate
     * @return
     */
    public List<String> getChecksumTypes()
    {
        if ( this.checksumTypes == null )
        {
            this.checksumTypes = new java.util.ArrayList<String>();
        }

        return this.checksumTypes;
    }

    /**
     * Adds a checksum type
     * @param type
     */
    public void addChecksumType(String type) {

        if (!getChecksumTypes().contains(type)) {
            getChecksumTypes().add(type);
        }
    }

    /**
     * Removes a checksum type
     * @param type
     */
    public void removeChecksumType(String type) {
        getChecksumTypes().remove(type);
    }

    /**
     * Set all checksum types
     * @param checksumTypes
     */
    public void setChecksumTypes(List<String> checksumTypes) {
        if (checksumTypes!=null) {
            getChecksumTypes().clear();
            getChecksumTypes().addAll(checksumTypes);
        }
    }

}
