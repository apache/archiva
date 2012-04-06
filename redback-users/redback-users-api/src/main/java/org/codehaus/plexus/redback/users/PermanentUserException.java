package org.codehaus.plexus.redback.users;

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
 * PermanentUserException - tossed when a forbidden action against a permanent user
 * occurs. 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class PermanentUserException
    extends UserManagerException
{

    public PermanentUserException()
    {
        super();
    }

    public PermanentUserException( String message, Throwable cause )
    {
        super( message, cause );
    }

    public PermanentUserException( String message )
    {
        super( message );
    }

    public PermanentUserException( Throwable cause )
    {
        super( cause );
    }
}
