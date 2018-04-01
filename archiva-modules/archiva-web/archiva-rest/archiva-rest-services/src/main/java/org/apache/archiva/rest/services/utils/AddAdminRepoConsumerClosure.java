package org.apache.archiva.rest.services.utils;

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

import org.apache.archiva.consumers.RepositoryContentConsumer;
import org.apache.archiva.rest.api.model.AdminRepositoryConsumer;
import org.apache.commons.collections4.Closure;

import java.util.ArrayList;
import java.util.List;

/**
 * AddAdminRepoConsumerClosure
 */
public class AddAdminRepoConsumerClosure
    implements Closure<RepositoryContentConsumer>
{
    private List<AdminRepositoryConsumer> list = new ArrayList<>( );

    private List<String> selectedIds;

    public AddAdminRepoConsumerClosure( List<String> selectedIds )
    {
        this.selectedIds = selectedIds;
    }

    @Override
    public void execute( RepositoryContentConsumer input )
    {
        RepositoryContentConsumer consumer = (RepositoryContentConsumer) input;

        boolean enabled = this.selectedIds.contains( consumer.getId( ) );
        AdminRepositoryConsumer adminconsumer = new AdminRepositoryConsumer( );
        adminconsumer.setEnabled( enabled );
        adminconsumer.setId( consumer.getId( ) );
        adminconsumer.setDescription( consumer.getDescription( ) );

        list.add( adminconsumer );
    }

    public List<AdminRepositoryConsumer> getList( )
    {
        return list;
    }
}
