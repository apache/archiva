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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.maven.archiva.consumers.ConsumerException;
import org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.content.ManagedDefaultRepositoryContent;
import org.codehaus.plexus.util.FileUtils;
import org.osgi.impl.bundle.obr.resource.BundleInfo;
import org.osgi.impl.bundle.obr.resource.RepositoryImpl;
import org.osgi.impl.bundle.obr.resource.ResourceImpl;
import org.osgi.impl.bundle.obr.resource.Tag;
import org.osgi.service.obr.Repository;
import org.osgi.service.obr.Resource;

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
        try
        {
            final String name = content.getRepository().getName();
            final File repoRoot = new File(content.getRepository().getLocation());
            createRepositoryIndex(name, repoRoot, new File(path));
        }
        catch (Exception e)
        {
            throw new ConsumerException("Could not add jar " + path + " to obr repository.xml", e);
        }
    }
    
    private void createRepositoryIndex(String repositoryName, File repoRoot, File jarFile) throws Exception
    {
        File repositoryXml = new File(repoRoot, "repository.zip");
        RepositoryImpl repositoryImpl = new RepositoryImpl(repositoryXml.toURL());
        repositoryImpl.refresh();

        BundleInfo info = new BundleInfo(repositoryImpl, jarFile);

        Tag tag = new Tag("repository");
		tag.addAttribute("lastmodified", new Date());
		tag.addAttribute("name", repositoryName);
        tag.addContent(info.build().toXML());

        for (Resource resource : repositoryImpl.getResources())
        {
            tag.addContent(((ResourceImpl)resource).toXML());
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, "UTF-8"));
        tag.print(0, pw);
        pw.close();
        byte buffer[] = out.toByteArray();
        String name = "repository.xml";

        //Write file out of place
        File tmpFile = File.createTempFile("repository.zip", null);
        FileOutputStream fout = new FileOutputStream(tmpFile);
        ZipOutputStream zip = new ZipOutputStream(fout);
        CRC32 checksum = new CRC32();
        checksum.update(buffer);
        ZipEntry ze = new ZipEntry(name);
        ze.setSize(buffer.length);
        ze.setCrc(checksum.getValue());
        zip.putNextEntry(ze);
        zip.write(buffer, 0, buffer.length);
        zip.closeEntry();
        zip.close();
        fout.close();

        //Copy into place
        FileUtils.copyFile(tmpFile, repositoryXml);
	}
}
