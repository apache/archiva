package org.apache.maven.repository.proxy.configuration;

/*
 * Copyright 2003-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;

/**
 * Strips file:/// off the front of the configured URL and uses that to find files locally.
 *
 * @author Ben Walding
 */
public class FileRepoConfiguration
    extends RepoConfiguration
{
    private final String basePath;

    public FileRepoConfiguration( String key, String url, String description, boolean copy, boolean hardFail,
                                  boolean cacheFailures, long cachePeriod )
    {
        super( key, url, description, copy, hardFail, cacheFailures, cachePeriod );
        basePath = url.substring( 8 );
    }

    public String getBasePath()
    {
        return basePath;
    }

    /**
     * Given a relative path, returns the absolute file in the repository
     */
    public File getLocalFile( String path )
    {
        return new File( basePath + path );
    }
}