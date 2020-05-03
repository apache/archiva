package org.apache.archiva.repository;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.repository.content.Artifact;
import org.apache.archiva.repository.content.ContentItem;
import org.apache.archiva.repository.content.Namespace;
import org.apache.archiva.repository.content.Project;
import org.apache.archiva.repository.content.Version;

/**
 *
 * Deletion status of a given item.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class ItemDeleteStatus
{
    public static final int OK = 0;
    public static final int DELETION_FAILED = 1;
    public static final int ITEM_NOT_FOUND = 2;
    public static final int UNKNOWN = 128;

    private ContentItem item;
    private int status;
    private Throwable exception;

    public ItemDeleteStatus(ContentItem item) {
        this.item = item;
        this.status = OK;
    }

    public ItemDeleteStatus(ContentItem item, int status) {
        this.item = item;
        this.status = status;
    }

    public ItemDeleteStatus(ContentItem item, int status, Throwable e) {
        this.item = item;
        this.status = status;
        this.exception = e;
    }

    public ContentItem getItem( )
    {
        return item;
    }

    public int getStatus( )
    {
        return status;
    }

    public Throwable getException( )
    {
        return exception;
    }

    public Class<? extends ContentItem> getItemType() {
        if (item instanceof Namespace ) {
            return Namespace.class;
        } else if (item instanceof Project ) {
            return Project.class;
        } else if (item instanceof Version ) {
            return Version.class;
        } else if (item instanceof Artifact ) {
            return Artifact.class;
        } else {
            return ContentItem.class;
        }
    }


    public <U extends ContentItem> U adapt(Class<U> clazz) throws IllegalArgumentException {
        if (clazz.isAssignableFrom( item.getClass() )) {
            return (U) item;
        } else {
            throw new IllegalArgumentException( "Cannot convert instance of " + item.getClass( ) + " to " + clazz );
        }
    }




}
