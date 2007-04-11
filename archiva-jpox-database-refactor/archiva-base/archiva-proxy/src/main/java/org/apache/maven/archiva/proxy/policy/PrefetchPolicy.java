package org.apache.maven.archiva.proxy.policy;

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

import java.io.File;

/**
 * Policy to apply before the fetch of content. 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public interface PrefetchPolicy
{
    /**
     * Apply the policy using the provided policy code and local file.
     * 
     * @param policyCode the policy code to use.
     * @param localFile the local file that might affect the policy.
     * @return true if the policy passes, false if the policy prevents the
     *         fetching of the content.
     */
    public boolean applyPolicy( String policyCode, File localFile );
}
