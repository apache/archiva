package org.apache.archiva.redback.keys;

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

import java.util.Date;

/**
 * AuthenticationKey is an object representing a key established to
 * automatically authenticate a user without the user providing typical
 * login credentials.
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 *
 */
public interface AuthenticationKey
{
    Date getDateCreated();

    Date getDateExpires();

    String getForPrincipal();

    String getKey();

    /**
     * A String representation of what the purpose of existence is for this key.
     * <p/>
     * Examples: "selfservice password reset", "inter system communications", "remember me"
     *
     * @return
     */
    String getPurpose();

    void setDateCreated( Date dateCreated );

    void setDateExpires( Date dateExpires );

    void setForPrincipal( String forPrincipal );

    void setKey( String key );

    void setPurpose( String requestedFrom );
}
