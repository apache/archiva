package org.apache.archiva.metadata.repository.storage.maven2;

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

import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.plugin.LifecycleBindingsInjector;

/**
 * Required as plexus-spring doesn't understand the optional = true argument added to Plexus and used here.
 *
 *
 */
public class DummyLifecycleBindingsInjector
    implements LifecycleBindingsInjector
{
    @Override
    public void injectLifecycleBindings( Model model, ModelBuildingRequest modelBuildingRequest, ModelProblemCollector modelProblemCollector )
    {
        // left intentionally blank
    }
}
