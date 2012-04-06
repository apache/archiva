package org.codehaus.plexus.redback.users.ldap;

/*
 * Copyright 2001-2007 The Codehaus.
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

import org.codehaus.plexus.redback.common.ldap.UserMapper;
import org.codehaus.plexus.redback.users.AbstractUserQuery;

public class LdapUserQuery
    extends AbstractUserQuery
{

    public void setFirstResult( int firstResult )
    {
        super.setFirstResult( firstResult );
        throw new UnsupportedOperationException( "Result limiting is not yet supported for LDAP." );
    }

    public void setMaxResults( int maxResults )
    {
        super.setMaxResults( maxResults );
        throw new UnsupportedOperationException( "Result limiting is not yet supported for LDAP." );
    }

    public void setOrderBy( String orderBy )
    {
        super.setOrderBy( orderBy );
        throw new UnsupportedOperationException( "Free-form ordering is not yet supported for LDAP." );
    }
    
    public String getLdapFilter( UserMapper mapper )
    {
        String filter = "";
        if (this.getEmail() != null )
        {
            filter += "(" + mapper.getEmailAddressAttribute() + "=" + this.getEmail() + ")";
        }
        if ( this.getFullName() != null )
        {
            filter += "(" + mapper.getUserFullNameAttribute() + "=" + this.getFullName() + ")";
        }
        filter += "(" + mapper.getUserIdAttribute() + "=" + ( this.getUsername() != null ? this.getUsername() : "*" ) + ")";
        
        return filter;
    }

}
