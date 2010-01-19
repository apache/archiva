package org.apache.maven.archiva.web.action;

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

import java.util.Arrays;

import org.apache.maven.archiva.repository.audit.AuditEvent;
import org.easymock.ArgumentsMatcher;

public class AuditEventArgumentsMatcher
    implements ArgumentsMatcher
{
    public boolean matches( Object[] objects, Object[] objects1 )
    {
        if ( objects.length != 1 || objects1.length != 1 )
        {
            return false;
        }
        else
        {
            AuditEvent o1 = (AuditEvent) objects[0];
            AuditEvent o2 = (AuditEvent) objects1[0];
            o2.setTimestamp( o1.getTimestamp() ); // effectively ignore the timestamp
            return o1.equals( o2 );
        }
    }

    public String toString( Object[] objects )
    {
        return Arrays.asList( objects ).toString();
    }
}
