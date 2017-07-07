package org.apache.archiva.common.plexusbridge;

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

import org.apache.maven.index.context.IndexCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Olivier Lamy
 * @since 1.4-M1
 */
@Service( "mavenIndexerUtils" )
public class MavenIndexerUtils
{

    private Logger log = LoggerFactory.getLogger( getClass() );

    private List<? extends IndexCreator> allIndexCreators;

    private ApplicationContext applicationContext;

    @Inject
    public MavenIndexerUtils(ApplicationContext applicationContext, Map<String, IndexCreator> indexers)
    {
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void initialize()
    {
        allIndexCreators = new ArrayList<>( applicationContext.getBeansOfType( IndexCreator.class ).values());

        if ( allIndexCreators == null || allIndexCreators.isEmpty() )
        {
            throw new RuntimeException( "cannot initiliaze IndexCreators" );
        }

        log.debug( "allIndexCreators {}", allIndexCreators );
    }

    public List<? extends IndexCreator> getAllIndexCreators()
    {
        return allIndexCreators;
    }

    public void setAllIndexCreators( List<IndexCreator> allIndexCreators )
    {
        this.allIndexCreators = allIndexCreators;
    }
}
