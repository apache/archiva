package org.apache.maven.archiva.database.updater;

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

import org.apache.commons.collections.Closure;
import org.apache.maven.archiva.consumers.ArchivaArtifactConsumer;
import org.apache.maven.archiva.consumers.ConsumerException;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.codehaus.plexus.logging.AbstractLogEnabled;

/**
 * ProcessArchivaArtifactClosure 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.commons.collections.Closure" 
 *                   role-hint="process-artifact"
 *                   instantiation-strategy="per-lookup"
 */
class ProcessArchivaArtifactClosure
    extends AbstractLogEnabled
    implements Closure
{
    private ArchivaArtifact artifact;

    public void execute( Object input )
    {
        if ( input instanceof ArchivaArtifactConsumer )
        {
            ArchivaArtifactConsumer consumer = (ArchivaArtifactConsumer) input;

            try
            {
                consumer.processArchivaArtifact( artifact );
            }
            catch ( ConsumerException e )
            {
                getLogger().warn(
                                  "Unable to process artifact [" + artifact + "] with consumer [" + consumer.getId()
                                      + "]" );
            }
        }

    }

    public ArchivaArtifact getArtifact()
    {
        return artifact;
    }

    public void setArtifact( ArchivaArtifact artifact )
    {
        this.artifact = artifact;
    }
}
