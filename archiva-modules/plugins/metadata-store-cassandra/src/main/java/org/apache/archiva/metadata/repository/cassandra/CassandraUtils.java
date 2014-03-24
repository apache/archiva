package org.apache.archiva.metadata.repository.cassandra;

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

import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.SerializerTypeInferer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Serializer;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.factory.HFactory;
import org.apache.commons.lang.StringUtils;

/**
 * @author Olivier Lamy
 * @since 2.0.0
 */
public class CassandraUtils
{

    private static final String EMPTY_VALUE = "";

    public static final String SEPARATOR = "->";

    public static String generateKey( final String... bases )
    {
        final StringBuilder builder = new StringBuilder();
        if ( bases == null || bases.length == 0 )
        {
            return builder.toString();
        }

        for ( final String s : bases )
        {
            if ( s != null )
            {
                builder.append( s );
            }
            else
            {
                builder.append( EMPTY_VALUE );
            }
            builder.append( SEPARATOR );
        }
        if ( builder.length() > 0 )
        {
            builder.setLength( builder.length() - SEPARATOR.length() );
        }
        return builder.toString();
    }

    public static <A, B> HColumn<A, B> column( final A name, final B value )
    {
        return HFactory.createColumn( name, //
                                      value, //
                                      (Serializer<A>) SerializerTypeInferer.getSerializer( name ), //
                                      (Serializer<B>) SerializerTypeInferer.getSerializer( value ) );
    }

    public static String getStringValue( ColumnSlice<String, String> columnSlice, String columnName )
    {
        if ( StringUtils.isNotEmpty( columnName ) )
        {
            return null;
        }

        HColumn<String, String> hColumn = columnSlice.getColumnByName( columnName );
        return hColumn == null ? null : hColumn.getValue();
    }

    public static Long getLongValue( ColumnSlice<String, Long> columnSlice, String columnName )
    {
        if ( StringUtils.isNotEmpty( columnName ) )
        {
            return null;
        }

        HColumn<String, Long> hColumn = columnSlice.getColumnByName( columnName );
        return hColumn == null ? null : hColumn.getValue();
    }

    public static String getAsStringValue( ColumnSlice<String, Long> columnSlice, String columnName )
    {
        StringSerializer ss = StringSerializer.get();
        if ( StringUtils.isNotEmpty( columnName ) )
        {
            return null;
        }

        HColumn<String, Long> hColumn = columnSlice.getColumnByName( columnName );
        return hColumn == null ? null : ss.fromByteBuffer( hColumn.getValueBytes() );
    }

    public static Long getAsLongValue( ColumnSlice<String, String> columnSlice, String columnName )
    {
        LongSerializer ls = LongSerializer.get();
        if ( StringUtils.isNotEmpty( columnName ) )
        {
            return null;
        }

        HColumn<String, String> hColumn = columnSlice.getColumnByName( columnName );
        return hColumn == null ? null : ls.fromByteBuffer( hColumn.getValueBytes() );
    }

    private CassandraUtils()
    {
        // no-op
    }

}
