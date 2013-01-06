package org.apache.archiva.web.security;
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

import org.apache.archiva.redback.authorization.AuthorizationDataSource;
import org.apache.archiva.redback.authorization.AuthorizationException;
import org.apache.archiva.redback.authorization.AuthorizationResult;
import org.apache.archiva.redback.authorization.Authorizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * @author Olivier Lamy
 * @since 1.4-M4
 */
@Service( "authorizer#archiva" )
public class ArchivaAuthorizer
    implements Authorizer
{
    private Logger log = LoggerFactory.getLogger( getClass() );

    @Inject
    @Named( value = "authorizer#rbac" )
    private Authorizer rbacAuthorizer;

    public String getId()
    {
        return "archiva";
    }

    public AuthorizationResult isAuthorized( AuthorizationDataSource source )
        throws AuthorizationException
    {
        log.debug( "isAuthorized source: {}", source );
        return rbacAuthorizer.isAuthorized( source );
    }

    public boolean isFinalImplementation()
    {
        return false;
    }

    public String getDescriptionKey()
    {
        return "archiva.redback.authorizer.archiva";
    }
}
