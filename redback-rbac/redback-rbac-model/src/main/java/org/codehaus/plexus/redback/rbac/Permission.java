package org.codehaus.plexus.redback.rbac;

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

/**
 * Permission
 * <p/>
 * A permission is the wrapper for an operation and a resource effectively saying
 * that the operation is authorized for that resource.
 * <p/>
 * P(Operation, Resource)
 *
 * @author Jesse McConnell <jmcconnell@apache.org>
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public interface Permission
{

    /**
     * Long description of the Permission
     */
    String getDescription();

    /**
     * Get the short name of the permission.
     * <p/>
     * NOTE: This field is considered the Primary Key for this object.
     *
     * @return the short name for this permission.
     */
    String getName();

    /**
     * Operation that this permission is authorizing
     */
    Operation getOperation();

    /**
     * This is the resource associated with this permission.
     * <p/>
     * Implementors must always supply a Resource.
     *
     * @return the Resource.
     */
    Resource getResource();

    /**
     * Set null
     *
     * @param description
     */
    void setDescription( String description );

    /**
     * Set the short name for this permission.
     *
     * @param name
     */
    void setName( String name );

    /**
     * Set null
     *
     * @param operation
     */
    void setOperation( Operation operation );

    /**
     * @param resource
     */
    void setResource( Resource resource );

    /**
     * Test to see if the object is a permanent object or not.
     *
     * @return true if the object is permanent.
     */
    boolean isPermanent();

    /**
     * Set flag indicating if the object is a permanent object or not.
     *
     * @param permanent true if the object is permanent.
     */
    void setPermanent( boolean permanent );
}