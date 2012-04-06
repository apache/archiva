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

import java.util.Date;

/**
 * AuthenticationKey is an object representing a key established to 
 * automatically authenticate a user without the user providing typical
 * login credentials.  
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public interface AuthenticationKey
{
    public Date getDateCreated();
    public Date getDateExpires();
    public String getForPrincipal();
    public String getKey();
    
    /**
     * A String representation of what the purpose of existence is for this key.
     * 
     * Examples: "selfservice password reset", "inter system communications", "remember me"
     * 
     * @return
     */
    public String getPurpose();
    
    public void setDateCreated( Date dateCreated );
    public void setDateExpires( Date dateExpires );
    public void setForPrincipal( String forPrincipal );
    public void setKey( String key );
    public void setPurpose( String requestedFrom );
}
