package org.apache.archiva.metadata.repository.cassandra.model;

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

import org.apache.archiva.metadata.repository.cassandra.CassandraUtils;

import java.io.Serializable;

/**
 * @author Olivier Lamy
 * @since 2.0.0
 */
public class Project
    implements Serializable
{

    public static class KeyBuilder
    {

        private Namespace namespace;

        private String projectId;

        public KeyBuilder()
        {
            // no op
        }

        public KeyBuilder withNamespace( Namespace namespace )
        {
            this.namespace = namespace;
            return this;
        }

        public KeyBuilder withProjectId( String projectId )
        {
            this.projectId = projectId;
            return this;
        }


        public String build()
        {
            // FIXME add some controls
            return CassandraUtils.generateKey( new Namespace.KeyBuilder().withNamespace( this.namespace ).build(),
                                               this.projectId );
        }
    }
}
