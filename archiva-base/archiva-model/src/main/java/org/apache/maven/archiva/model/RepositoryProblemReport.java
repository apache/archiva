package org.apache.maven.archiva.model;

public class RepositoryProblemReport extends RepositoryProblem
{
    protected String groupURL;

    protected String artifactURL;

    protected String versionURL;

    public RepositoryProblemReport( RepositoryProblem repositoryProblem )
    {
        setGroupId( repositoryProblem.getGroupId() );
        setArtifactId( repositoryProblem.getArtifactId() );
        setVersion( repositoryProblem.getVersion() );
        setMessage( repositoryProblem.getMessage() );
        setOrigin( repositoryProblem.getOrigin() );
        setPath( repositoryProblem.getPath() );
        setType( repositoryProblem.getType() );
    }

    public void setGroupURL( String groupURL )
    {
        this.groupURL = groupURL;
    }

    public String getGroupURL()
    {
        return groupURL; 
    }

    public void setArtifactURL( String artifactURL )
    {
        this.artifactURL = artifactURL;
    }

    public String getArtifactURL()
    {
        return artifactURL; 
    }

    public void setVersionURL( String versionURL )
    {
        this.versionURL = versionURL;
    }

    public String getVersionURL()
    {
        return versionURL; 
    }
}
