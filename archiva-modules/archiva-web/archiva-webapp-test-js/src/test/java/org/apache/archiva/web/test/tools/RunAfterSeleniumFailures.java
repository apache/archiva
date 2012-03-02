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


import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Olivier Lamy
 */
public class RunAfterSeleniumFailures
    extends Statement
{

    private final Statement next;

    private final Object target;

    private final List<FrameworkMethod> afterFailures;

    public RunAfterSeleniumFailures( Statement next, List<FrameworkMethod> afterFailures, Object target )
    {
        this.next = next;
        this.afterFailures = afterFailures;
        this.target = target;
    }

    @Override
    public void evaluate()
        throws Throwable
    {
        List<Throwable> errors = new ArrayList<Throwable>();
        errors.clear();
        try
        {
            next.evaluate();
        }
        catch ( Throwable t )
        {
            errors.add( t );
            for ( FrameworkMethod each : afterFailures )
            {
                try
                {
                    each.invokeExplosively( target, t );
                }
                catch ( Throwable t2 )
                {
                    errors.add( t2 );
                }
            }
        }
        if ( errors.isEmpty() )
        {
            return;
        }
        if ( errors.size() == 1 )
        {
            throw errors.get( 0 );
        }
        throw new MultipleFailureException( errors );
    }

}
