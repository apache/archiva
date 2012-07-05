package org.apache.archiva.redback.integration.model;

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

import org.apache.archiva.redback.users.User;
import org.apache.archiva.redback.integration.util.DateUtils;

/**
 * EditUserCredentials
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 *
 */
public class EditUserCredentials
    extends UserCredentials
{
    public EditUserCredentials()
    {
        super();
    }

    public EditUserCredentials( String username )
    {
        super();
        super.setUsername( username );
    }

    public EditUserCredentials( User user )
    {
        super();
        super.setUsername( user.getUsername() );
        super.setFullName( user.getFullName() );
        super.setEmail( user.getEmail() );
        super.setPassword( "" );
        super.setConfirmPassword( "" );

        super.setTimestampAccountCreation( DateUtils.formatWithAge( user.getAccountCreationDate(), "ago" ) );
        super.setTimestampLastLogin( DateUtils.formatWithAge( user.getLastLoginDate(), "ago" ) );
        super.setTimestampLastPasswordChange( DateUtils.formatWithAge( user.getLastPasswordChange(), "ago" ) );
    }

    public boolean isEdit()
    {
        return true;
    }
}
