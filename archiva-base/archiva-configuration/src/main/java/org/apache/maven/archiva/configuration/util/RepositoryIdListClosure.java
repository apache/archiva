package org.apache.maven.archiva.configuration.util;

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

import org.apache.commons.collections.Closure;
import org.apache.maven.archiva.configuration.RepositoryConfiguration;

import java.util.List;

/**
 * RepositoryIdListClosure 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class RepositoryIdListClosure
    implements Closure
{
    private List list;

    public RepositoryIdListClosure( List list )
    {
        this.list = list;
    }

    public void execute( Object input )
    {
        if ( input instanceof RepositoryConfiguration )
        {
            RepositoryConfiguration repoconfig = (RepositoryConfiguration) input;
            list.add( repoconfig.getId() );
        }
    }

    public List getList()
    {
        return list;
    }
}
