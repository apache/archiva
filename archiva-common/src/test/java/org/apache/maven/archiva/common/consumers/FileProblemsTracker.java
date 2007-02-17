package org.apache.maven.archiva.common.consumers;

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

import org.apache.maven.archiva.common.utils.BaseFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * FileProblemsTracker 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class FileProblemsTracker
{
    private Map problemMap = new HashMap();

    public void addProblem( BaseFile file, String message )
    {
        String path = file.getRelativePath();
        addProblem( path, message );
    }

    private void addProblem( String path, String message )
    {
        List problems = getProblems( path );
        problems.add( message );
        problemMap.put( path, problems );
    }

    public void addProblem( ConsumerException e )
    {
        if ( e.getFile() != null )
        {
            this.addProblem( e.getFile(), e.getMessage() );
        }
        else
        {
            this.addProblem( "|fatal|", e.getMessage() );
        }
    }

    public boolean hasProblems( String path )
    {
        if ( !problemMap.containsKey( path ) )
        {
            // No tracking of path at all.
            return false;
        }

        List problems = (List) problemMap.get( path );
        if ( problems == null )
        {
            // found path, but no list.
            return false;
        }

        return !problems.isEmpty();
    }

    public Set getPaths()
    {
        return problemMap.keySet();
    }

    public List getProblems( String path )
    {
        List problems = (List) problemMap.get( path );
        if ( problems == null )
        {
            problems = new ArrayList();
        }

        return problems;
    }

    public int getProblemCount()
    {
        int count = 0;
        for ( Iterator it = problemMap.values().iterator(); it.hasNext(); )
        {
            List problems = (List) it.next();
            count += problems.size();
        }

        return count;
    }

}
