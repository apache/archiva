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
 * Resource
 *
 * Resources are things that can be paired up with operations inside of a
 * permission.
 *
 * Rbac doesn't strictly specify what a resource (or Object) is, so there are a
 * couple of variations planned for resources.
 *
 * Initially the resource is simply a string representaton of whatever you desire
 * to match up to an operation.  Eventually we want to support different types of
 * expression evaluation for these resources, like a tuple resource.  *-* where
 * wildcards can be used on the resource definition to streamline the assigning of
 * permissions for _large_ sets of things.
 *
 * @author Jesse McConnell <jmcconnell@apache.org>
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public interface Resource
{
    /**
     * Resource identifier refering to all objects.
     */
    public static final String GLOBAL = "*";

    /**
     * Resource identifier refering to no objects.
     */
    public static final String NULL = "-";

    /**
     * Get The string identifier for an operation.
     *
     * NOTE: This field is considered the Primary Key for this object.
     */
    public String getIdentifier();

    /**
     * true if the identifer is a pattern that is to be evaluated, for
     * example x.* could match x.a or x.b and x.** could match x.foo
     *
     * Jesse: See {@link #setPattern(boolean)}
     *
     */
    public boolean isPattern();

    /**
     * Set The string identifier for an operation.
     *
     * NOTE: This field is considered the Primary Key for this object.
     * 
     * @param identifier
     */
    public void setIdentifier( String identifier );

    /**
     * true if the identifer is a pattern that is to be evaluated, for
     * example x.* could match x.a or x.b and x.** could match x.foo
     *
     * TODO is this even a good idea?
     * TODO we could look for a character like '*' or a string starting with "%/" to indicate if this is a pattern or not.
     * 
     * @param pattern
     */
    public void setPattern( boolean pattern );

    /**
     * Test to see if the object is a permanent object or not.
     * 
     * @return true if the object is permanent.
     */
    public boolean isPermanent();

    /**
     * Set flag indicating if the object is a permanent object or not.
     * 
     * @param permanent true if the object is permanent.
     */
    public void setPermanent( boolean permanent );
}