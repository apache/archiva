package org.apache.maven.repository.indexing.query;

import junit.framework.*;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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

/**
 *
 * @author Edwin Punzalan
 */
public class QueryTest extends TestCase
{
    public void testSinglePhraseQueryObject()
    {
        SinglePhraseQuery query = new SinglePhraseQuery( "Field", "Value" );
        assertTrue( query instanceof Query );
        assertEquals( "Field", query.getField() );
        assertEquals( "Value", query.getValue() );
    }
    
    public void testCompoundQueries()
    {
        RequiredQuery rQuery = new RequiredQuery();
        assertTrue( rQuery instanceof Query );
        rQuery.add( new SinglePhraseQuery( "r1Field", "r1Value" ) );
        rQuery.add( new SinglePhraseQuery( "r2Field", "r2Value" ) );
        
        OptionalQuery oQuery = new OptionalQuery();
        oQuery.add( new SinglePhraseQuery( "oField", "oValue" ) );
        
        RequiredQuery all = new RequiredQuery();
        all.add( rQuery );
        all.add( oQuery );
        assertEquals( 2, all.getQueryList().size() );
        
        for( int ctr = 0; ctr < all.getQueryList().size(); ctr++ )
        {
            Query query = (Query) all.getQueryList().get( ctr );
            switch ( ctr )
            {
                case 0:
                    assertTrue( query instanceof RequiredQuery );
                    rQuery = (RequiredQuery) query;
                    assertEquals( 2, rQuery.getQueryList().size() );
                    query = (Query) rQuery.getQueryList().get( 0 );
                    assertTrue( query instanceof SinglePhraseQuery );
                    SinglePhraseQuery sQuery = (SinglePhraseQuery) query;
                    assertEquals( "r1Field", sQuery.getField() );
                    assertEquals( "r1Value", sQuery.getValue() );
                    query = (Query) rQuery.getQueryList().get( 1 );
                    assertTrue( query instanceof SinglePhraseQuery );
                    sQuery = (SinglePhraseQuery) query;
                    assertEquals( "r2Field", sQuery.getField() );
                    assertEquals( "r2Value", sQuery.getValue() );
                    break;
                case 1:
                    assertTrue( query instanceof OptionalQuery );
                    oQuery = (OptionalQuery) query;
                    assertEquals( 1, oQuery.getQueryList().size() );
                    query = (Query) oQuery.getQueryList().get( 0 );
                    assertTrue( query instanceof SinglePhraseQuery );
                    sQuery = (SinglePhraseQuery) query;
                    assertEquals( "oField", sQuery.getField() );
                    assertEquals( "oValue", sQuery.getValue() );
                    break;
            }
        }
    }
}
