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

package org.apache.archiva.consumers;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.maven.archiva.consumers.ConsumerException;
import org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.content.ManagedDefaultRepositoryContent;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.selectors.FilenameSelector;
import org.osgi.impl.bundle.bindex.ant.BindexTask;

/**
 *
 * @author jdumay
 */
public class OBRRepositoryConsumer 
        extends AbstractMonitoredConsumer
        implements KnownRepositoryContentConsumer
{
    private ManagedRepositoryContent content;

    public String getDescription() {
        return "Produces the OSGi OBR repository index";
    }

    public String getId() {
        return "create-obr-repositoryxml";
    }

    public boolean isPermanent() {
        return false;
    }

    public void beginScan(ManagedRepositoryConfiguration repository, Date whenGathered) throws ConsumerException {
        content = new ManagedDefaultRepositoryContent();
        content.setRepository(repository);
    }

    public void completeScan() {
        /** do nothing **/
    }

    public List<String> getExcludes() {
        return null;
    }

    public List<String> getIncludes() {
        return Arrays.asList("**/*.jar");
    }

    public void processFile(String path)
        throws ConsumerException
    {
        BindexTask task = new BindexTask();
        File repositoryIndexFile = new File(new File(path).getParentFile(), ".repository.xml");
        task.setRepositoryFile(repositoryIndexFile);
        task.setName(content.getRepository().getName());
        task.setQuiet(false);
        task.setRoot(new File(content.getRepoRoot()));

        FileSet fileSet = new FileSet();
        fileSet.setDir(new File(path).getParentFile());
        fileSet.setIncludes("**/*.jar");
        try
        {
            task.execute();
        }
        catch (BuildException e)
        {
            throw new ConsumerException("Could not add jar " + path + " to obr repository.xml", e);
        }
    }

}
