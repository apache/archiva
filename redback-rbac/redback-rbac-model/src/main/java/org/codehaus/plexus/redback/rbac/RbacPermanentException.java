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
 * RbacPermanentException - tossed when a forbidden action against a permanent RBAC Object occurs.  
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class RbacPermanentException
    extends RbacManagerException
{
    public RbacPermanentException()
    {
        super();
    }

    public RbacPermanentException( String message, Throwable cause )
    {
        super( message, cause );
    }

    public RbacPermanentException( String message )
    {
        super( message );
    }

    public RbacPermanentException( Throwable cause )
    {
        super( cause );
    }
}
