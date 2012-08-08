package org.apache.archiva.redback.struts2.interceptor;

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
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.Interceptor;
import org.apache.archiva.redback.system.check.EnvironmentCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * EnvironmentCheckInterceptor
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 *
 */
@Controller( "redbackEnvironmentCheckInterceptor" )
@Scope( "prototype" )
public class EnvironmentCheckInterceptor
    implements Interceptor
{
    private static boolean checked = false;

    private Logger log = LoggerFactory.getLogger( EnvironmentCheckInterceptor.class );


    /**
     *
     */
    @Inject
    private List<EnvironmentCheck> checkers;

    public void destroy()
    {
        // no-op
    }

    @PostConstruct
    public void init()
    {

        if ( EnvironmentCheckInterceptor.checked )
        {
            // No need to check twice.
            return;
        }

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

        EnvironmentCheckInterceptor.checked = true;
    }

    public String intercept( ActionInvocation invocation )
        throws Exception
    {
        // A no-op here. Work for this intereceptor is done in init().
        return invocation.invoke();
    }
}
