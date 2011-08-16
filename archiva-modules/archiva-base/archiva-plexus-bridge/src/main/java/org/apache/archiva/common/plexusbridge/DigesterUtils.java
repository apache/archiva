package org.apache.archiva.common.plexusbridge;

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

import org.codehaus.plexus.digest.Digester;
import org.codehaus.plexus.digest.Md5Digester;
import org.codehaus.plexus.digest.Sha1Digester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

/**
 * @author Olivier Lamy
 * @since 1.4
 */
@Service( "digesterUtils" )
public class DigesterUtils
{

    private Logger log = LoggerFactory.getLogger( getClass() );

    private List<? extends Digester> allDigesters;

    @Inject
    public DigesterUtils( PlexusSisuBridge plexusSisuBridge )
        throws PlexusSisuBridgeException
    {
        this.allDigesters = plexusSisuBridge.lookupList( Digester.class );

        if ( allDigesters == null || allDigesters.isEmpty() )
        {
            // olamy when the TCL is not a URLClassLoader lookupList fail !
            // when using tomcat maven plugin so adding a simple hack
            log.warn( "using lookList from sisu plexus failed so build plexus Digesters manually" );

            allDigesters = Arrays.asList( new Sha1Digester(), new Md5Digester() );

        }

        log.debug( "allIndexCreators {}", allDigesters );

    }

    public List<? extends Digester> getAllDigesters()
    {
        return allDigesters;
    }

    public void setAllDigesters( List<? extends Digester> allDigesters )
    {
        this.allDigesters = allDigesters;
    }
}
