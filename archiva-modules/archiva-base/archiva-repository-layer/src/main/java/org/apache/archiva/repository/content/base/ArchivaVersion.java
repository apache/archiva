package org.apache.archiva.repository.content.base;

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

import org.apache.archiva.repository.content.Project;
import org.apache.archiva.repository.content.Version;
import org.apache.archiva.repository.content.base.builder.VersionOptBuilder;
import org.apache.archiva.repository.content.base.builder.WithProjectBuilder;
import org.apache.archiva.repository.content.base.builder.WithVersionBuilder;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.regex.PatternSyntaxException;

/**
 * Immutable version instance.
 * <p>
 * You have to use the builder method to create instances of this version object.
 * <p>
 * The project and the version string are required attributes of this instance additional to the base
 * attributes repository and asset.
 * <p>
 * Two instances are equal, if the project and the version match in addition to the base attributes repository and asset.
 */
public class ArchivaVersion extends ArchivaContentItem implements Version
{

    private String version;
    private Project project;
    private String separatorExpression = "\\.";
    private List<String> versionSegments;

    private ArchivaVersion( )
    {

    }

    /**
     * Creates a new builder for creating new version instances. You have to provide the required
     * attributes before the build() method can be called.
     *
     * @param storageAsset the storage asset
     * @return the builder for creating new version instances
     */
    public static WithProjectBuilder withAsset( StorageAsset storageAsset )
    {
        return new Builder( ).withAsset( storageAsset );
    }

    @Override
    public List<String> getVersionSegments( )
    {
        return versionSegments;
    }

    @Override
    public String getVersion( )
    {
        return version;
    }

    @Override
    public Project getProject( )
    {
        return project;
    }


    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( o == null || getClass( ) != o.getClass( ) ) return false;
        if ( !super.equals( o ) ) return false;

        ArchivaVersion that = (ArchivaVersion) o;

        if ( !version.equals( that.version ) ) return false;
        return project.equals( that.project );
    }

    @Override
    public int hashCode( )
    {
        int result = super.hashCode( );
        result = 31 * result + version.hashCode( );
        result = 31 * result + project.hashCode( );
        return result;
    }

    @Override
    public String toString( )
    {
        return version+", project="+project.toString();
    }

    private static final class Builder extends ContentItemBuilder<ArchivaVersion, VersionOptBuilder, WithProjectBuilder>
        implements WithProjectBuilder, WithVersionBuilder, VersionOptBuilder
    {

        Builder( )
        {
            super( new ArchivaVersion( ) );
        }

        @Override
        protected WithProjectBuilder getNextBuilder( )
        {
            return this;
        }

        @Override
        protected VersionOptBuilder getOptBuilder( )
        {
            return this;
        }

        @Override
        public VersionOptBuilder withVersion( String version )
        {
            if ( StringUtils.isEmpty( version ) )
            {
                throw new IllegalArgumentException( "Version parameter must not be empty or null." );
            }
            item.version = version;
            updateVersionSegments( );
            return this;
        }


        private void updateVersionSegments( )
        {
            item.versionSegments = Arrays.asList( item.version.split( item.separatorExpression ) );
        }

        @Override
        public WithVersionBuilder withProject( Project project )
        {
            if ( project == null )
            {
                throw new IllegalArgumentException( "Project may not be null" );
            }
            item.project = project;
            super.setRepository( project.getRepository( ) );
            return this;
        }

        @Override
        public ArchivaVersion build( )
        {
            super.build( );
            return item;
        }

        @Override
        public VersionOptBuilder withSeparatorExpression( String expression )
        {
            if ( StringUtils.isEmpty( expression ) )
            {
                throw new IllegalArgumentException( "Separator expression may not be null or empty" );
            }
            this.item.separatorExpression = expression;
            try
            {
                updateVersionSegments( );
            }
            catch ( PatternSyntaxException e )
            {
                throw new IllegalArgumentException( "Bad separator expression " + expression + ": " + e.getMessage( ), e );
            }
            return this;
        }
    }

}
