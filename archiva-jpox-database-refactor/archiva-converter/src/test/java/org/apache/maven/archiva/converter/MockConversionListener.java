package org.apache.maven.archiva.converter;

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
import org.apache.maven.artifact.Artifact;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * MockConversionListener 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class MockConversionListener
    implements ConversionListener
{
    private Map warnings = new HashMap();

    private Map errors = new HashMap();

    private Map exceptions = new HashMap();

    private List processed = new ArrayList();

    private List repositories = new ArrayList();

    public void conversionEvent( ConversionEvent event )
    {
        switch ( event.getType() )
        {
            case ConversionEvent.STARTED:
                addUnique( repositories, event.getRepository() );
                break;
            case ConversionEvent.PROCESSED:
                addUnique( processed, event.getArtifact() );
                break;
            case ConversionEvent.WARNING:
                if ( event.getException() != null )
                {
                    addObjectList( exceptions, toKey( event.getArtifact() ), event.getException() );
                }

                if ( event.getMessage() != null )
                {
                    addObjectList( warnings, toKey( event.getArtifact() ), event.getMessage() );
                }
                break;
            case ConversionEvent.ERROR:
                if ( event.getException() != null )
                {
                    addObjectList( exceptions, toKey( event.getArtifact() ), event.getException() );
                }

                if ( event.getMessage() != null )
                {
                    addObjectList( errors, toKey( event.getArtifact() ), event.getMessage() );
                }
                break;
            case ConversionEvent.FINISHED:
                addUnique( repositories, event.getRepository() );
                break;
        }
    }

    public String toKey( Artifact artifact )
    {
        return StringUtils.defaultString( artifact.getGroupId() ) + ":"
            + StringUtils.defaultString( artifact.getArtifactId() ) + ":"
            + StringUtils.defaultString( artifact.getVersion() ) + ":" + StringUtils.defaultString( artifact.getType() )
            + ":" + StringUtils.defaultString( artifact.getClassifier() );
    }

    private void addObjectList( Map map, String key, Object value )
    {
        List objlist = (List) map.get( key );
        if ( objlist == null )
        {
            objlist = new ArrayList();
        }

        objlist.add( value );

        map.put( key, objlist );
    }

    private void addUnique( Collection collection, Object obj )
    {
        if ( !collection.contains( obj ) )
        {
            collection.add( obj );
        }
    }

    public Map getErrors()
    {
        return errors;
    }

    public Map getExceptions()
    {
        return exceptions;
    }

    public List getProcessed()
    {
        return processed;
    }

    public List getRepositories()
    {
        return repositories;
    }

    public Map getWarnings()
    {
        return warnings;
    }

    private int getObjectListCount( Map map )
    {
        int count = 0;
        for ( Iterator it = map.values().iterator(); it.hasNext(); )
        {
            List objList = (List) it.next();
            count += objList.size();
        }
        return count;
    }

    public int getWarningMessageCount()
    {
        return getObjectListCount( warnings );
    }

    public int getErrorMessageCount()
    {
        return getObjectListCount( errors );
    }
}
