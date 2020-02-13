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
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.commons.lang3.StringUtils;

public class ArchivaVersion extends ArchivaContentItem implements Version
{

    private String version;
    private StorageAsset asset;
    private Project project;

    private ArchivaVersion() {

    }

    public static ProjectBuilder withVersion(String version) {
        return new Builder( ).withVersion( version );
    }

    @Override
    public String getVersion( )
    {
        return version;
    }

    @Override
    public StorageAsset getAsset( )
    {
        return asset;
    }

    @Override
    public Project getProject( )
    {
        return project;
    }

    public interface ProjectBuilder {
        Builder withProject( Project project );
    }

    public static final class Builder implements ProjectBuilder {

        private ArchivaVersion version  = new ArchivaVersion();

        ProjectBuilder withVersion( String version )
        {
            if ( StringUtils.isEmpty( version ) ) {
                throw new IllegalArgumentException( "Version parameter must not be empty or null." );
            }
            this.version.version = version;
            return this;
        }


        @Override
        public Builder withProject( Project project )
        {
            this.version.project = project;
            return this;
        }

        public Builder withAsset( StorageAsset asset )
        {
            this.version.asset = asset;
            return this;
        }

        public Builder withAttribute(String key, String value) {
            this.version.putAttribute( key, value );
            return this;
        }

        public ArchivaVersion build() {
            if (this.version.asset == null) {
                this.version.project.getAsset( );
            }
            return this.version;
        }
    }

}
