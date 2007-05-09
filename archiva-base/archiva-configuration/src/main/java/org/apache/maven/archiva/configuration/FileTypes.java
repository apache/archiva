package org.apache.maven.archiva.configuration;

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

import org.apache.commons.collections.Closure;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.functors.FiletypeSelectionPredicate;
import org.apache.maven.archiva.xml.ElementTextListClosure;
import org.apache.maven.archiva.xml.XMLException;
import org.apache.maven.archiva.xml.XMLReader;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.dom4j.Element;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FileTypes 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.configuration.FileTypes"
 */
public class FileTypes
    implements Initializable
{
    public static final String ARTIFACTS = "artifacts";

    public static final String AUTO_REMOVE = "auto-remove";

    public static final String INDEXABLE_CONTENT = "indexable-content";

    public static final String IGNORED = "ignored";

    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration archivaConfiguration;

    /**
     * Map of default values for the file types.
     */
    private Map defaultTypeMap = new HashMap();

    /**
     * <p>
     * Get the list of patterns for a specified filetype.
     * </p>
     * 
     * <p>
     * You will always get a list.  In this order.
     *   <ul>
     *     <li>The Configured List</li>
     *     <li>The Default List</li>
     *     <li>A single item list of <code>"**<span>/</span>*"</code></li>
     *   </ul>
     * </p>
     * 
     * @param id the id to lookup.
     * @return the list of patterns.
     */
    public List getFileTypePatterns( String id )
    {
        Configuration config = archivaConfiguration.getConfiguration();
        Predicate selectedFiletype = new FiletypeSelectionPredicate( id );
        FileType filetype = (FileType) CollectionUtils.find( config.getRepositoryScanning().getFileTypes(),
                                                             selectedFiletype );

        if ( ( filetype != null ) && CollectionUtils.isNotEmpty( filetype.getPatterns() ) )
        {
            return filetype.getPatterns();
        }

        List defaultPatterns = (List) defaultTypeMap.get( id );

        if ( CollectionUtils.isEmpty( defaultPatterns ) )
        {
            return Collections.singletonList( "**/*" );
        }

        return defaultPatterns;
    }

    public void initialize()
        throws InitializationException
    {
        defaultTypeMap.clear();

        try
        {
            URL defaultArchivaXml = this.getClass()
                .getResource( "/org/apache/maven/archiva/configuration/default-archiva.xml" );

            XMLReader reader = new XMLReader( "configuration", defaultArchivaXml );
            List resp = reader.getElementList( "//configuration/repositoryScanning/fileTypes/fileType" );

            CollectionUtils.forAllDo( resp, new AddFileTypeToDefaultMap() );
        }
        catch ( XMLException e )
        {
            throw new InitializationException( "Unable to setup default filetype maps.", e );
        }
    }

    class AddFileTypeToDefaultMap
        implements Closure
    {
        public void execute( Object input )
        {
            if ( !( input instanceof Element ) )
            {
                // Not an element. skip.
                return;
            }

            Element elem = (Element) input;
            if ( !StringUtils.equals( "fileType", elem.getName() ) )
            {
                // Not a 'fileType' element. skip.
                return;
            }

            String id = elem.elementText( "id" );
            Element patternsElem = elem.element( "patterns" );
            if ( patternsElem == null )
            {
                // No patterns. skip.
                return;
            }

            List patternElemList = patternsElem.elements( "pattern" );

            ElementTextListClosure elemTextList = new ElementTextListClosure();
            CollectionUtils.forAllDo( patternElemList, elemTextList );
            List patterns = elemTextList.getList();

            defaultTypeMap.put( id, patterns );
        }
    }
}
