package org.apache.maven.repository.proxy.web.action.test.stub;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.repository.proxy.ProxyManager;
import org.apache.maven.repository.proxy.configuration.ProxyConfiguration;

import java.io.File;

public class ProxyManagerStub
    implements ProxyManager
{
    String baseDir;

    public ProxyManagerStub( String base )
    {
        baseDir = base;
    }

    public File get( String requestFile )
    {
        return new File( baseDir, "proxy-cache/test-0.0.jar" );
    }

    public File getRemoteFile( String reqFile )
    {
        return new File( baseDir, "proxy-chache/test-0.0.jar" );
    }

    public void setConfiguration( ProxyConfiguration config )
    {
        // do nothing
    }

    public ProxyConfiguration getConfiguration()
    {
        return null;
    }

    public File getAlways( String name )
    {
        return null;
    }
}
