package org.apache.maven.archiva.policies;

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
import java.util.Properties;

/**
 * Policy to apply after the download has completed, but before the
 * resource is made available to the calling client. 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public interface PostDownloadPolicy
    extends DownloadPolicy
{
    /**
     * Apply the download policy.
     * 
     * A true result allows the download to succeed.  false indicates that the 
     * download is a failure.
     * 
     * @param policySetting the policy setting.
     * @param request the list of request properties that the policy might use.
     * @param localFile the local file that this policy affects
     * 
     * @throws PolicyViolationException if the policy has been violated.
     */
    public void applyPolicy( String policySetting, Properties request, File localFile )
        throws PolicyViolationException, PolicyConfigurationException;
}
