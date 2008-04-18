package org.apache.maven.archiva.webdav;

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

import org.apache.jackrabbit.webdav.*;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.io.InputContext;
import org.apache.jackrabbit.webdav.io.OutputContext;
import org.apache.jackrabbit.webdav.lock.*;
import org.apache.jackrabbit.util.Text;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.maven.archiva.webdav.util.MimeTypes;
import org.apache.maven.archiva.webdav.util.IndexWriter;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Date;
import java.io.*;

/**
 * @author <a href="mailto:james@atlassian.com">James William Dumay</a>
 */
public class ArchivaDavResource implements DavResource
{
    private final MimeTypes mimeTypes;

    private final DavResourceLocator locator;

    private final DavResourceFactory factory;

    private final DavSession session;

    private final File localResource;

    private final String logicalResource;

    private static final String METHODS = "OPTIONS, GET, HEAD, POST, TRACE, PROPFIND, PROPPATCH, MKCOL, COPY, PUT, DELETE, MOVE";

    private static final String COMPLIANCE_CLASS = "1";

    private DavPropertySet properties;

    public ArchivaDavResource(String localResource, String logicalResource, MimeTypes mimeTypes, DavResourceLocator locator, DavResourceFactory factory, DavSession session)
    {
        this.mimeTypes = mimeTypes;
        this.localResource = new File(localResource);
        this.logicalResource = logicalResource;
        this.locator = locator;
        this.factory = factory;
        this.session = session;
        this.properties = new DavPropertySet();
    }

    public String getContentType()
    {
        return mimeTypes.getMimeType(localResource.getName());
    }

    public String getComplianceClass()
    {
        return COMPLIANCE_CLASS;
    }

    public String getSupportedMethods()
    {
        return METHODS;
    }

    public boolean exists()
    {
        return localResource.exists();
    }

    public boolean isCollection()
    {
        return localResource.isDirectory();
    }

    public String getDisplayName()
    {
        String resPath = getResourcePath();
        return (resPath != null) ? Text.getName(resPath) : resPath;
    }

    public DavResourceLocator getLocator()
    {
        return locator;
    }

    public String getResourcePath()
    {
        return locator.getResourcePath();
    }

    public String getHref()
    {
        return locator.getHref(isCollection());
    }

    public long getModificationTime()
    {
        return localResource.lastModified();
    }

    public long getContentLength()
    {
        return localResource.length();
    }

    public void spool(OutputContext outputContext) throws IOException
    {
        if (!isCollection())
        {
            IOUtils.copy(new FileInputStream(localResource), outputContext.getOutputStream());
        }
        else
        {
            IndexWriter writer = new IndexWriter(this, localResource, logicalResource);
            writer.write(outputContext);
        }
    }

    public DavPropertyName[] getPropertyNames()
    {
        return new DavPropertyName[0];
    }

    public DavProperty getProperty(DavPropertyName name)
    {
        return null;
    }

    public DavPropertySet getProperties()
    {
        return properties;
    }

    public void setProperty(DavProperty property) throws DavException
    {
    }

    public void removeProperty(DavPropertyName propertyName) throws DavException
    {
    }

    public MultiStatusResponse alterProperties(DavPropertySet setProperties, DavPropertyNameSet removePropertyNames) throws DavException
    {
        return null;
    }

    public MultiStatusResponse alterProperties(List changeList) throws DavException
    {
        return null;
    }

    public DavResource getCollection()
    {
        DavResource parent = null;
        if (getResourcePath() != null && !getResourcePath().equals("/")) {
            String parentPath = Text.getRelativeParent(getResourcePath(), 1);
            if (parentPath.equals("")) {
                parentPath = "/";
            }
            DavResourceLocator parentloc = locator.getFactory().createResourceLocator(locator.getPrefix(), locator.getWorkspacePath(), parentPath);
            try {
                parent = factory.createResource(parentloc, session);
            } catch (DavException e) {
                // should not occur
            }
        }
        return parent;
    }

    public void addMember(DavResource resource, InputContext inputContext) throws DavException
    {
        File localFile = new File(localResource, resource.getDisplayName());
        if (!resource.isCollection() && isCollection() && inputContext.hasStream()) //New File
        {
            boolean deleteFile = false;
            FileOutputStream stream = null;
            try
            {
                stream = new FileOutputStream(localFile);
                IOUtils.copy(inputContext.getInputStream(), stream);
                if (inputContext.getContentLength() != localFile.length())
                {
                    deleteFile = true;
                    throw new DavException(HttpServletResponse.SC_BAD_REQUEST, "Content Header length was "
                            + inputContext.getContentLength() + " but was " + localFile.length());
                }
            }
            catch (IOException e)
            {
                throw new DavException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
            }
            finally
            {
                IOUtils.closeQuietly(stream);
                if (deleteFile)
                {
                    FileUtils.deleteQuietly(localFile);
                }
            }
        }
        else if (resource.isCollection() && isCollection()) //New directory
        {
            localFile.mkdir();
        }
        else
        {
            throw new DavException(HttpServletResponse.SC_BAD_REQUEST, "Could not write member "
                    + resource.getResourcePath() + " at " + getResourcePath());
        }
    }

    public DavResourceIterator getMembers()
    {
        return null;
    }

    public void removeMember(DavResource member) throws DavException
    {
    }

    public void move(DavResource destination) throws DavException
    {
    }

    public void copy(DavResource destination, boolean shallow) throws DavException
    {
    }

    public boolean isLockable(Type type, Scope scope)
    {
        return false;
    }

    public boolean hasLock(Type type, Scope scope)
    {
        return false;
    }

    public ActiveLock getLock(Type type, Scope scope)
    {
        return null;
    }

    public ActiveLock[] getLocks()
    {
        return new ActiveLock[0];
    }

    public ActiveLock lock(LockInfo reqLockInfo) throws DavException
    {
        return null;
    }

    public ActiveLock refreshLock(LockInfo reqLockInfo, String lockToken) throws DavException
    {
        return null;
    }

    public void unlock(String lockToken) throws DavException
    {
    }

    public void addLockManager(LockManager lockmgr)
    {
    }

    public DavResourceFactory getFactory()
    {
        return factory;
    }

    public DavSession getSession()
    {
        return session;
    }
}
