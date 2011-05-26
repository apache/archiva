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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.net.URL;
import java.util.List;

/**
 * Simple component which will initiate the plexus shim component
 * to see plexus components inside a guice container.<br/>
 * So move all of this here to be able to change quickly if needed.
 *
 * @author Olivier Lamy
 */
@Service("plexusSisuBridge")
public class PlexusSisuBridge
{

    private boolean containerAutoWiring = false;

    private String containerClassPathScanning = PlexusConstants.SCANNING_OFF;

    private String containerComponentVisibility = PlexusConstants.REALM_VISIBILITY;

    private URL overridingComponentsXml;

    private DefaultPlexusContainer plexusContainer;

    @PostConstruct
    public void initialize()
        throws PlexusContainerException
    {
        DefaultContainerConfiguration conf = new DefaultContainerConfiguration();

        conf.setAutoWiring( containerAutoWiring );
        conf.setClassPathScanning( containerClassPathScanning );
        conf.setComponentVisibility( containerComponentVisibility );

        conf.setContainerConfigurationURL( overridingComponentsXml );

        ClassWorld classWorld = new ClassWorld();

        ClassRealm classRealm = new ClassRealm( classWorld, "maven", Thread.currentThread().getContextClassLoader() );
        conf.setRealm( classRealm );

        conf.setClassWorld( classWorld );

        plexusContainer = new DefaultPlexusContainer( conf );
    }

    public <T> T lookup( Class<T> clazz )
        throws ComponentLookupException
    {
        return plexusContainer.lookup( clazz );
    }

    public <T> T lookup( Class<T> clazz, String hint )
        throws ComponentLookupException
    {
        return plexusContainer.lookup( clazz, hint );
    }

    public <T> List<T> lookupList( Class<T> clazz )
        throws ComponentLookupException
    {
        return plexusContainer.lookupList( clazz );
    }
}
