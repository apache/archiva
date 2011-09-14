package org.apache.archiva.indexer.search;

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

import java.util.ArrayList;
import java.util.List;

/**
 * SearchResultHit
 *
 * @version $Id: SearchResultHit.java 740552 2009-02-04 01:09:17Z oching $
 */
public class SearchResultHit
{
    // The (optional) context for this result.
    private String context;

    // Basic hit, direct to non-artifact resource.
    private String url;

    // Advanced hit, reference to groupId.
    private String groupId;

    //  Advanced hit, reference to artifactId.
    private String artifactId;

    private String repositoryId = "";

    private List<String> versions = new ArrayList<String>();

    private String packaging;

    /**
     * Plugin goal prefix (only if packaging is "maven-plugin")
     */
    private String prefix;

    /**
     * Plugin goals (only if packaging is "maven-plugin")
     */
    private List<String> goals;

    /**
     * contains osgi metadata Bundle-Version if available
     *
     * @since 1.4
     */
    private String bundleVersion;

    /**
     * contains osgi metadata Bundle-SymbolicName if available
     *
     * @since 1.4
     */
    private String bundleSymbolicName;

    /**
     * contains osgi metadata Export-Package if available
     *
     * @since 1.4
     */
    private String bundleExportPackage;

    /**
     * contains osgi metadata Export-Service if available
     *
     * @since 1.4
     */
    private String bundleExportService;

    /**
     * contains osgi metadata Bundle-Description if available
     *
     * @since 1.4
     */
    private String bundleDescription;

    /**
     * contains osgi metadata Bundle-Name if available
     *
     * @since 1.4
     */
    private String bundleName;

    /**
     * contains osgi metadata Bundle-License if available
     *
     * @since 1.4
     */
    private String bundleLicense;

    /**
     * contains osgi metadata Bundle-DocURL if available
     *
     * @since 1.4
     */
    private String bundleDocUrl;

    /**
     * contains osgi metadata Import-Package if available
     *
     * @since 1.4
     */
    private String bundleImportPackage;

    /**
     * contains osgi metadata Require-Bundle if available
     *
     * @since 1.4
     */
    private String bundleRequireBundle;

    private String classifier;

    public String getContext()
    {
        return context;
    }

    public void setContext( String context )
    {
        this.context = context;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl( String url )
    {
        this.url = url;
    }

    public String getUrlFilename()
    {
        return this.url.substring( this.url.lastIndexOf( '/' ) );
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public List<String> getVersions()
    {
        return versions;
    }

    public void setVersions( List<String> versions )
    {
        this.versions = versions;
    }

    public String getRepositoryId()
    {
        return repositoryId;
    }

    public void setRepositoryId( String repositoryId )
    {
        this.repositoryId = repositoryId;
    }

    public void addVersion( String version )
    {
        versions.add( version );
    }

    public String getBundleVersion()
    {
        return bundleVersion;
    }

    public void setBundleVersion( String bundleVersion )
    {
        this.bundleVersion = bundleVersion;
    }

    public String getBundleSymbolicName()
    {
        return bundleSymbolicName;
    }

    public void setBundleSymbolicName( String bundleSymbolicName )
    {
        this.bundleSymbolicName = bundleSymbolicName;
    }

    public String getBundleExportPackage()
    {
        return bundleExportPackage;
    }

    public void setBundleExportPackage( String bundleExportPackage )
    {
        this.bundleExportPackage = bundleExportPackage;
    }

    public String getBundleExportService()
    {
        return bundleExportService;
    }

    public void setBundleExportService( String bundleExportService )
    {
        this.bundleExportService = bundleExportService;
    }

    public String getPrefix()
    {
        return prefix;
    }

    public void setPrefix( String prefix )
    {
        this.prefix = prefix;
    }

    public List<String> getGoals()
    {
        return goals;
    }

    public void setGoals( List<String> goals )
    {
        this.goals = goals;
    }

    public String getBundleDescription()
    {
        return bundleDescription;
    }

    public void setBundleDescription( String bundleDescription )
    {
        this.bundleDescription = bundleDescription;
    }

    public String getBundleName()
    {
        return bundleName;
    }

    public void setBundleName( String bundleName )
    {
        this.bundleName = bundleName;
    }

    public String getBundleLicense()
    {
        return bundleLicense;
    }

    public void setBundleLicense( String bundleLicense )
    {
        this.bundleLicense = bundleLicense;
    }

    public String getBundleDocUrl()
    {
        return bundleDocUrl;
    }

    public void setBundleDocUrl( String bundleDocUrl )
    {
        this.bundleDocUrl = bundleDocUrl;
    }

    public String getBundleImportPackage()
    {
        return bundleImportPackage;
    }

    public void setBundleImportPackage( String bundleImportPackage )
    {
        this.bundleImportPackage = bundleImportPackage;
    }

    public String getBundleRequireBundle()
    {
        return bundleRequireBundle;
    }

    public void setBundleRequireBundle( String bundleRequireBundle )
    {
        this.bundleRequireBundle = bundleRequireBundle;
    }

    public String getPackaging()
    {
        return packaging;
    }

    public void setPackaging( String packaging )
    {
        this.packaging = packaging;
    }

    public String getType()
    {
        return getPackaging();
    }

    public String getClassifier()
    {
        return classifier;
    }

    public void setClassifier( String classifier )
    {
        this.classifier = classifier;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "SearchResultHit" );
        sb.append( "{context='" ).append( context ).append( '\'' );
        sb.append( ", url='" ).append( url ).append( '\'' );
        sb.append( ", groupId='" ).append( groupId ).append( '\'' );
        sb.append( ", artifactId='" ).append( artifactId ).append( '\'' );
        sb.append( ", repositoryId='" ).append( repositoryId ).append( '\'' );
        sb.append( ", versions=" ).append( versions );
        sb.append( ", packaging='" ).append( packaging ).append( '\'' );
        sb.append( ", prefix='" ).append( prefix ).append( '\'' );
        sb.append( ", goals=" ).append( goals );
        sb.append( ", bundleVersion='" ).append( bundleVersion ).append( '\'' );
        sb.append( ", bundleSymbolicName='" ).append( bundleSymbolicName ).append( '\'' );
        sb.append( ", bundleExportPackage='" ).append( bundleExportPackage ).append( '\'' );
        sb.append( ", bundleExportService='" ).append( bundleExportService ).append( '\'' );
        sb.append( ", bundleDescription='" ).append( bundleDescription ).append( '\'' );
        sb.append( ", bundleName='" ).append( bundleName ).append( '\'' );
        sb.append( ", bundleLicense='" ).append( bundleLicense ).append( '\'' );
        sb.append( ", bundleDocUrl='" ).append( bundleDocUrl ).append( '\'' );
        sb.append( ", bundleImportPackage='" ).append( bundleImportPackage ).append( '\'' );
        sb.append( ", bundleRequireBundle='" ).append( bundleRequireBundle ).append( '\'' );
        sb.append( ", classifier='" ).append( classifier ).append( '\'' );
        sb.append( '}' );
        return sb.toString();
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        SearchResultHit that = (SearchResultHit) o;

        if ( artifactId != null ? !artifactId.equals( that.artifactId ) : that.artifactId != null )
        {
            return false;
        }
        if ( classifier != null ? !classifier.equals( that.classifier ) : that.classifier != null )
        {
            return false;
        }
        if ( groupId != null ? !groupId.equals( that.groupId ) : that.groupId != null )
        {
            return false;
        }
        if ( packaging != null ? !packaging.equals( that.packaging ) : that.packaging != null )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = groupId != null ? groupId.hashCode() : 0;
        result = 31 * result + ( artifactId != null ? artifactId.hashCode() : 0 );
        result = 31 * result + ( packaging != null ? packaging.hashCode() : 0 );
        result = 31 * result + ( classifier != null ? classifier.hashCode() : 0 );
        return result;
    }
}
