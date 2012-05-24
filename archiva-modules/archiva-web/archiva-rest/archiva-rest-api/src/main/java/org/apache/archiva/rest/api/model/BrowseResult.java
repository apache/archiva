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
import java.util.Collections;
import java.util.List;

/**
 * @author Olivier Lamy
 * @since 1.4-M3
 */
@XmlRootElement( name = "browseResult" )
public class BrowseResult
    implements Serializable
{
    private List<BrowseResultEntry> browseResultEntries;

    private boolean rootLevel;

    public BrowseResult()
    {
        // no op
    }

    public BrowseResult( List<BrowseResultEntry> browseResultEntries )
    {
        this.browseResultEntries = browseResultEntries;
    }

    public List<BrowseResultEntry> getBrowseResultEntries()
    {
        return browseResultEntries == null ? Collections.<BrowseResultEntry>emptyList() : browseResultEntries;
    }

    public void setBrowseResultEntries( List<BrowseResultEntry> browseResultEntries )
    {
        this.browseResultEntries = browseResultEntries;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "BrowseResult" );
        sb.append( "{browseResultEntries=" ).append( browseResultEntries );
        sb.append( ", rootLevel=" ).append( rootLevel );
        sb.append( '}' );
        return sb.toString();
    }
}
