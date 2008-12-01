package org.apache.maven.archiva.web.action.admin.database;

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

import java.util.Comparator;

/**
 * AdminDatabaseConsumerComparator 
 *
 * @version $Id$
 */
public class AdminDatabaseConsumerComparator
    implements Comparator
{
    private static AdminDatabaseConsumerComparator INSTANCE = new AdminDatabaseConsumerComparator();

    public static AdminDatabaseConsumerComparator getInstance()
    {
        return INSTANCE;
    }

    public int compare( Object o1, Object o2 )
    {
        if ( o1 == null && o2 == null )
        {
            return 0;
        }

        if ( o1 == null && o2 != null )
        {
            return 1;
        }

        if ( o1 != null && o2 == null )
        {
            return -1;
        }

        if ( ( o1 instanceof AdminDatabaseConsumer ) && ( o2 instanceof AdminDatabaseConsumer ) )
        {
            String id1 = ( (AdminDatabaseConsumer) o1 ).getId();
            String id2 = ( (AdminDatabaseConsumer) o2 ).getId();
            return id1.compareToIgnoreCase( id2 );
        }

        return 0;
    }

}
