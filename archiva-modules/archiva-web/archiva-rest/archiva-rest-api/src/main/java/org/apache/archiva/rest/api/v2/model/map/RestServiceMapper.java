package org.apache.archiva.rest.api.v2.model.map;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.common.AbstractMapper;
import org.apache.archiva.common.MultiModelMapper;
import org.apache.archiva.configuration.model.ConfigurationModel;
import org.apache.archiva.repository.Repository;
import org.apache.archiva.rest.api.v2.model.RestModel;

/**
 * @author Martin Schreier <martin_s@apache.org>
 */
public abstract class RestServiceMapper<S extends RestModel, T extends ConfigurationModel, R extends Repository> extends AbstractMapper<S,T,R>
    implements MultiModelMapper<S,T,R>
{
}
