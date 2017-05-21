package org.apache.archiva.test.utils;

/*
 * Copyright 2012 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.junit.runners.model.FrameworkMethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Generator of list of random test method
 * -Dorg.apache.archiva.test=n
 * n&lt;=0 default jdk behavior
 * n&gt;0 number of round of random collection
 *
 * @author Eric
 */
public class ListGenerator
{
    private static int MAXROUND = 10;

    private ListGenerator()
    {
    }

    static List<FrameworkMethod> getShuffleList( List<FrameworkMethod> computeTestMethods )
    {
        int testRound;
        try
        {
            testRound = Integer.valueOf( System.getProperty( "org.apache.archiva.test", "0" ) );
        }
        catch ( NumberFormatException nfe )
        {
            testRound = 0;
        }
        if ( testRound <= 0 ) // default list usage
        {
            return computeTestMethods;
        }
        if ( computeTestMethods == null )
        {
            return null;
        }

        List<FrameworkMethod> generated = new ArrayList<>();

        testRound = Math.min( MAXROUND, testRound );

        for ( int i = 0; i < testRound; i++ )
        {
            Collections.shuffle( computeTestMethods );
            generated.addAll( computeTestMethods );
        }
        // Collections.sort( generated, new FrameworkMethodComparator() );

        return generated;
    }

    /*private static class FrameworkMethodComparator
        implements Comparator<FrameworkMethod>
    {
        public int compare( FrameworkMethod frameworkMethod, FrameworkMethod frameworkMethod1 )
        {
            return frameworkMethod.getName().compareTo( frameworkMethod1.getName() );
        }
    }*/

}
