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
 * RbacObjectInvalidException 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class RbacObjectInvalidException
    extends RbacManagerException
{

    public RbacObjectInvalidException()
    {
        super();
    }

    public RbacObjectInvalidException( String message, Throwable cause )
    {
        super( message, cause );
    }

    public RbacObjectInvalidException( String message )
    {
        super( message );
    }
    
    public RbacObjectInvalidException( String scope, String message )
    {
        super( ( ( scope != null ) ? scope + ": " : "" ) + message );
    }

    public RbacObjectInvalidException( Throwable cause )
    {
        super( cause );
    }
}
