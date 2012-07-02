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
import java.util.Comparator;
import java.util.List;

/**
 * @author Eric
 */
public class ListGenerator
{
    private static int MAXROUND = 1;

    private ListGenerator()
    {
    }

    static List<FrameworkMethod> getShuffleList( List<FrameworkMethod> computeTestMethods )
    {
        String javaSpecVersion = System.getProperty( "java.specification.version" );
        // 1.6 1.5 version not shuffled to allow build
        if ( javaSpecVersion.equals( "1.6" ) || javaSpecVersion.equals( "1.5" ) )
        {
            return computeTestMethods;
        }
        if ( computeTestMethods == null )
        {
            return null;
        }
        List<FrameworkMethod> generated = new ArrayList<FrameworkMethod>( computeTestMethods );

        Collections.sort( generated, new FrameworkMethodComparator() );

        // 1.7 and more generated shuffled list
        // double test method to have more change of failure
        /*for ( int i = 0; i < MAXROUND; i++ )
        {
            Collections.shuffle( computeTestMethods );
            generated.addAll( computeTestMethods );
        }*/
        //generated.add( computeTestMethods.get( 0 ) );

        //Collections.shuffle( computeTestMethods );
        //generated.addAll( computeTestMethods );

        return generated;
    }

    private static class FrameworkMethodComparator
        implements Comparator<FrameworkMethod>
    {
        public int compare( FrameworkMethod frameworkMethod, FrameworkMethod frameworkMethod1 )
        {
            return frameworkMethod.getName().compareTo( frameworkMethod1.getName() );
        }
    }

}
