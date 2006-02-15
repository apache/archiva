package org.apache.maven.repository.proxy.configuration;

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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * @author Ben Walding
 */
public class RetrievalComponentConfiguration
{
    private final Map proxies = new HashMap();

    private final List repos = new ArrayList();

    private String localStore;

    private String serverName;

    private String bgColor;

    private String bgColorHighlight;

    private String rowColor;

    private String rowColorHighlight;

    private String stylesheet;

    private boolean searchable;

    private boolean browsable;

    private int port;

    private String prefix;

    private boolean snapshotUpdate;

    private boolean metaDataUpdate;

    private boolean pomUpdate;

    private String lastModifiedDateFormat;

    public String getPrefix()
    {
        return prefix;
    }

    public void setPrefix( String prefix )
    {
        this.prefix = prefix;
    }

    /**
     * @return
     */
    public boolean isBrowsable()
    {
        return browsable;
    }

    /**
     * @param browsable
     */
    public void setBrowsable( boolean browsable )
    {
        this.browsable = browsable;
    }

    public int getPort()
    {
        return port;
    }

    public void setPort( int port )
    {
        this.port = port;
    }

    /**
     * @return
     */
    public String getLocalStore()
    {
        return localStore;
    }

    /**
     * @param localStore
     */
    public void setLocalStore( String localStore )
    {
        this.localStore = localStore;
    }

    public void addProxy( MavenProxyConfiguration pc )
    {
        proxies.put( pc.getKey(), pc );
    }

    public void removeProxy( String key )
    {
        proxies.remove( key );
    }

    public MavenProxyConfiguration getProxy( String key )
    {
        return (MavenProxyConfiguration) proxies.get( key );
    }

    /**
     * There is no specific order to proxy configuration.
     *
     * @return
     */
    public List getProxies()
    {
        return new ArrayList( proxies.values() );
    }

    public void addRepo( RepoConfiguration repo )
    {
        repos.add( repo );
    }

    public List getRepos()
    {
        return Collections.unmodifiableList( repos );
    }

    public String getServerName()
    {
        return serverName;
    }

    public void setServerName( String serverName )
    {
        this.serverName = serverName;
    }

    public void setSnapshotUpdate( boolean snapshotUpdate )
    {
        this.snapshotUpdate = snapshotUpdate;
    }

    public boolean getSnapshotUpdate()
    {
        return snapshotUpdate;
    }

    public void setMetaDataUpdate( boolean metaDataUpdate )
    {
        this.metaDataUpdate = metaDataUpdate;
    }

    public boolean getMetaDataUpdate()
    {
        return metaDataUpdate;
    }

    public void setPOMUpdate( boolean pomUpdate )
    {
        this.pomUpdate = pomUpdate;
    }

    public boolean getPOMUpdate()
    {
        return pomUpdate;
    }

    public String getLastModifiedDateFormat()
    {
        return lastModifiedDateFormat;
    }

    public void setLastModifiedDateFormat( String lastModifiedDateFormat )
    {
        this.lastModifiedDateFormat = lastModifiedDateFormat;
    }

    public boolean isSearchable()
    {
        return searchable;
    }

    public void setSearchable( boolean searchable )
    {
        this.searchable = searchable;
    }

    /**
     * @return the global repo configuration
     */
    public GlobalRepoConfiguration getGlobalRepo()
    {
        for ( Iterator iter = repos.iterator(); iter.hasNext(); )
        {
            RepoConfiguration repo = (RepoConfiguration) iter.next();
            if ( repo instanceof GlobalRepoConfiguration )
            {
                return (GlobalRepoConfiguration) repo;
            }
        }
        return null;
    }

    private ThreadLocal dateFormatThreadLocal = new ThreadLocal()
    {
        protected synchronized Object initialValue()
        {
            DateFormat df;

            if ( getLastModifiedDateFormat() == null || getLastModifiedDateFormat() == "" )
            {
                df = new SimpleDateFormat();
            }
            else
            {
                df = new SimpleDateFormat( getLastModifiedDateFormat() );
            }

            df.setTimeZone( TimeZone.getTimeZone( "GMT" ) );
            return df;
        }
    };

    /**
     * Retrieves and casts the appropriate DateFormat object from a ThreadLocal
     *
     * @return
     */
    public DateFormat getLastModifiedDateFormatForThread()
    {
        return (DateFormat) dateFormatThreadLocal.get();
    }

    public String getBgColor()
    {
        return bgColor;
    }

    public void setBgColor( String bgColor )
    {
        this.bgColor = bgColor;
    }

    public String getBgColorHighlight()
    {
        return bgColorHighlight;
    }

    public void setBgColorHighlight( String bgColorHighlight )
    {
        this.bgColorHighlight = bgColorHighlight;
    }

    public String getStylesheet()
    {
        return stylesheet;
    }

    public void setStylesheet( String stylesheet )
    {
        this.stylesheet = stylesheet;
    }

    public String getRowColor()
    {
        return rowColor;
    }

    public void setRowColor( String rowColor )
    {
        this.rowColor = rowColor;
    }

    public String getRowColorHighlight()
    {
        return rowColorHighlight;
    }

    public void setRowColorHighlight( String rowColorHighlight )
    {
        this.rowColorHighlight = rowColorHighlight;
    }

}