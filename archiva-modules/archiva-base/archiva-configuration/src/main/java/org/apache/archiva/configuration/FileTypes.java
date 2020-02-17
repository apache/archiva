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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.common.FileTypeUtils;
import org.apache.archiva.configuration.functors.FiletypeSelectionPredicate;
import org.apache.archiva.components.registry.Registry;
import org.apache.archiva.components.registry.RegistryListener;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FileTypes
 */
@Service("fileTypes")
public class FileTypes
    implements RegistryListener
{
    public static final String ARTIFACTS = "artifacts";

    public static final String AUTO_REMOVE = "auto-remove";

    public static final String INDEXABLE_CONTENT = "indexable-content";

    public static final String IGNORED = "ignored";

    @Inject
    @Named(value = "archivaConfiguration#default")
    private ArchivaConfiguration archivaConfiguration;


    public FileTypes() {

    }

    /**
     * Map of default values for the file types.
     */
    private Map<String, List<String>> defaultTypeMap = new HashMap<>();

    private List<String> artifactPatterns;

    /**
     * Default exclusions from artifact consumers that are using the file types. Note that this is simplistic in the
     * case of the support files (based on extension) as it is elsewhere - it may be better to match these to actual
     * artifacts and exclude later during scanning.
     *
     * @deprecated
     */
    public static final List<String> DEFAULT_EXCLUSIONS = FileTypeUtils.DEFAULT_EXCLUSIONS;

    public void setArchivaConfiguration( ArchivaConfiguration archivaConfiguration )
    {
        this.archivaConfiguration = archivaConfiguration;
    }

    /**
     * Get the list of patterns for a specified filetype.
     * You will always get a list.  In this order.
     * <ul>
     * <li>The Configured List</li>
     * <li>The Default List</li>
     * <li>A single item list of <code>&quot;**&#47;*&quot;</code></li>
     * </ul>
     *
     * @param id the id to lookup.
     * @return the list of patterns.
     */
    public List<String> getFileTypePatterns( String id )
    {
        Configuration config = archivaConfiguration.getConfiguration();
        Predicate selectedFiletype = new FiletypeSelectionPredicate( id );
        RepositoryScanningConfiguration repositoryScanningConfiguration = config.getRepositoryScanning();
        if ( repositoryScanningConfiguration != null )
        {
            FileType filetype =
                IterableUtils.find( config.getRepositoryScanning().getFileTypes(), selectedFiletype );

            if ( ( filetype != null ) && CollectionUtils.isNotEmpty( filetype.getPatterns() ) )
            {
                return filetype.getPatterns();
            }
        }
        List<String> defaultPatterns = defaultTypeMap.get( id );

        if ( CollectionUtils.isEmpty( defaultPatterns ) )
        {
            return Collections.singletonList( "**/*" );
        }

        return defaultPatterns;
    }

    public synchronized boolean matchesArtifactPattern( String relativePath )
    {
        // Correct the slash pattern.
        relativePath = relativePath.replace( '\\', '/' );

        if ( artifactPatterns == null )
        {
            artifactPatterns = getFileTypePatterns( ARTIFACTS );
        }

        for ( String pattern : artifactPatterns )
        {
            if ( FileSystems.getDefault().getPathMatcher( "glob:" + pattern).matches( Paths.get( relativePath ) ) )
            {
                // Found match
                return true;
            }
        }

        // No match.
        return false;
    }

    public boolean matchesDefaultExclusions( String relativePath )
    {
        // Correct the slash pattern.
        relativePath = relativePath.replace( '\\', '/' );

        for ( String pattern : DEFAULT_EXCLUSIONS )
        {
            if ( FileSystems.getDefault().getPathMatcher( "glob:" + pattern).matches( Paths.get( relativePath ) ) )
            {
                // Found match
                return true;
            }
        }

        // No match.
        return false;
    }

    @PostConstruct
    public void initialize()
    {
        initialiseTypeMap( this.archivaConfiguration.getConfiguration() );

        this.archivaConfiguration.addChangeListener( this );
    }

    private void initialiseTypeMap( Configuration configuration )
    {
        defaultTypeMap.clear();

        // Store the default file type declaration.
        List<FileType> filetypes = configuration.getRepositoryScanning().getFileTypes();
        for ( FileType filetype : filetypes )
        {
            List<String> patterns = defaultTypeMap.get( filetype.getId() );
            if ( patterns == null )
            {
                patterns = new ArrayList<>( filetype.getPatterns().size() );
            }
            patterns.addAll( filetype.getPatterns() );

            defaultTypeMap.put( filetype.getId(), patterns );
        }
    }

    @Override
    public void afterConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        if ( propertyName.contains( "fileType" ) )
        {
            artifactPatterns = null;

            initialiseTypeMap( archivaConfiguration.getConfiguration() );
        }
    }

    @Override
    public void beforeConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        /* nothing to do */
    }
}
