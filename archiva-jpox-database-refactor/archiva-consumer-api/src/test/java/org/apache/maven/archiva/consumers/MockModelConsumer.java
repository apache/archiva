package org.apache.maven.archiva.consumers;

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
import org.apache.maven.archiva.consumers.GenericModelConsumer;
import org.apache.maven.model.Model;

import java.util.HashMap;
import java.util.Map;

/**
 * MockModelConsumer 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.common.consumers.Consumers"
 *     role-hint="mock-model"
 *     instantiation-strategy="per-lookup"
 */
public class MockModelConsumer
    extends GenericModelConsumer
{
    private Map modelMap = new HashMap();

    private FileProblemsTracker problemsTracker = new FileProblemsTracker();

    public void processModel( Model model, BaseFile file )
    {
        modelMap.put( file.getRelativePath(), model );
    }

    public void processFileProblem( BaseFile file, String message )
    {
        problemsTracker.addProblem( file, message );
    }

    public Map getModelMap()
    {
        return modelMap;
    }

    public String getName()
    {
        return "Mock Model Consumer (Testing Only)";
    }

    public FileProblemsTracker getProblemsTracker()
    {
        return problemsTracker;
    }

}