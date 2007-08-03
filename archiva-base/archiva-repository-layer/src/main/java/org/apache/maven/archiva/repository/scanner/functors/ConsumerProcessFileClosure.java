package org.apache.maven.archiva.repository.scanner.functors;

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
import org.apache.maven.archiva.common.utils.BaseFile;
import org.apache.maven.archiva.consumers.RepositoryContentConsumer;
import org.codehaus.plexus.logging.Logger;

/**
 * ConsumerProcessFileClosure 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class ConsumerProcessFileClosure
    implements Closure
{
    private BaseFile basefile;

    private Logger logger;

    public ConsumerProcessFileClosure( Logger logger )
    {
        // Lame. I know, but seeing as plexus doesn't like to cleanup after 
        // application loaded/lookup'd components, this is the best I can do.
        this.logger = logger;
    }

    public void execute( Object input )
    {
        if ( input instanceof RepositoryContentConsumer )
        {
            RepositoryContentConsumer consumer = (RepositoryContentConsumer) input;

            try
            {
                logger.debug( "Sending to consumer: " + consumer.getId() );

                consumer.processFile( basefile.getRelativePath() );
            }
            catch ( Exception e )
            {
                /* Intentionally Catch all exceptions.
                 * So that the discoverer processing can continue.
                 */
                logger.error( "Consumer [" + consumer.getId() + "] had an error when processing file ["
                    + basefile.getAbsolutePath() + "]: " + e.getMessage(), e );
            }
        }
    }

    public BaseFile getBasefile()
    {
        return basefile;
    }

    public void setBasefile( BaseFile basefile )
    {
        this.basefile = basefile;
    }

    public Logger getLogger()
    {
        return logger;
    }

    public void setLogger( Logger logger )
    {
        this.logger = logger;
    }
}
