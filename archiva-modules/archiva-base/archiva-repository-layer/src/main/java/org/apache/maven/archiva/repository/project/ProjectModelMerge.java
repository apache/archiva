package org.apache.maven.archiva.repository.project;

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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.model.ArchivaModelCloner;
import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.model.CiManagement;
import org.apache.maven.archiva.model.Dependency;
import org.apache.maven.archiva.model.Exclusion;
import org.apache.maven.archiva.model.Individual;
import org.apache.maven.archiva.model.IssueManagement;
import org.apache.maven.archiva.model.License;
import org.apache.maven.archiva.model.Organization;
import org.apache.maven.archiva.model.ProjectRepository;
import org.apache.maven.archiva.model.Scm;

/**
 * ProjectModelMerge
 *
 * TODO: Should call this ProjectModelAncestry as it deals with the current project and its parent.
 *
 * @version $Id$
 */
public class ProjectModelMerge
{
    /**
     * Merge the contents of a project with it's parent project.
     * 
     * @param mainProject the main project.
     * @param parentProject the parent project to merge.
     * @throws ProjectModelException if there was a problem merging the model.
     */
    public static ArchivaProjectModel merge( ArchivaProjectModel mainProject, ArchivaProjectModel parentProject )
        throws ProjectModelException
    {
        if ( mainProject == null )
        {
            throw new ProjectModelException( "Cannot merge with a null main project." );
        }

        if ( parentProject == null )
        {
            throw new ProjectModelException( "Cannot merge with a null parent project." );
        }

        ArchivaProjectModel merged = new ArchivaProjectModel();

        // Unmerged.
        merged.setParentProject(mainProject.getParentProject());
        merged.setArtifactId( mainProject.getArtifactId() );
        merged.setPackaging( StringUtils.defaultIfEmpty( mainProject.getPackaging(), "jar" ) );
        merged.setRelocation( mainProject.getRelocation() );

        // Merged
        merged.setGroupId( merge( mainProject.getGroupId(), parentProject.getGroupId() ) );
        merged.setVersion( merge( mainProject.getVersion(), parentProject.getVersion() ) );
        merged.setName( merge( mainProject.getName(), parentProject.getName() ) );
        merged.setUrl( merge( mainProject.getUrl(), parentProject.getUrl() ) );
        merged.setDescription( merge( mainProject.getDescription(), parentProject.getDescription() ) );

        merged.setOrigin( "merged" );

        merged.setCiManagement( merge( mainProject.getCiManagement(), parentProject.getCiManagement() ) );
        merged.setIndividuals( mergeIndividuals( mainProject.getIndividuals(), parentProject.getIndividuals() ) );
        merged.setIssueManagement( merge( mainProject.getIssueManagement(), parentProject.getIssueManagement() ) );
        merged.setLicenses( mergeLicenses( mainProject.getLicenses(), parentProject.getLicenses() ) );
        merged.setOrganization( merge( mainProject.getOrganization(), parentProject.getOrganization() ) );
        merged.setScm( merge( mainProject.getScm(), parentProject.getScm() ) );
        merged.setRepositories( mergeRepositories( mainProject.getRepositories(), parentProject.getRepositories() ) );
        merged.setDependencies( mergeDependencies( mainProject.getDependencies(), parentProject.getDependencies() ) );
        merged.setDependencyManagement( mergeDependencyManagement( mainProject.getDependencyManagement(), parentProject
            .getDependencyManagement() ) );
        merged.setPlugins( mergePlugins( mainProject.getPlugins(), parentProject.getPlugins() ) );
        merged.setReports( mergeReports( mainProject.getReports(), parentProject.getReports() ) );
        merged.setProperties( merge( mainProject.getProperties(), parentProject.getProperties() ) );
        
        return merged;
    }

    private static Map<String, ArtifactReference> createArtifactReferenceMap( List<ArtifactReference> artifactReferences )
    {
        Map<String, ArtifactReference> ret = new HashMap<String, ArtifactReference>();

        for ( ArtifactReference artifactReference : artifactReferences )
        {
            String key = toVersionlessArtifactKey( artifactReference );
            ret.put( key, artifactReference );
        }

        return ret;
    }

    private static Map<String, Dependency> createDependencyMap( List<Dependency> dependencies )
    {
        Map<String, Dependency> ret = new HashMap<String, Dependency>();

        Iterator<Dependency> it = dependencies.iterator();
        while ( it.hasNext() )
        {
            Dependency dep = it.next();
            String key = toVersionlessDependencyKey( dep );
            ret.put( key, dep );
        }

        return ret;
    }

    private static Map<String, Exclusion> createExclusionMap( List<Exclusion> exclusions )
    {
        Map<String, Exclusion> ret = new HashMap<String, Exclusion>();

        Iterator<Exclusion> it = exclusions.iterator();
        while ( it.hasNext() )
        {
            Exclusion exclusion = it.next();
            String key = exclusion.getGroupId() + ":" + exclusion.getArtifactId();
            ret.put( key, exclusion );
        }

        return ret;
    }

    private static Map<String, License> createLicensesMap( List<License> licenses )
    {
        Map<String, License> ret = new HashMap<String, License>();

        for ( License license : licenses )
        {
            // TODO: Change to 'id' when LicenseTypeMapper is created.
            String key = license.getName();
            ret.put( key, license );
        }

        return ret;
    }

    private static Map<String, ProjectRepository> createRepositoriesMap( List<ProjectRepository> repositories )
    {
        Map<String, ProjectRepository> ret = new HashMap<String, ProjectRepository>();

        for ( ProjectRepository repo : repositories )
        {
            // Should this really be using repo.id ?
            String key = repo.getUrl();
            ret.put( key, repo );
        }

        return ret;
    }

    private static boolean empty( String val )
    {
        if ( val == null )
        {
            return true;
        }

        return ( val.trim().length() <= 0 );
    }

    private static ArtifactReference merge( ArtifactReference mainArtifactReference,
                                            ArtifactReference parentArtifactReference )
    {
        if ( parentArtifactReference == null )
        {
            return mainArtifactReference;
        }

        if ( mainArtifactReference == null )
        {
            return ArchivaModelCloner.clone( parentArtifactReference );
        }

        ArtifactReference merged = new ArtifactReference();

        // Unmerged.
        merged.setGroupId( mainArtifactReference.getGroupId() );
        merged.setArtifactId( mainArtifactReference.getArtifactId() );

        // Merged.
        merged.setVersion( merge( mainArtifactReference.getVersion(), parentArtifactReference.getVersion() ) );
        merged.setClassifier( merge( mainArtifactReference.getClassifier(), parentArtifactReference.getClassifier() ) );
        merged.setType( merge( mainArtifactReference.getType(), parentArtifactReference.getType() ) );

        return merged;
    }

    private static CiManagement merge( CiManagement mainCim, CiManagement parentCim )
    {
        if ( parentCim == null )
        {
            return mainCim;
        }

        if ( mainCim == null )
        {
            return ArchivaModelCloner.clone( parentCim );
        }

        CiManagement merged = new CiManagement();

        merged.setSystem( merge( mainCim.getSystem(), parentCim.getSystem() ) );
        merged.setUrl( merge( mainCim.getUrl(), parentCim.getUrl() ) );
        merged.setCiUrl( merge( mainCim.getCiUrl(), parentCim.getCiUrl() ) );

        return merged;
    }

    private static Dependency merge( Dependency mainDep, Dependency parentDep )
    {
        if ( parentDep == null )
        {
            return mainDep;
        }

        if ( mainDep == null )
        {
            Dependency dep = ArchivaModelCloner.clone( parentDep );
            dep.setFromParent( true );
            return dep;
        }

        Dependency merged = new Dependency();

        merged.setFromParent( true );

        // Unmerged.
        merged.setGroupId( mainDep.getGroupId() );
        merged.setArtifactId( mainDep.getArtifactId() );

        // Merged.
        merged.setVersion( merge( mainDep.getVersion(), parentDep.getVersion() ) );
        merged.setClassifier( merge( mainDep.getClassifier(), parentDep.getClassifier() ) );
        merged.setType( merge( mainDep.getType(), parentDep.getType() ) );
        merged.setScope( merge( mainDep.getScope(), parentDep.getScope() ) );
        if ( parentDep.isOptional() )
        {
            merged.setOptional( true );
        }

        merged.setSystemPath( merge( mainDep.getSystemPath(), parentDep.getSystemPath() ) );
        merged.setUrl( merge( mainDep.getUrl(), parentDep.getUrl() ) );
        merged.setExclusions( mergeExclusions( mainDep.getExclusions(), parentDep.getExclusions() ) );

        return merged;
    }

    private static IssueManagement merge( IssueManagement mainIssueManagement, IssueManagement parentIssueManagement )
    {
        if ( parentIssueManagement == null )
        {
            return mainIssueManagement;
        }

        if ( mainIssueManagement == null )
        {
            return ArchivaModelCloner.clone( parentIssueManagement );
        }

        IssueManagement merged = new IssueManagement();

        merged.setSystem( merge( mainIssueManagement.getSystem(), parentIssueManagement.getSystem() ) );
        merged.setUrl( merge( mainIssueManagement.getUrl(), parentIssueManagement.getUrl() ) );
        merged.setIssueManagementUrl( merge( mainIssueManagement.getIssueManagementUrl(), parentIssueManagement.getIssueManagementUrl() ) );
        
        return merged;
    }

    private static Organization merge( Organization mainOrganization, Organization parentOrganization )
    {
        if ( parentOrganization == null )
        {
            return mainOrganization;
        }

        if ( mainOrganization == null )
        {
            return ArchivaModelCloner.clone( parentOrganization );
        }

        Organization merged = new Organization();

        merged.setFavicon( merge( mainOrganization.getFavicon(), parentOrganization.getFavicon() ) );
        merged.setOrganizationName( merge( mainOrganization.getOrganizationName(), parentOrganization.getOrganizationName() ) );
        merged.setName( merge( mainOrganization.getName(), parentOrganization.getName() ) );
        merged.setUrl( merge( mainOrganization.getUrl(), parentOrganization.getUrl() ) );

        return merged;
    }

    @SuppressWarnings("unchecked")
    private static Properties merge( Properties mainProperties, Properties parentProperties )
    {
        if ( parentProperties == null )
        {
            return mainProperties;
        }

        if ( mainProperties == null )
        {
            return ArchivaModelCloner.clone( parentProperties );
        }

        Properties merged = new Properties();
        merged.putAll(mainProperties);

        Enumeration<String> keys = (Enumeration<String>) parentProperties.propertyNames();
        while ( keys.hasMoreElements() )
        {
            String key = (String) keys.nextElement();
            String value = merge( mainProperties.getProperty( key ), parentProperties.getProperty( key ) );
            
            if ( value != null )
            {
                merged.put( key, value );
            }
        }

        return merged;
    }

    private static Scm merge( Scm mainScm, Scm parentScm )
    {
        if ( parentScm == null )
        {
            return mainScm;
        }

        if ( mainScm == null )
        {
            return ArchivaModelCloner.clone( parentScm );
        }

        Scm merged = new Scm();

        merged.setConnection( merge( mainScm.getConnection(), parentScm.getConnection() ) );
        merged.setDeveloperConnection( merge( mainScm.getDeveloperConnection(), parentScm.getDeveloperConnection() ) );
        merged.setUrl( merge( mainScm.getUrl(), parentScm.getUrl() ) );

        return merged;
    }

    private static String merge( String main, String parent )
    {
        if ( empty( main ) && !empty( parent ) )
        {
            return parent;
        }

        return main;
    }

    private static List<ArtifactReference> mergeArtifactReferences( List<ArtifactReference> mainArtifactReferences, List<ArtifactReference> parentArtifactReferences )
    {
        if ( parentArtifactReferences == null )
        {
            return mainArtifactReferences;
        }

        if ( mainArtifactReferences == null )
        {
            return ArchivaModelCloner.cloneArtifactReferences( parentArtifactReferences );
        }

        List<ArtifactReference> merged = new ArrayList<ArtifactReference>();

        Map<String, ArtifactReference> mainArtifactReferenceMap = createArtifactReferenceMap( mainArtifactReferences );
        Map<String, ArtifactReference> parentArtifactReferenceMap = createArtifactReferenceMap( parentArtifactReferences );

        for ( Map.Entry<String,ArtifactReference> entry : mainArtifactReferenceMap.entrySet() )
        {
            String key = entry.getKey();
            ArtifactReference mainArtifactReference = (ArtifactReference) entry.getValue();
            ArtifactReference parentArtifactReference = parentArtifactReferenceMap.get( key );

            if ( parentArtifactReference == null )
            {
                merged.add( mainArtifactReference );
            }
            else
            {
                // Not merging. Local wins.
                merged.add( merge( mainArtifactReference, parentArtifactReference ) );
            }
        }

        return merged;
    }

    private static List<Dependency> mergeDependencies( List<Dependency> mainDependencies, List<Dependency> parentDependencies )
    {
        if ( parentDependencies == null )
        {
            return mainDependencies;
        }

        if ( mainDependencies == null )
        {
            List<Dependency> merged = ArchivaModelCloner.cloneDependencies( parentDependencies );
            Iterator<Dependency> it = merged.iterator();
            while ( it.hasNext() )
            {
                Dependency dep = it.next();
                dep.setFromParent( true );
            }
            return merged;
        }

        List<Dependency> merged = new ArrayList<Dependency>();

        Map<String, Dependency> mainDepMap = createDependencyMap( mainDependencies );
        Map<String, Dependency> parentDepMap = createDependencyMap( parentDependencies );
        Set<String> uniqueKeys = new HashSet<String>();
        uniqueKeys.addAll( mainDepMap.keySet() );
        uniqueKeys.addAll( parentDepMap.keySet() );

        Iterator<String> it = uniqueKeys.iterator();
        while ( it.hasNext() )
        {
            String key = it.next();
            Dependency parentDep = parentDepMap.get( key );
            Dependency mainDep = mainDepMap.get( key );

            if ( parentDep == null )
            {
                // Means there is no parent dep to override main dep.
                merged.add( mainDep );
            }
            else
            {
                // Parent dep exists (main doesn't have to).
                // Merge the parent over the main dep.
                merged.add( merge( mainDep, parentDep ) );
            }
        }

        return merged;
    }

    private static List<Dependency> mergeDependencyManagement( List<Dependency> mainDepMgmt, List<Dependency> parentDepMgmt )
    {
        if ( parentDepMgmt == null )
        {
            return mainDepMgmt;
        }

        if ( mainDepMgmt == null )
        {
            List<Dependency> merged = ArchivaModelCloner.cloneDependencies( parentDepMgmt );
            Iterator<Dependency> it = merged.iterator();
            while ( it.hasNext() )
            {
                Dependency dep = it.next();
                dep.setFromParent( true );
            }
            return merged;
        }

        List<Dependency> merged = new ArrayList<Dependency>();

        Map<String, Dependency> mainDepMap = createDependencyMap( mainDepMgmt );
        Map<String, Dependency> parentDepMap = createDependencyMap( parentDepMgmt );
        Set<String> uniqueKeys = new HashSet<String>();
        uniqueKeys.addAll( mainDepMap.keySet() );
        uniqueKeys.addAll( parentDepMap.keySet() );

        Iterator<String> it = uniqueKeys.iterator();
        while ( it.hasNext() )
        {
            String key = it.next();
            Dependency parentDep = parentDepMap.get( key );
            Dependency mainDep = mainDepMap.get( key );

            if ( parentDep == null )
            {
                // Means there is no parent depMan entry to override main depMan.
                merged.add( mainDep );
            }
            else
            {
                // Parent depMan entry exists (main doesn't have to).
                // Merge the parent over the main depMan entry.
                merged.add( merge( mainDep, parentDep ) );
            }
        }

        return merged;
    }

    public static List<Exclusion> mergeExclusions( List<Exclusion> mainExclusions, List<Exclusion> parentExclusions )
    {
        if ( parentExclusions == null )
        {
            return mainExclusions;
        }

        if ( mainExclusions == null )
        {
            return ArchivaModelCloner.cloneExclusions( parentExclusions );
        }

        List<Exclusion> merged = new ArrayList<Exclusion>();

        Map<String, Exclusion> mainExclusionMap = createExclusionMap( mainExclusions );
        Map<String, Exclusion> parentExclusionMap = createExclusionMap( parentExclusions );

        for ( Map.Entry<String, Exclusion> entry : mainExclusionMap.entrySet() )
        {
            String key = entry.getKey();
            Exclusion mainExclusion = entry.getValue();
            Exclusion parentExclusion = parentExclusionMap.get( key );

            if ( parentExclusion == null )
            {
                merged.add( mainExclusion );
            }
            else
            {
                merged.add( parentExclusion );
            }
        }

        return merged;
    }

    private static List<Individual> mergeIndividuals( List<Individual> mainIndividuals, List<Individual> parentIndividuals )
    {
        if ( parentIndividuals == null )
        {
            return mainIndividuals;
        }

        if ( mainIndividuals == null )
        {
            return ArchivaModelCloner.cloneIndividuals( parentIndividuals );
        }

        List<Individual> merged = ArchivaModelCloner.cloneIndividuals( mainIndividuals );

        Iterator<Individual> it = parentIndividuals.iterator();
        while ( it.hasNext() )
        {
            Individual parentIndividual = it.next();

            if ( !mainIndividuals.contains( parentIndividual ) )
            {
                merged.add( parentIndividual );
            }
        }

        return merged;
    }

    private static List<License> mergeLicenses( List<License> mainLicenses, List<License> parentLicenses )
    {
        if ( parentLicenses == null )
        {
            return mainLicenses;
        }

        if ( mainLicenses == null )
        {
            return ArchivaModelCloner.cloneLicenses( parentLicenses );
        }

        List<License> merged = new ArrayList<License>();

        Map<String, License> mainLicensesMap = createLicensesMap( mainLicenses );
        Map<String, License> parentLicensesMap = createLicensesMap( parentLicenses );

        for ( Map.Entry<String, License> entry : mainLicensesMap.entrySet() )
        {
            String key = entry.getKey();
            License mainLicense = entry.getValue();
            License parentLicense = parentLicensesMap.get( key );

            if ( parentLicense == null )
            {
                merged.add( mainLicense );
            }
            else
            {
                // Not merging. Local wins.
                merged.add( parentLicense );
            }
        }

        return merged;
    }

    private static List<ArtifactReference> mergePlugins( List<ArtifactReference> mainPlugins, List<ArtifactReference> parentPlugins )
    {
        return mergeArtifactReferences( mainPlugins, parentPlugins );
    }

    private static List<ArtifactReference> mergeReports( List<ArtifactReference> mainReports, List<ArtifactReference> parentReports )
    {
        return mergeArtifactReferences( mainReports, parentReports );
    }

    private static List<ProjectRepository> mergeRepositories( List<ProjectRepository> mainRepositories, List<ProjectRepository> parentRepositories )
    {
        if ( parentRepositories == null )
        {
            return mainRepositories;
        }

        if ( mainRepositories == null )
        {
            return ArchivaModelCloner.cloneRepositories( parentRepositories );
        }

        List<ProjectRepository> merged = new ArrayList<ProjectRepository>();

        Map<String, ProjectRepository> mainRepositoriesMap = createRepositoriesMap( mainRepositories );
        Map<String, ProjectRepository> parentRepositoriesMap = createRepositoriesMap( parentRepositories );

        for ( Map.Entry<String, ProjectRepository> entry : mainRepositoriesMap.entrySet() )
        {
            String key = entry.getKey();
            ProjectRepository mainProjectRepository = entry.getValue();
            ProjectRepository parentProjectRepository = parentRepositoriesMap.get( key );

            if ( parentProjectRepository == null )
            {
                merged.add( mainProjectRepository );
            }
            else
            {
                // Not merging. Local wins.
                merged.add( parentProjectRepository );
            }
        }

        return merged;
    }

    private static String toVersionlessArtifactKey( ArtifactReference artifactReference )
    {
        StringBuffer key = new StringBuffer();

        key.append( artifactReference.getGroupId() ).append( ":" ).append( artifactReference.getArtifactId() );
        key.append( StringUtils.defaultString( artifactReference.getClassifier() ) ).append( ":" );
        key.append( artifactReference.getType() );

        return key.toString();
    }

    private static String toVersionlessDependencyKey( Dependency dep )
    {
        StringBuffer key = new StringBuffer();

        key.append( dep.getGroupId() ).append( ":" ).append( dep.getArtifactId() );
        key.append( StringUtils.defaultString( dep.getClassifier() ) ).append( ":" );
        key.append( dep.getType() );

        return key.toString();
    }
}
