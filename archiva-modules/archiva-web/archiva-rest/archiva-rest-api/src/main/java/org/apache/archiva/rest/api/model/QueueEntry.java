package org.apache.archiva.rest.api.model;
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

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * @author Olivier Lamy
 * @since 1.4-M3
 */
@XmlRootElement( name = "queueEntry" )
public class QueueEntry
    implements Serializable
{
    private String key;

    private int entriesNumber;

    public QueueEntry()
    {
        // no op
    }

    public QueueEntry( String key, int entriesNumber )
    {
        this.key = key;
        this.entriesNumber = entriesNumber;
    }

    public String getKey()
    {
        return key;
    }

    public void setKey( String key )
    {
        this.key = key;
    }

    public int getEntriesNumber()
    {
        return entriesNumber;
    }

    public void setEntriesNumber( int entriesNumber )
    {
        this.entriesNumber = entriesNumber;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "QueueEntry" );
        sb.append( "{key='" ).append( key ).append( '\'' );
        sb.append( ", entriesNumber=" ).append( entriesNumber );
        sb.append( '}' );
        return sb.toString();
    }
}
