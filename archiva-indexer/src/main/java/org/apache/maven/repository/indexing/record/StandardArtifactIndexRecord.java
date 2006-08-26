package org.apache.maven.repository.indexing.record;

import java.util.List;

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

/**
 * The a record with the fields in the standard index.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class StandardArtifactIndexRecord
    extends MinimalArtifactIndexRecord
{
    /**
     * The SHA-1 checksum of the artifact file.
     */
    private String sha1Checksum;

    /**
     * The artifact's group.
     */
    private String groupId;

    /**
     * The artifact's identifier within the group.
     */
    private String artifactId;

    /**
     * The artifact's version.
     */
    private String version;

    /**
     * The classifier, if there is one.
     */
    private String classifier;

    /**
     * The artifact type (from the file).
     */
    private String type;

    /**
     * A list of files (separated by '\n') in the artifact if it is an archive.
     */
    private List files;

    /**
     * The identifier of the repository that the artifact came from.
     */
    private String repository;

    /**
     * The packaging specified in the POM for this artifact.
     */
    private String packaging;

    /**
     * The plugin prefix specified in the metadata if the artifact is a plugin.
     */
    private String pluginPrefix;

    /**
     * The year the project was started.
     */
    private String inceptionYear;

    /**
     * The description of the project.
     */
    private String projectDescription;

    /**
     * The name of the project.
     */
    private String projectName;

    /**
     * The base version (before the snapshot is determined).
     */
    private String baseVersion;

    public void setSha1Checksum( String sha1Checksum )
    {
        this.sha1Checksum = sha1Checksum;
    }

    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    public void setClassifier( String classifier )
    {
        this.classifier = classifier;
    }

    public void setType( String type )
    {
        this.type = type;
    }

    public void setFiles( List files )
    {
        this.files = files;
    }

    public void setRepository( String repository )
    {
        this.repository = repository;
    }

    /**
     * @noinspection RedundantIfStatement
     */
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null || getClass() != obj.getClass() )
        {
            return false;
        }
        if ( !super.equals( obj ) )
        {
            return false;
        }

        StandardArtifactIndexRecord that = (StandardArtifactIndexRecord) obj;

        if ( !artifactId.equals( that.artifactId ) )
        {
            return false;
        }
        if ( classifier != null ? !classifier.equals( that.classifier ) : that.classifier != null )
        {
            return false;
        }
        if ( files != null ? !files.equals( that.files ) : that.files != null )
        {
            return false;
        }
        if ( !groupId.equals( that.groupId ) )
        {
            return false;
        }
        if ( repository != null ? !repository.equals( that.repository ) : that.repository != null )
        {
            return false;
        }
        if ( sha1Checksum != null ? !sha1Checksum.equals( that.sha1Checksum ) : that.sha1Checksum != null )
        {
            return false;
        }
        if ( type != null ? !type.equals( that.type ) : that.type != null )
        {
            return false;
        }
        if ( !version.equals( that.version ) )
        {
            return false;
        }
        if ( !baseVersion.equals( that.baseVersion ) )
        {
            return false;
        }
        if ( packaging != null ? !packaging.equals( that.packaging ) : that.packaging != null )
        {
            return false;
        }
        if ( pluginPrefix != null ? !pluginPrefix.equals( that.pluginPrefix ) : that.pluginPrefix != null )
        {
            return false;
        }
        if ( projectName != null ? !projectName.equals( that.projectName ) : that.projectName != null )
        {
            return false;
        }
        if ( inceptionYear != null ? !inceptionYear.equals( that.inceptionYear ) : that.inceptionYear != null )
        {
            return false;
        }
        if ( projectDescription != null ? !projectDescription.equals( that.projectDescription )
            : that.projectDescription != null )
        {
            return false;
        }

        return true;
    }

    public int hashCode()
    {
        int result = super.hashCode();
        result = 31 * result + ( sha1Checksum != null ? sha1Checksum.hashCode() : 0 );
        result = 31 * result + groupId.hashCode();
        result = 31 * result + artifactId.hashCode();
        result = 31 * result + version.hashCode();
        result = 31 * result + baseVersion.hashCode();
        result = 31 * result + ( classifier != null ? classifier.hashCode() : 0 );
        result = 31 * result + ( type != null ? type.hashCode() : 0 );
        result = 31 * result + ( files != null ? files.hashCode() : 0 );
        result = 31 * result + ( repository != null ? repository.hashCode() : 0 );
        result = 31 * result + ( packaging != null ? packaging.hashCode() : 0 );
        result = 31 * result + ( pluginPrefix != null ? pluginPrefix.hashCode() : 0 );
        result = 31 * result + ( inceptionYear != null ? inceptionYear.hashCode() : 0 );
        result = 31 * result + ( projectName != null ? projectName.hashCode() : 0 );
        result = 31 * result + ( projectDescription != null ? projectDescription.hashCode() : 0 );
        return result;
    }

    public String getSha1Checksum()
    {
        return sha1Checksum;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public String getVersion()
    {
        return version;
    }

    public String getClassifier()
    {
        return classifier;
    }

    public String getType()
    {
        return type;
    }

    public List getFiles()
    {
        return files;
    }

    public String getRepository()
    {
        return repository;
    }

    public String getPackaging()
    {
        return packaging;
    }

    public String getPluginPrefix()
    {
        return pluginPrefix;
    }

    public void setPackaging( String packaging )
    {
        this.packaging = packaging;
    }

    public void setPluginPrefix( String pluginPrefix )
    {
        this.pluginPrefix = pluginPrefix;
    }

    public void setInceptionYear( String inceptionYear )
    {
        this.inceptionYear = inceptionYear;
    }

    public void setProjectDescription( String description )
    {
        this.projectDescription = description;
    }

    public void setProjectName( String projectName )
    {
        this.projectName = projectName;
    }

    public String getInceptionYear()
    {
        return inceptionYear;
    }

    public String getProjectDescription()
    {
        return projectDescription;
    }

    public String getProjectName()
    {
        return projectName;
    }

    public void setBaseVersion( String baseVersion )
    {
        this.baseVersion = baseVersion;
    }

    public String getBaseVersion()
    {
        return baseVersion;
    }
}
