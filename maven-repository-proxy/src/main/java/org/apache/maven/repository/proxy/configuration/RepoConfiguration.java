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

import org.codehaus.plexus.logging.AbstractLogEnabled;

/**
 * Immutable.
 * <p/>
 * hardfail - if a repository is set to hard fail, then the download engine will terminate the whole download
 * process (with a status 500) if any of the repositories have unexpected errors.
 * <p/>
 * if a repository expects an error - eg. 400 (not found) - then it is not required to terminate the
 * download process.
 *
 * @author Ben Walding
 */
public abstract class RepoConfiguration
    extends AbstractLogEnabled
{
    private final String key;

    private final String description;

    private final String url;

    private final boolean copy;

    private final boolean hardFail;

    private final boolean cacheFailures;

    private final long cachePeriod;

    public RepoConfiguration( String key, String url, String description, boolean copy, boolean hardFail,
                              boolean cacheFailures, long cachePeriod )
    {
        this.key = key;
        this.url = url;
        this.description = description;
        this.copy = copy;
        this.hardFail = hardFail;
        this.cacheFailures = cacheFailures;
        this.cachePeriod = cachePeriod;
    }

    /**
     *
     */
    public String getUrl()
    {
        return url;
    }

    /**
     *
     */
    public String getKey()
    {
        return key;
    }

    /**
     * If a file repository is set to "copy" mode, it will copy the found files into
     * the main repository store.
     */
    public boolean getCopy()
    {
        return copy;
    }

    public String getDescription()
    {
        return description;
    }

    public boolean getHardFail()
    {
        return hardFail;
    }

    public boolean getCacheFailures()
    {
        return cacheFailures;
    }

    public long getCachePeriod()
    {
        return cachePeriod;
    }

    public String toString()
    {
        return "Repo[" + getKey() + "]";
    }
}