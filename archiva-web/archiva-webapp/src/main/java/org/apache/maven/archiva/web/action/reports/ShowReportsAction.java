package org.apache.maven.archiva.web.action.reports;

import java.util.Collection;

import org.apache.maven.archiva.database.ArchivaDAO;
import org.codehaus.plexus.xwork.action.PlexusActionSupport;

/**
 * Show reports.
 * 
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="showReportsAction"
 */
public class ShowReportsAction extends PlexusActionSupport
{
    /**
     * @plexus.requirement role-hint="jdo"
     */
    protected ArchivaDAO dao;

    private Collection groupIds;

    private Collection artifactIds;

    private Collection repositoryIds;

    public String execute() throws Exception
    {
        /*
         * TODO Use combo box for groupIds, artifactIds and repositoryIds instead of a text field.
         */

        return SUCCESS;
    }

    public Collection getGroupIds()
    {
        return groupIds;
    }

    public Collection getArtifactIds()
    {
        return artifactIds;
    }

    public Collection getRepositoryIds()
    {
        return repositoryIds;
    }
}
