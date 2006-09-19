package org.apache.maven.archiva.web.servlet.repository;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
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

import org.apache.maven.archiva.configuration.RepositoryConfiguration;
import org.codehaus.plexus.logging.Logger;

import java.io.File;
import java.io.IOException;

import it.could.webdav.DAVListener;
import it.could.webdav.DAVProcessor;
import it.could.webdav.DAVRepository;
import it.could.webdav.DAVResource;

/**
 * RepositoryMapping 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class RepositoryMapping implements DAVListener
{
    private RepositoryConfiguration repositoryConfiguration;
    private DAVProcessor davProcessor;
    private DAVRepository davRepository;
    private Logger logger;
    
    public RepositoryMapping(RepositoryConfiguration repoConfig) throws IOException
    {
        this.repositoryConfiguration = repoConfig;
        File repoDir = new File(repositoryConfiguration.getDirectory());
        this.davRepository = new DAVRepository( repoDir );
        this.davProcessor = new DAVProcessor(this.davRepository);
        this.davRepository.addListener(this);
    }
    
    public DAVProcessor getDavProcessor()
    {
        return davProcessor;
    }

    /**
     * <p>Receive notification of an event occurred in a specific
     * {@link DAVRepository}.</p>
     */
    public void notify(DAVResource resource, int event) {
        String message = "Unknown event";
        switch (event) {
            case DAVListener.COLLECTION_CREATED:
                message = "Collection created";
                break;
            case DAVListener.COLLECTION_REMOVED:
                message = "Collection removed";
                break;
            case DAVListener.RESOURCE_CREATED:
                message = "Resource created";
                break;
            case DAVListener.RESOURCE_REMOVED:
                message = "Resource removed";
                break;
            case DAVListener.RESOURCE_MODIFIED:
                message = "Resource modified";
                break;
        }
        logger.info(message + ": " + this.repositoryConfiguration.getId() + " : \"" + resource.getRelativePath() + "\"");
    }
}
