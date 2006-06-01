package org.apache.maven.repository.manager.web.action;

import com.opensymphony.xwork.Action;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.repository.discovery.ArtifactDiscoverer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

/**
 * TODO: Description.
 *
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="org.apache.maven.repository.manager.web.action.RepositoryBrowseAction"
 */
public class RepositoryBrowseAction
    implements Action
{
    /**
     * @plexus.requirement role-hint="org.apache.maven.repository.discovery.DefaultArtifactDiscoverer"
     */
    private ArtifactDiscoverer discoverer;

    /**
     * @plexus.requirement
     */
    private ArtifactRepositoryFactory repositoryFactory;

    /**
     * @plexus.requirement role-hint="default"
     */
    private ArtifactRepositoryLayout layout;

    private String group;

    private TreeMap artifactMap;

    private String folder;

    private int idx;

    public String execute()
        throws Exception
    {
        String path = "E:/jeprox/maven-repository-manager/trunk/maven-repository-discovery/src/test/repository";

        ArtifactRepository repository =
            repositoryFactory.createArtifactRepository( "discoveryRepo", "file://" + path, layout, null, null );

        List artifacts = discoverer.discoverArtifacts( repository, null, true );

        Iterator iterator = artifacts.iterator();

        artifactMap = new TreeMap();

        String groupId;

        while ( iterator.hasNext() )
        {
            Artifact artifact = (Artifact) iterator.next();

            groupId = artifact.getGroupId();

            String key = groupId.replace( '.', '/' ) + "/" + artifact.getArtifactId() + "/" + artifact.getVersion();

            ArrayList artifactList;

            if ( artifactMap.containsKey( key ) )
            {
                artifactList = (ArrayList) artifactMap.get( key );
            }
            else
            {
                artifactList = new ArrayList();
            }

            artifactList.add( artifact );

            Collections.sort( artifactList );

            artifactMap.put( key, artifactList );
        }

        //set the index for folder level to be displayed
        setIdx( 1 );

        setFolder( "" );

        return SUCCESS;
    }

    public String doEdit()
        throws Exception
    {
        setIdx( getIdx() + 1 );

        //set folder to "" if we are at the root directory
        if ( getIdx() == 1 )
        {
            setFolder( "" );
        }

        return SUCCESS;
    }

    public TreeMap getArtifactMap()
    {
        return artifactMap;
    }

    public String getGroup()
    {
        return group;
    }

    public void setGroup( String group )
    {
        this.group = group;
    }

    public String getFolder()
    {
        return folder;
    }

    public void setFolder( String folder )
    {
        this.folder = folder;
    }

    public int getIdx()
    {
        return idx;
    }

    public void setIdx( int index )
    {
        this.idx = index;
    }

}
