package org.apache.archiva.web.runtime;
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

import org.apache.commons.lang.math.NumberUtils;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Properties;

/**
 * @author Olivier Lamy
 * @since 1.4-M3
 */
@Service( "archivaRuntimeInfo" )
public class ArchivaRuntimeInfo
{

    private String version;

    private String buildNumber;

    private long timestamp;


    @Inject
    public ArchivaRuntimeInfo( @Named( value = "archivaRuntimeProperties" ) Properties archivaRuntimeProperties )
    {
        this.version = (String) archivaRuntimeProperties.get( "archiva.version" );
        this.buildNumber = (String) archivaRuntimeProperties.get( "archiva.buildNumber" );
        this.timestamp = NumberUtils.createLong( (String) archivaRuntimeProperties.get( "archiva.timestamp" ) );
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    public String getBuildNumber()
    {
        return buildNumber;
    }

    public void setBuildNumber( String buildNumber )
    {
        this.buildNumber = buildNumber;
    }

    public long getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp( long timestamp )
    {
        this.timestamp = timestamp;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "ArchivaRuntimeInfo" );
        sb.append( "{version='" ).append( version ).append( '\'' );
        sb.append( ", buildNumber='" ).append( buildNumber ).append( '\'' );
        sb.append( ", timestamp=" ).append( timestamp );
        sb.append( '}' );
        return sb.toString();
    }
}
