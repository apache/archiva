package org.codehaus.plexus.redback.rbac;

/*
 * Copyright 2001-2006 The Codehaus.
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
 * RbacObjectNotFoundException used by {@link RBACManager} methods to identify
 * when a RBAC Object Was Not Found. 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class RbacObjectNotFoundException
    extends RbacManagerException
{
    private Object object;

    public RbacObjectNotFoundException()
    {
        super();
    }

    public RbacObjectNotFoundException( String message, Throwable cause )
    {
        super( message, cause );
    }

    public RbacObjectNotFoundException( String message, Throwable cause, Object object )
    {
        super( message, cause );
        this.object = object;
    }

    public RbacObjectNotFoundException( String message )
    {
        super( message );
    }

    public RbacObjectNotFoundException( String message, Object object )
    {
        super( message );
        this.object = object;
    }

    public RbacObjectNotFoundException( Throwable cause )
    {
        super( cause );
    }

    public Object getObject()
    {
        return object;
    }
}
