package org.apache.archiva.repository.api;

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

import java.io.File;
import java.util.Comparator;

/**
 * Represents the Status of an resource from a RepositoryManager
 */
public final class Status
{
    private static final MimeTypes mimeTypes = new MimeTypes();

    private Status()
    {
    }

    private ResourceType resourceType;

    private String contentType;

    private long contentLength;

    private long lastModified;

    private long createdDate;

    private String name;

    /**
     * Sets the Name of the resource
     * @param name
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Sets the Name of the resource
     * @return name
     */
    public String getName()
    {
        return name;
    }

    /**
     * Gets the length of the resources
     * @return length
     */
    public long getContentLength()
    {
        return contentLength;
    }

    /**
     * Sets the length of the resource
     * @param contentLength
     */
    public void setContentLength(long contentLength)
    {
        this.contentLength = contentLength;
    }

    /**
     * Gets the Content Type of the resource
     * @return
     */
    public String getContentType()
    {
        return contentType;
    }

    /**
     * Sets the contentType of the resource
     * @param contentType
     */
    public void setContentType(String contentType)
    {
        this.contentType = contentType;
    }

    /**
     * Gets the created date of a resource
     * @return createdDate
     */
    public long getCreatedDate()
    {
        return createdDate;
    }

    /**
     * Sets the created date of a resource
     * @param createdDate
     */
    public void setCreatedDate(long createdDate)
    {
        this.createdDate = createdDate;
    }

    /**
     * Gets the last modified date of a resource
     * @return lastModified
     */
    public long getLastModified()
    {
        return lastModified;
    }

    /**
     * Sets the last modified date of a resource
     * @param lastModified
     */
    public void setLastModified(long lastModified)
    {
        this.lastModified = lastModified;
    }

    /**
     * Gets the resource type
     * @return resourceType
     */
    public ResourceType getResourceType()
    {
        return resourceType;
    }

    /**
     * Sets the resource type
     * @param resourceType
     */
    public void setResourceType(ResourceType resourceType)
    {
        this.resourceType = resourceType;
    }

    /**
     * Builds a Status object for the given file.
     *
     * @param file
     * @return status
     */
    public static Status fromFile(File file)
    {
        Status status = new Status();
        status.setName(file.getName());
        status.setLastModified(file.lastModified());
        status.setCreatedDate(file.lastModified());

        if (file.isDirectory())
        {
            status.setResourceType(ResourceType.Collection);
        }
        else
        {
            status.setContentType(mimeTypes.getMimeType(file.getName()));
            status.setContentLength(file.length());
            status.setResourceType(ResourceType.Resource);
        }
        return status;
    }

    /**
     * Comparator for Status names
     */
    public static class StatusNameComparator implements Comparator<Status>
    {
        public int compare(Status status1, Status status2)
        {
            return status1.getName().compareTo(status2.getName());
        }
    }
}
