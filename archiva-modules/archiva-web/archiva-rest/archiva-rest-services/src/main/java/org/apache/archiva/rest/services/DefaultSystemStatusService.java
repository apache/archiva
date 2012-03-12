package org.apache.archiva.rest.services;
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

import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.rest.api.services.SystemStatusService;
import org.springframework.stereotype.Service;

/**
 * @author Olivier Lamy
 * @since 1.4-M3
 */
@Service("systemStatusService#rest")
public class DefaultSystemStatusService
    implements SystemStatusService
{
    public String getMemoryStatus()
        throws ArchivaRestServiceException
    {
        Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        long total = runtime.totalMemory();
        long used = total - runtime.freeMemory();
        long max = runtime.maxMemory();
        return formatMemory( used ) + "/" + formatMemory( total ) + " (Max: " + formatMemory( max ) + ")";
    }

    private static String formatMemory( long l )
    {
        return l / ( 1024 * 1024 ) + "M";
    }
}
