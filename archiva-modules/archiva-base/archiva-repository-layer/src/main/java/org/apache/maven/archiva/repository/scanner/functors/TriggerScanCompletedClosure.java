/*
 *  Copyright 2008 jdumay.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package org.apache.maven.archiva.repository.scanner.functors;

import org.apache.commons.collections.Closure;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.consumers.RepositoryContentConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TriggerScanCompletedClosure implements Closure
{
    private Logger log = LoggerFactory.getLogger( TriggerScanCompletedClosure.class );

    private final ManagedRepositoryConfiguration repository;

    public TriggerScanCompletedClosure(ManagedRepositoryConfiguration repository)
    {
        this.repository = repository;
    }

    public void execute(Object input)
    {
        if ( input instanceof RepositoryContentConsumer )
        {
            RepositoryContentConsumer consumer = (RepositoryContentConsumer) input;
            consumer.completeScan();
            log.info( "Consumer [" + consumer.getId() + "] completed for repository [" + repository.getId() + "]");
        }
    }
}
