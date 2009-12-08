package org.apache.maven.archiva.database.constraints;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.archiva.database.Constraint;
import org.apache.maven.archiva.database.DeclarativeConstraint;

/**
 * AbstractDeclarativeConstraint
 *
 * @version $Id$
 */
public abstract class AbstractDeclarativeConstraint
    implements DeclarativeConstraint
{
    protected String[] declImports;

    protected String[] declParams;

    protected String[] variables;

    protected Object[] params;

    protected int[] range;

    protected String sortDirection = Constraint.ASCENDING;

    public String getFilter()
    {
        return null;
    }

    public String getFetchLimits()
    {
        return null;
    }

    public String[] getDeclaredImports()
    {
        return declImports;
    }

    public String[] getDeclaredParameters()
    {
        return declParams;
    }

    public Object[] getParameters()
    {
        return params;
    }

    public String getSortDirection()
    {
        return sortDirection;
    }

    public String[] getVariables()
    {
        return variables;
    }

    public int[] getRange()
    {
        return range;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        AbstractDeclarativeConstraint that = (AbstractDeclarativeConstraint) o;

        if ( !Arrays.equals( declImports, that.declImports ) )
        {
            return false;
        }
        if ( !Arrays.equals( declParams, that.declParams ) )
        {
            return false;
        }
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if ( !Arrays.equals( params, that.params ) )
        {
            return false;
        }
        if ( !Arrays.equals( range, that.range ) )
        {
            return false;
        }
        if ( sortDirection != null ? !sortDirection.equals( that.sortDirection ) : that.sortDirection != null )
        {
            return false;
        }
        if ( !Arrays.equals( variables, that.variables ) )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = declImports != null ? Arrays.hashCode( declImports ) : 0;
        result = 31 * result + ( declParams != null ? Arrays.hashCode( declParams ) : 0 );
        result = 31 * result + ( variables != null ? Arrays.hashCode( variables ) : 0 );
        result = 31 * result + ( params != null ? Arrays.hashCode( params ) : 0 );
        result = 31 * result + ( range != null ? Arrays.hashCode( range ) : 0 );
        result = 31 * result + ( sortDirection != null ? sortDirection.hashCode() : 0 );
        return result;
    }

    @Override
    public String toString()
    {
        List<Integer> r = null;
        if ( range != null )
        {
            r = new ArrayList<Integer>();
            for ( int i : range )
            {
                r.add( i );
            }
        }
        return "AbstractDeclarativeConstraint{" + "declImports=" +
            ( declImports == null ? null : Arrays.asList( declImports ) ) + ", declParams=" +
            ( declParams == null ? null : Arrays.asList( declParams ) ) + ", variables=" +
            ( variables == null ? null : Arrays.asList( variables ) ) + ", params=" +
            ( params == null ? null : Arrays.asList( params ) ) + ", range=" + r + ", sortDirection='" + sortDirection +
            '\'' + '}';
    }
}
