package org.apache.archiva.rest.services.utils;
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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.admin.model.AuditInformation;
import org.apache.archiva.redback.rest.services.RedbackAuthenticationThreadLocal;
import org.apache.archiva.redback.rest.services.RedbackRequestInformation;
import org.apache.archiva.redback.users.User;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class AuditHelper
{
    private static final AuditInformation NULL_RESULT = new AuditInformation( null, null );
    public static AuditInformation getAuditData() {
        RedbackRequestInformation redbackRequestInformation = RedbackAuthenticationThreadLocal.get( );
        if (redbackRequestInformation==null) {
            return NULL_RESULT;
        } else
        {
            return new AuditInformation( redbackRequestInformation.getUser( ), redbackRequestInformation.getRemoteAddr( ) );
        }
    }
}
