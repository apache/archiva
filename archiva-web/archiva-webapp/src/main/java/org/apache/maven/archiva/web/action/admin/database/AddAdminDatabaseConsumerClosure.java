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

import org.apache.commons.collections.Closure;
import org.apache.maven.archiva.consumers.ArchivaArtifactConsumer;

import java.util.ArrayList;
import java.util.List;

/**
 * AddAdminDatabaseConsumerClosure 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class AddAdminDatabaseConsumerClosure
    implements Closure
{
    private List list = new ArrayList();

    private List selectedIds;

    public AddAdminDatabaseConsumerClosure( List selectedIds )
    {
        this.selectedIds = selectedIds;
    }

    public void execute( Object input )
    {
        if ( input instanceof ArchivaArtifactConsumer )
        {
            ArchivaArtifactConsumer consumer = (ArchivaArtifactConsumer) input;

            boolean enabled = this.selectedIds.contains( consumer.getId() );

            AdminDatabaseConsumer adminconsumer = new AdminDatabaseConsumer();
            adminconsumer.setEnabled( enabled );
            adminconsumer.setId( consumer.getId() );
            adminconsumer.setDescription( consumer.getDescription() );

            list.add( adminconsumer );
        }
    }

    public List getList()
    {
        return list;
    }
}
