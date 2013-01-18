package org.apache.archiva.redback.rest.services.utils;
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

import org.apache.archiva.redback.system.check.EnvironmentCheck;
import org.apache.commons.lang.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Olivier Lamy
 * @since 1.4
 */
@Service("environmentChecker#rest")
public class EnvironmentChecker
{

    private Logger log = LoggerFactory.getLogger( getClass() );


    @Inject
    public EnvironmentChecker( ApplicationContext applicationContext )
    {
        Collection<EnvironmentCheck> checkers = applicationContext.getBeansOfType( EnvironmentCheck.class ).values();

        StopWatch stopWatch = new StopWatch();
        stopWatch.reset();
        stopWatch.start();

        if ( checkers != null )
        {
            List<String> violations = new ArrayList<String>();

            for ( EnvironmentCheck check : checkers )
            {
                check.validateEnvironment( violations );
            }

            if ( !violations.isEmpty() )
            {
                StringBuilder msg = new StringBuilder();
                msg.append( "EnvironmentCheck Failure.\n" );
                msg.append( "======================================================================\n" );
                msg.append( " ENVIRONMENT FAILURE !! \n" );
                msg.append( "\n" );

                for ( String v : violations )
                {
                    msg.append( v ).append( "\n" );
                }

                msg.append( "\n" );
                msg.append( "======================================================================" );
                log.error( msg.toString() );
            }
        }

        stopWatch.stop();
        log.info( "time to execute all EnvironmentCheck: {} ms", stopWatch.getTime() );
    }
}
