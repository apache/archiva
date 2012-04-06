package org.codehaus.plexus.redback.keys;

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
 * KeyManagerException 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class KeyManagerException
    extends Exception
{

    public KeyManagerException()
    {
        super();
    }

    public KeyManagerException( String message, Throwable cause )
    {
        super( message, cause );
    }

    public KeyManagerException( String message )
    {
        super( message );
    }

    public KeyManagerException( Throwable cause )
    {
        super( cause );
    }

}
