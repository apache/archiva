package org.apache.maven.archiva.artifact;

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

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * ManagedArtifactTypes - provides place to test an unknown artifact type.
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class ManagedArtifactTypes
{
    public static final int GENERIC = 0;

    public static final int JAVA = 1;

    public static final int EJB = 2;

    private static List javaArtifacts;

    private static List ejbArtifacts;

    static
    {
        javaArtifacts = new ArrayList();
        javaArtifacts.add( "jar" );
        javaArtifacts.add( "war" );
        javaArtifacts.add( "sar" );
        javaArtifacts.add( "rar" );
        javaArtifacts.add( "ear" );

        ejbArtifacts = new ArrayList();
        ejbArtifacts.add( "ejb" );
        ejbArtifacts.add( "ejb-client" );
    }

    public static int whichType( String type )
    {
        if ( StringUtils.isBlank( type ) )
        {
            // TODO: is an empty type even possible?
            return GENERIC;
        }

        type = type.toLowerCase();

        if ( ejbArtifacts.contains( type ) )
        {
            return EJB;
        }

        if ( javaArtifacts.contains( type ) )
        {
            return JAVA;
        }

        return GENERIC;
    }
}
