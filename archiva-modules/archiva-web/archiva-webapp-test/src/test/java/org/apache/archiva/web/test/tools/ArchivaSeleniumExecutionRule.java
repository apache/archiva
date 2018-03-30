package org.apache.archiva.web.test.tools;
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

import org.apache.archiva.web.test.parent.AbstractSeleniumTest;
import org.junit.rules.MethodRule;
import org.junit.runner.Description;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Olivier Lamy
 */
public class ArchivaSeleniumExecutionRule
    implements MethodRule //TestRule
{

    final Logger log = LoggerFactory.getLogger( ArchivaSeleniumExecutionRule.class );


    public Statement apply( Statement base, FrameworkMethod method, Object target )
    {
        try
        {
            log.info("Test "+method.getDeclaringClass().getName()+"#"+method.getName());
            ( (AbstractSeleniumTest) target ).open();
            method.getMethod().invoke( target );
        }
        catch ( Throwable e )
        {

            log.info("Exception thrown in Selenium test: "+e.getClass().getName()+" - "+e.getMessage());
            log.info("Method "+method.getName());
            String fileName =
                ( (AbstractSeleniumTest) target ).captureScreenShotOnFailure( e, method.getMethod().getName(),
                                                                              target.getClass().getName() );
            log.info("Screenshot: "+fileName);
            throw new RuntimeException( e.getMessage() + " see screenShot file:" + fileName, e );
        }
        finally
        {
            ( (AbstractSeleniumTest) target ).close();

        }
        return new Statement()
        {
            @Override
            public void evaluate()
                throws Throwable
            {
                // no op
            }
        };
    }

    public Statement apply( Statement base, Description description )
    {
        return base;
    }
}
