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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.repository.indexing.query.Query;
import org.apache.maven.repository.indexing.query.QueryTerm;
import org.apache.maven.repository.indexing.query.SingleTermQuery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * This class is to be invoked or called by the action class for
 * general and advanced searching. It uses the DefaultRepositoryIndexSearcher
 * to perform the search and constructs the search result objects to be
 * returned to tha webapp action class.
 *
 * @plexus.component role="org.apache.maven.repository.indexing.RepositoryIndexSearchLayer"
 */
public class DefaultRepositoryIndexSearchLayer
    implements RepositoryIndexSearchLayer
{
    /**
     * @plexus.requirement
     */
    private ArtifactFactory factory;

    /**
     * @plexus.requirement
     */
    private RepositoryIndexSearcher searcher;

    public List searchGeneral( String keyword, RepositoryIndex index )
        throws RepositoryIndexSearchException
    {
        List generalSearchResults = new ArrayList();
        for ( int i = 0; i < RepositoryIndex.FIELDS.length; i++ )
        {
            // TODO! does simply iterating the fields and searching each perform well enough and yield correct rankings?
            // look into: http://wiki.apache.org/jakarta-lucene/LuceneFAQ#head-300f0756fdaa71f522c96a868351f716573f2d77
            // ie: http://lucene.apache.org/java/docs/api/org/apache/lucene/queryParser/MultiFieldQueryParser.html
            QueryTerm term = new QueryTerm( RepositoryIndex.FIELDS[i], keyword );
            List results = searchAdvanced( new SingleTermQuery( term ), index );
            for ( Iterator iter = results.iterator(); iter.hasNext(); )
            {
                SearchResult result = (SearchResult) iter.next();
                Map map = result.getFieldMatches();
                Set entrySet = map.entrySet();
                for ( Iterator it = entrySet.iterator(); it.hasNext(); )
                {
                    Map.Entry entry = (Map.Entry) it.next();
                    SearchResult result2 = createSearchResult( result.getArtifact(), map, keyword,
                                                               (String) entry.getKey(), generalSearchResults );
                    generalSearchResults.add( result2 );
                }
            }
        }

        return generalSearchResults;
    }

    public List searchAdvanced( Query qry, RepositoryIndex index )
        throws RepositoryIndexSearchException
    {
        List searchResults = new ArrayList();

        List hits = searcher.search( qry, index );
        for ( Iterator it = hits.iterator(); it.hasNext(); )
        {
            RepositoryIndexSearchHit hit = (RepositoryIndexSearchHit) it.next();
            SearchResult result = new SearchResult();
            if ( hit.isHashMap() )
            {
                Map map = (Map) hit.getObject();
                result.setArtifact( (Artifact) map.get( RepositoryIndex.ARTIFACT ) );

                Map fields = new HashMap();
                fields.put( RepositoryIndex.FLD_CLASSES, map.get( RepositoryIndex.FLD_CLASSES ) );
                fields.put( RepositoryIndex.FLD_PACKAGES, map.get( RepositoryIndex.FLD_PACKAGES ) );
                fields.put( RepositoryIndex.FLD_FILES, map.get( RepositoryIndex.FLD_FILES ) );
                fields.put( RepositoryIndex.FLD_PACKAGING, map.get( RepositoryIndex.FLD_PACKAGING ) );
                fields.put( RepositoryIndex.FLD_SHA1, map.get( RepositoryIndex.FLD_SHA1 ) );
                fields.put( RepositoryIndex.FLD_MD5, map.get( RepositoryIndex.FLD_MD5 ) );

                // TODO! this doesn't seem like the correct way to determine what matched
                result.setFieldMatches( fields );
                searchResults.add( result );
            }
            else if ( hit.isModel() )
            {
                Model model = (Model) hit.getObject();
                for ( int i = 0; i < RepositoryIndex.MODEL_FIELDS.length; i++ )
                {
                    result = createSearchResult( model, RepositoryIndex.MODEL_FIELDS[i], searchResults );
                    searchResults.add( result );
                }
            }
            else if ( hit.isMetadata() )
            {
                //@todo what about metadata objects?
//                RepositoryMetadata metadata = (RepositoryMetadata) hit.getObject();
            }
        }

        return searchResults;
    }

    /**
     * Method for checking if the artifact already exists in the search result list.
     *
     * @param groupId    the group id of the artifact
     * @param artifactId the artifact id of the artifact
     * @param version    the version of the artifact
     * @return the int index number of the artifact in the search result
     */
    private int getListIndex( String groupId, String artifactId, String version, List list )
    {
        int index = 0;
        for ( Iterator iter = list.iterator(); iter.hasNext(); )
        {
            SearchResult result = (SearchResult) iter.next();
            Artifact artifact = result.getArtifact();
            if ( artifact.getGroupId().equals( groupId ) && artifact.getArtifactId().equals( artifactId ) &&
                artifact.getVersion().equals( version ) )
            {
                return index;
            }
            index++;
        }
        return -1;
    }

    /**
     * Method to create the unique artifact id to represent the artifact in the repository
     *
     * @param groupId    the artifact groupId
     * @param artifactId the artifact artifactId
     * @param version    the artifact version
     * @return the String id to uniquely represent the artifact
     */
    private String getId( String groupId, String artifactId, String version )
    {
        return groupId + ":" + artifactId + ":" + version;
    }

    /**
     * Method to get the matching values (packages, classes and files) in the
     * given string to be tokenized.
     *
     * @param tokenizeStr the string to be tokenized
     * @param key         the map key
     * @param resultMap   the map to be populated
     * @param keyword     the value to be matched
     * @return the map that contains the matched values
     */
    private Map getArtifactHits( String tokenizeStr, String key, Map resultMap, String keyword )
    {
        List values = new ArrayList();
        StringTokenizer st = new StringTokenizer( tokenizeStr, "\n" );
        while ( st.hasMoreTokens() )
        {
            String str = st.nextToken();
            if ( str.toLowerCase().indexOf( keyword.toLowerCase() ) != -1 )
            {
                values.add( str );
            }
        }

        if ( !values.isEmpty() )
        {
            resultMap.put( key, values );
        }

        return resultMap;
    }

    /**
     * Method to create SearchResult object from a given HashMap. Used for general search results
     *
     * @param artifact the retrieved artifact from the index
     * @param map      the HashMap object that contains the values for the search result
     * @param keyword  the query term
     * @return the SearchResult object
     */
    private SearchResult createSearchResult( Artifact artifact, Map map, String keyword, String field,
                                             List generalSearchResults )
    {
        int index = getListIndex( artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(),
                                  generalSearchResults );
        SearchResult result;
        Map resultMap;

        if ( index > -1 )
        {
            result = (SearchResult) generalSearchResults.remove( index );
            resultMap = result.getFieldMatches();
        }
        else
        {
            result = new SearchResult();
            result.setArtifact( artifact );
            resultMap = new HashMap();
        }

        // the searched field is either the class, package or file field
        if ( field.equals( RepositoryIndex.FLD_CLASSES ) || field.equals( RepositoryIndex.FLD_PACKAGES ) ||
            field.equals( RepositoryIndex.FLD_FILES ) )
        {
            resultMap = getArtifactHits( (String) map.get( field ), field, resultMap, keyword );
        }
        else if ( field.equals( RepositoryIndex.FLD_SHA1 ) ||
            ( field.equals( RepositoryIndex.FLD_MD5 ) || field.equals( RepositoryIndex.FLD_PACKAGING ) ) )
        {
            if ( map.get( field ) != null )
            {
                // the searched field is either the md5, sha1 or packaging field
                if ( ( (String) map.get( field ) ).toLowerCase().equals( keyword.toLowerCase() ) ||
                    ( (String) map.get( field ) ).toLowerCase().indexOf( keyword.toLowerCase() ) != -1 )
                {
                    resultMap.put( field, map.get( field ) );
                }
            }
        }
        else if ( field.equals( RepositoryIndex.FLD_DEPENDENCIES ) ||
            field.equals( RepositoryIndex.FLD_PLUGINS_BUILD ) || field.equals( RepositoryIndex.FLD_PLUGINS_REPORT ) ||
            field.equals( RepositoryIndex.FLD_LICENSE_URLS ) )
        {
            List contents = (List) map.get( field );
            List values = new ArrayList();
            for ( Iterator it = contents.iterator(); it.hasNext(); )
            {
                String str = (String) it.next();
                if ( str.toLowerCase().equals( keyword.toLowerCase() ) )
                {
                    values.add( str );
                }
            }
            if ( values.size() > 0 )
            {
                resultMap.put( field, values );
            }
        }
        result.setFieldMatches( resultMap );

        return result;
    }

    /**
     * Method to create a SearchResult object from the given model. Used for advanced search results
     *
     * @param model the Model object that contains the values for the search result
     * @param field the field whose value is to be retrieved
     * @return a SearchResult object
     */
    private SearchResult createSearchResult( Model model, String field, List searchResults )
    {
        String groupId = model.getGroupId();
        if ( groupId == null )
        {
            groupId = model.getParent().getGroupId();
        }
        String version = model.getVersion();
        if ( version == null )
        {
            version = model.getParent().getVersion();
        }
        int index = getListIndex( groupId, model.getArtifactId(), version, searchResults );
        SearchResult result;
        Map map;

        // the object already exists in the search result list
        if ( index > -1 )
        {
            result = (SearchResult) searchResults.remove( index );
            map = result.getFieldMatches();
        }
        else
        {
            result = new SearchResult();
            result.setArtifact(
                factory.createBuildArtifact( groupId, model.getArtifactId(), version, model.getPackaging() ) );
            map = new HashMap();
        }

        // get the matched value with the query term
        List values = new ArrayList();
        if ( field.equals( RepositoryIndex.FLD_LICENSE_URLS ) )
        {
            values = getLicenseUrls( model );
        }
        else if ( field.equals( RepositoryIndex.FLD_DEPENDENCIES ) )
        {
            values = getDependencies( model );
        }
        else if ( field.equals( RepositoryIndex.FLD_PLUGINS_BUILD ) )
        {
            if ( model.getBuild() != null && model.getBuild().getPlugins() != null )
            {
                values = getBuildPlugins( model );
            }
        }
        else if ( field.equals( RepositoryIndex.FLD_PLUGINS_REPORT ) )
        {
            if ( model.getReporting() != null && model.getReporting().getPlugins() != null )
            {
                values = getReportPlugins( model );
            }
        }
        else if ( field.equals( RepositoryIndex.FLD_PACKAGING ) )
        {
            if ( model.getPackaging() != null )
            {
                map.put( RepositoryIndex.FLD_PACKAGING, model.getPackaging() );
            }
        }

        if ( !values.isEmpty() )
        {
            map.put( field, values );
        }
        result.setFieldMatches( map );

        return result;
    }

    /**
     * Method for getting the query term hits or matches in the pom's license urls.
     *
     * @param model the Model object that contains the pom values
     * @return a List of matched license urls
     */
    private List getLicenseUrls( Model model )
    {
        List licenseUrls = new ArrayList();
        List licenseList = model.getLicenses();
        for ( Iterator it = licenseList.iterator(); it.hasNext(); )
        {
            License license = (License) it.next();
            licenseUrls.add( license.getUrl() );
        }
        return licenseUrls;
    }

    /**
     * Method for getting the hits or matches in the dependencies specified in the pom
     *
     * @param model the Model object that contains the pom values
     * @return a List of matched dependencies
     */
    private List getDependencies( Model model )
    {
        List dependencies = new ArrayList();
        List dependencyList = model.getDependencies();
        for ( Iterator it = dependencyList.iterator(); it.hasNext(); )
        {
            Dependency dep = (Dependency) it.next();
            dependencies.add( getId( dep.getGroupId(), dep.getArtifactId(), dep.getVersion() ) );
        }

        return dependencies;
    }

    /**
     * Method for getting the hits or matches in the build plugins specified in the pom
     *
     * @param model the Model object that contains the pom values
     * @return a List of matched build plugins
     */
    private List getBuildPlugins( Model model )
    {
        List values = new ArrayList();
        List plugins = model.getBuild().getPlugins();
        for ( Iterator it = plugins.iterator(); it.hasNext(); )
        {
            Plugin plugin = (Plugin) it.next();
            values.add( getId( plugin.getGroupId(), plugin.getArtifactId(), plugin.getVersion() ) );
        }

        return values;
    }

    /**
     * Method for getting the hits or matches in the reporting plugins specified in the pom
     *
     * @param model the Model object that contains the pom values
     * @return a List of matched reporting plugins
     */
    private List getReportPlugins( Model model )
    {
        List values = new ArrayList();
        List plugins = model.getReporting().getPlugins();
        for ( Iterator it = plugins.iterator(); it.hasNext(); )
        {
            ReportPlugin plugin = (ReportPlugin) it.next();
            values.add( getId( plugin.getGroupId(), plugin.getArtifactId(), plugin.getVersion() ) );
        }

        return values;
    }

}
