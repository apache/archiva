package org.apache.archiva.indexer.search;

import java.util.ArrayList;
import java.util.List;

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

public class SearchFields
{
    /**
     * groupId
     */
    private String groupId;

    /**
     * artifactId
     */
    private String artifactId;

    /**
     * version
     */
    private String version;

    /**
     * packaging (jar, war, pom, etc.)
     */
    private String packaging;

    /**
     * class name or package name
     */
    private String className;

    /**
     * repositories
     */
    private List<String> repositories = new ArrayList<String>();


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
     * contains osgi metadata import package if available
     *
     * @since 1.4
     */
    private String bundleImportPackage;

    /**
     * contains osgi metadata name if available
     *
     * @since 1.4
     */
    private String bundleName;

    /**
     * contains osgi metadata Export-Service if available
     *
     * @since 1.4
     */
    private String bundleExportService;

    private String classifier;

    public SearchFields()
    {
        // no op
    }

    public SearchFields( String groupId, String artifactId, String version, String packaging, String className,
                         List<String> repositories )
    {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.packaging = packaging;
        this.className = className;
        this.repositories = repositories;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    public String getPackaging()
    {
        return packaging;
    }

    public void setPackaging( String packaging )
    {
        this.packaging = packaging;
    }

    public String getClassName()
    {
        return className;
    }

    public void setClassName( String className )
    {
        this.className = className;
    }

    public List<String> getRepositories()
    {
        return repositories;
    }

    public void setRepositories( List<String> repositories )
    {
        this.repositories = repositories;
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

    public String getClassifier()
    {
        return classifier;
    }

    public void setClassifier( String classifier )
    {
        this.classifier = classifier;
    }

    public String getBundleImportPackage()
    {
        return bundleImportPackage;
    }

    public void setBundleImportPackage( String bundleImportPackage )
    {
        this.bundleImportPackage = bundleImportPackage;
    }

    public String getBundleName()
    {
        return bundleName;
    }

    public void setBundleImportName( String bundleName )
    {
        this.bundleName = bundleName;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "SearchFields" );
        sb.append( "{groupId='" ).append( groupId ).append( '\'' );
        sb.append( ", artifactId='" ).append( artifactId ).append( '\'' );
        sb.append( ", version='" ).append( version ).append( '\'' );
        sb.append( ", packaging='" ).append( packaging ).append( '\'' );
        sb.append( ", className='" ).append( className ).append( '\'' );
        sb.append( ", repositories=" ).append( repositories );
        sb.append( ", bundleVersion='" ).append( bundleVersion ).append( '\'' );
        sb.append( ", bundleSymbolicName='" ).append( bundleSymbolicName ).append( '\'' );
        sb.append( ", bundleExportPackage='" ).append( bundleExportPackage ).append( '\'' );
        sb.append( ", bundleImportPackage='" ).append( bundleImportPackage ).append( '\'' );
        sb.append( ", bundleName='" ).append( bundleName ).append( '\'' );
        sb.append( ", bundleExportService='" ).append( bundleExportService ).append( '\'' );
        sb.append( ", classifier='" ).append( classifier ).append( '\'' );
        sb.append( '}' );
        return sb.toString();
    }
}
