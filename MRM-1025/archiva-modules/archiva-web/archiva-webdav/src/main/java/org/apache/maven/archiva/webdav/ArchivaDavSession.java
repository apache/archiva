package org.apache.maven.archiva.webdav;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.HashSet;
import org.apache.jackrabbit.webdav.DavSession;

public class ArchivaDavSession implements DavSession
{
    private final HashSet lockTokens = new HashSet();

    public void addLockToken(String token) 
    {
        lockTokens.add(token);
    }

    public String[] getLockTokens() 
    {
        return (String[]) lockTokens.toArray(new String[lockTokens.size()]);
    }

    public void removeLockToken(String token) 
    {
        lockTokens.remove(token);
    }

    public void removeReference(Object reference) 
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void addReference(Object reference) 
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
