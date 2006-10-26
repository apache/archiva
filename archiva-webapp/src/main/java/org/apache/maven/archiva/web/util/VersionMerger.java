package org.apache.maven.archiva.web.util;

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

import org.apache.maven.archiva.indexer.record.StandardArtifactIndexRecord;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.model.Dependency;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VersionMerger {

    public static List /*<DependencyWrapper>*/ wrap(List /*<StandardArtifactIndexRecord>*/ artifacts) 
    {
        List dependencies = new ArrayList();

        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Dependency dependency = (Dependency) i.next();

            dependencies.add( new DependencyWrapper( dependency ) );
        }

        return dependencies;
    }

    public static Collection /*<DependencyWrapper*/ merge(Collection /*<StandardArtifactIndexRecord>*/ artifacts) 
    {
        Map dependees = new LinkedHashMap();

        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            StandardArtifactIndexRecord record = (StandardArtifactIndexRecord) i.next();

            String key = record.getGroupId() + ":" + record.getArtifactId();
            if ( dependees.containsKey( key ) )
            {
                DependencyWrapper wrapper = (DependencyWrapper) dependees.get( key );
                wrapper.addVersion( record.getVersion() );
            }
            else
            {
                DependencyWrapper wrapper = new DependencyWrapper( record );

                dependees.put( key, wrapper );
            }
        }

        return dependees.values();
    }

    public static class DependencyWrapper
    {
        private final String groupId;

        private final String artifactId;

        /**
         * Versions added. We ignore duplicates since you might add those with varying classifiers.
         */
        private Set versions = new HashSet();

        private String version;

        private String scope;

        private String classifier;

        public DependencyWrapper( StandardArtifactIndexRecord record )
        {
            this.groupId = record.getGroupId();

            this.artifactId = record.getArtifactId();

            addVersion( record.getVersion() );
        }

        public DependencyWrapper( Dependency dependency )
        {
            this.groupId = dependency.getGroupId();

            this.artifactId = dependency.getArtifactId();

            this.scope = dependency.getScope();

            this.classifier = dependency.getClassifier();

            addVersion( dependency.getVersion() );
        }

        public String getScope()
        {
            return scope;
        }

        public String getClassifier()
        {
            return classifier;
        }

        public void addVersion( String version )
        {
            // We use DefaultArtifactVersion to get the correct sorting order later, however it does not have
            // hashCode properly implemented, so we add it here.
            // TODO: add these methods to the actual DefaultArtifactVersion and use that.
            versions.add( new DefaultArtifactVersion( version )
            {
                public int hashCode()
                {
                    int result;
                    result = getBuildNumber();
                    result = 31 * result + getMajorVersion();
                    result = 31 * result + getMinorVersion();
                    result = 31 * result + getIncrementalVersion();
                    result = 31 * result + ( getQualifier() != null ? getQualifier().hashCode() : 0 );
                    return result;
                }

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

                    DefaultArtifactVersion that = (DefaultArtifactVersion) o;

                    if ( getBuildNumber() != that.getBuildNumber() )
                    {
                        return false;
                    }
                    if ( getIncrementalVersion() != that.getIncrementalVersion() )
                    {
                        return false;
                    }
                    if ( getMajorVersion() != that.getMajorVersion() )
                    {
                        return false;
                    }
                    if ( getMinorVersion() != that.getMinorVersion() )
                    {
                        return false;
                    }
                    if ( getQualifier() != null ? !getQualifier().equals( that.getQualifier() )
                        : that.getQualifier() != null )
                    {
                        return false;
                    }

                    return true;
                }
            } );

            if ( versions.size() == 1 )
            {
                this.version = version;
            }
            else
            {
                this.version = null;
            }
        }

        public String getGroupId()
        {
            return groupId;
        }

        public String getArtifactId()
        {
            return artifactId;
        }

        public List getVersions()
        {
            List versions = new ArrayList( this.versions );
            Collections.sort( versions );
            return versions;
        }

        public String getVersion()
        {
            return version;
        }
    }
}
