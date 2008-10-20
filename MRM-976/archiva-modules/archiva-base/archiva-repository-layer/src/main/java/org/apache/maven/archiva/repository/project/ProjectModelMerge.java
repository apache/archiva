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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

/**
 * ProjectModelMerge
 *
 * TODO: Should call this ProjectModelAncestry as it deals with the current project and its parent.
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
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

    private static Map createArtifactReferenceMap( List artifactReferences )
    {
        Map ret = new HashMap();

        Iterator it = artifactReferences.iterator();
        while ( it.hasNext() )
        {
            ArtifactReference artifactReference = (ArtifactReference) it.next();
            String key = toVersionlessArtifactKey( artifactReference );
            ret.put( key, artifactReference );
        }

        return ret;
    }

    private static Map createDependencyMap( List dependencies )
    {
        Map ret = new HashMap();

        Iterator it = dependencies.iterator();
        while ( it.hasNext() )
        {
            Dependency dep = (Dependency) it.next();
            String key = toVersionlessDependencyKey( dep );
            ret.put( key, dep );
        }

        return ret;
    }

    private static Map createExclusionMap( List exclusions )
    {
        Map ret = new HashMap();

        Iterator it = exclusions.iterator();
        while ( it.hasNext() )
        {
            Exclusion exclusion = (Exclusion) it.next();
            String key = exclusion.getGroupId() + ":" + exclusion.getArtifactId();
            ret.put( key, exclusion );
        }

        return ret;
    }

    private static Map createLicensesMap( List licenses )
    {
        Map ret = new HashMap();

        Iterator it = licenses.iterator();
        while ( it.hasNext() )
        {
            License license = (License) it.next();
            // TODO: Change to 'id' when LicenseTypeMapper is created.
            String key = license.getName();
            ret.put( key, license );
        }

        return ret;
    }

    private static Map createRepositoriesMap( List repositories )
    {
        Map ret = new HashMap();

        Iterator it = repositories.iterator();
        while ( it.hasNext() )
        {
            ProjectRepository repo = (ProjectRepository) it.next();
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
        merged.setName( merge( mainOrganization.getName(), parentOrganization.getName() ) );
        merged.setUrl( merge( mainOrganization.getUrl(), parentOrganization.getUrl() ) );

        return merged;
    }

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

        Enumeration keys = parentProperties.propertyNames();
        while ( keys.hasMoreElements() )
        {
            String key = (String) keys.nextElement();
            merged.put( key, merge( mainProperties.getProperty( key ), parentProperties.getProperty( key ) ) );
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

    private static List mergeArtifactReferences( List mainArtifactReferences, List parentArtifactReferences )
    {
        if ( parentArtifactReferences == null )
        {
            return mainArtifactReferences;
        }

        if ( mainArtifactReferences == null )
        {
            return ArchivaModelCloner.cloneLicenses( parentArtifactReferences );
        }

        List merged = new ArrayList();

        Map mainArtifactReferenceMap = createArtifactReferenceMap( mainArtifactReferences );
        Map parentArtifactReferenceMap = createArtifactReferenceMap( parentArtifactReferences );

        Iterator it = mainArtifactReferenceMap.entrySet().iterator();
        while ( it.hasNext() )
        {
            Map.Entry entry = (Entry) it.next();
            String key = (String) entry.getKey();
            ArtifactReference mainArtifactReference = (ArtifactReference) entry.getValue();
            ArtifactReference parentArtifactReference = (ArtifactReference) parentArtifactReferenceMap.get( key );

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

    private static List mergeDependencies( List mainDependencies, List parentDependencies )
    {
        if ( parentDependencies == null )
        {
            return mainDependencies;
        }

        if ( mainDependencies == null )
        {
            List merged = ArchivaModelCloner.cloneDependencies( parentDependencies );
            Iterator it = merged.iterator();
            while ( it.hasNext() )
            {
                Dependency dep = (Dependency) it.next();
                dep.setFromParent( true );
            }
            return merged;
        }

        List merged = new ArrayList();

        Map mainDepMap = createDependencyMap( mainDependencies );
        Map parentDepMap = createDependencyMap( parentDependencies );
        Set uniqueKeys = new HashSet();
        uniqueKeys.addAll( mainDepMap.keySet() );
        uniqueKeys.addAll( parentDepMap.keySet() );

        Iterator it = uniqueKeys.iterator();
        while ( it.hasNext() )
        {
            String key = (String) it.next();
            Dependency parentDep = (Dependency) parentDepMap.get( key );
            Dependency mainDep = (Dependency) mainDepMap.get( key );

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

    private static List mergeDependencyManagement( List mainDepMgmt, List parentDepMgmt )
    {
        if ( parentDepMgmt == null )
        {
            return mainDepMgmt;
        }

        if ( mainDepMgmt == null )
        {
            List merged = ArchivaModelCloner.cloneDependencies( parentDepMgmt );
            Iterator it = merged.iterator();
            while ( it.hasNext() )
            {
                Dependency dep = (Dependency) it.next();
                dep.setFromParent( true );
            }
            return merged;
        }

        List merged = new ArrayList();

        Map mainDepMap = createDependencyMap( mainDepMgmt );
        Map parentDepMap = createDependencyMap( parentDepMgmt );
        Set uniqueKeys = new HashSet();
        uniqueKeys.addAll( mainDepMap.keySet() );
        uniqueKeys.addAll( parentDepMap.keySet() );

        Iterator it = uniqueKeys.iterator();
        while ( it.hasNext() )
        {
            String key = (String) it.next();
            Dependency parentDep = (Dependency) parentDepMap.get( key );
            Dependency mainDep = (Dependency) mainDepMap.get( key );

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

    public static List mergeExclusions( List mainExclusions, List parentExclusions )
    {
        if ( parentExclusions == null )
        {
            return mainExclusions;
        }

        if ( mainExclusions == null )
        {
            return ArchivaModelCloner.cloneExclusions( parentExclusions );
        }

        List merged = new ArrayList();

        Map mainExclusionMap = createExclusionMap( mainExclusions );
        Map parentExclusionMap = createExclusionMap( parentExclusions );

        Iterator it = mainExclusionMap.entrySet().iterator();
        while ( it.hasNext() )
        {
            Map.Entry entry = (Entry) it.next();
            String key = (String) entry.getKey();
            Exclusion mainExclusion = (Exclusion) entry.getValue();
            Exclusion parentExclusion = (Exclusion) parentExclusionMap.get( key );

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

    private static List mergeIndividuals( List mainIndividuals, List parentIndividuals )
    {
        if ( parentIndividuals == null )
        {
            return mainIndividuals;
        }

        if ( mainIndividuals == null )
        {
            return ArchivaModelCloner.cloneIndividuals( parentIndividuals );
        }

        List merged = ArchivaModelCloner.cloneIndividuals( mainIndividuals );

        Iterator it = parentIndividuals.iterator();
        while ( it.hasNext() )
        {
            Individual parentIndividual = (Individual) it.next();

            if ( !mainIndividuals.contains( parentIndividual ) )
            {
                merged.add( parentIndividual );
            }
        }

        return merged;
    }

    private static List mergeLicenses( List mainLicenses, List parentLicenses )
    {
        if ( parentLicenses == null )
        {
            return mainLicenses;
        }

        if ( mainLicenses == null )
        {
            return ArchivaModelCloner.cloneLicenses( parentLicenses );
        }

        List merged = new ArrayList();

        Map mainLicensesMap = createLicensesMap( mainLicenses );
        Map parentLicensesMap = createLicensesMap( parentLicenses );

        Iterator it = mainLicensesMap.entrySet().iterator();
        while ( it.hasNext() )
        {
            Map.Entry entry = (Entry) it.next();
            String key = (String) entry.getKey();
            License mainLicense = (License) entry.getValue();
            License parentLicense = (License) parentLicensesMap.get( key );

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

    private static List mergePlugins( List mainPlugins, List parentPlugins )
    {
        return mergeArtifactReferences( mainPlugins, parentPlugins );
    }

    private static List mergeReports( List mainReports, List parentReports )
    {
        return mergeArtifactReferences( mainReports, parentReports );
    }

    private static List mergeRepositories( List mainRepositories, List parentRepositories )
    {
        if ( parentRepositories == null )
        {
            return mainRepositories;
        }

        if ( mainRepositories == null )
        {
            return ArchivaModelCloner.cloneLicenses( parentRepositories );
        }

        List merged = new ArrayList();

        Map mainRepositoriesMap = createRepositoriesMap( mainRepositories );
        Map parentRepositoriesMap = createRepositoriesMap( parentRepositories );

        Iterator it = mainRepositoriesMap.entrySet().iterator();
        while ( it.hasNext() )
        {
            Map.Entry entry = (Entry) it.next();
            String key = (String) entry.getKey();
            ProjectRepository mainProjectRepository = (ProjectRepository) entry.getValue();
            ProjectRepository parentProjectRepository = (ProjectRepository) parentRepositoriesMap.get( key );

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
