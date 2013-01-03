package org.apache.archiva.redback.common.ldap.user;

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
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import javax.naming.directory.Attributes;

/**
 *
 */
public class UserUpdate
{

    private final Attributes created;

    private final Attributes modified;

    private final Attributes removed;

    public UserUpdate( Attributes created, Attributes modified, Attributes removed )
    {
        this.created = created;
        this.modified = modified;
        this.removed = removed;
    }

    public Attributes getAddedAttributes()
    {
        return created;
    }

    public Attributes getModifiedAttributes()
    {
        return modified;
    }

    public Attributes getRemovedAttributes()
    {
        return removed;
    }

    public boolean hasAdditions()
    {
        return ( created != null ) && ( created.size() > 0 );
    }

    public boolean hasModifications()
    {
        return ( modified != null ) && ( modified.size() > 0 );
    }



}
