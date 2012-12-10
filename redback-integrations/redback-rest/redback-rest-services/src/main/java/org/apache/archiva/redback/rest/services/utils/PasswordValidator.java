package org.apache.archiva.redback.rest.services.utils;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.redback.policy.PasswordRuleViolations;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.redback.users.UserManagerException;
import org.apache.archiva.redback.users.UserNotFoundException;
import org.apache.archiva.redback.policy.PasswordEncoder;
import org.apache.archiva.redback.policy.PasswordRuleViolationException;
import org.apache.archiva.redback.system.SecuritySystem;
import org.apache.archiva.redback.rest.api.model.ErrorMessage;
import org.apache.archiva.redback.rest.api.services.RedbackServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Olivier Lamy
 * @since 1.4
 */
@Service("passwordValidator#rest")
public class PasswordValidator
{

    private Logger log = LoggerFactory.getLogger( getClass() );

    @Inject
    private SecuritySystem securitySystem;

    /**
     * @param password
     * @param principal
     * @return encoded password
     * @throws RedbackServiceException
     */
    public String validatePassword( String password, String principal )
        throws RedbackServiceException
    {
        try
        {
            // password validation with a tmp user
            User tempUser = securitySystem.getUserManager().createUser( "temp", "temp", "temp" );
            tempUser.setPassword( password );
            securitySystem.getPolicy().validatePassword( tempUser );

            PasswordEncoder encoder = securitySystem.getPolicy().getPasswordEncoder();

            User user = securitySystem.getUserManager().findUser( principal );
            String encodedPassword = encoder.encodePassword( password );
            user.setEncodedPassword( encodedPassword );
            user.setPassword( password );

            securitySystem.getPolicy().validatePassword( user );

            return encodedPassword;
        }
        catch ( UserNotFoundException e )
        {
            log.info( "user {} not found", e.getMessage() );
            List<ErrorMessage> errorMessages = new ArrayList<ErrorMessage>( 2 );
            ErrorMessage errorMessage = new ErrorMessage( "cannot.update.user.not.found", new String[]{ principal } );
            errorMessages.add( errorMessage );
            errorMessage = new ErrorMessage( "admin.deleted.account" );
            errorMessages.add( errorMessage );
            throw new RedbackServiceException( errorMessages );
        }
        catch ( PasswordRuleViolationException e )
        {
            PasswordRuleViolations violations = e.getViolations();
            List<ErrorMessage> errorMessages = new ArrayList<ErrorMessage>( violations.getViolations().size() );
            if ( violations != null )
            {
                for ( String violation : violations.getLocalizedViolations() )
                {
                    errorMessages.add( new ErrorMessage( violation ) );
                }
            }
            throw new RedbackServiceException( errorMessages );
        }
        catch ( UserManagerException e )
        {
            log.info( "UserManagerException: {}", e.getMessage() );
            List<ErrorMessage> errorMessages =
                Arrays.asList( new ErrorMessage().message( "UserManagerException: " + e.getMessage() ) );
            throw new RedbackServiceException( errorMessages );
        }

    }
}
