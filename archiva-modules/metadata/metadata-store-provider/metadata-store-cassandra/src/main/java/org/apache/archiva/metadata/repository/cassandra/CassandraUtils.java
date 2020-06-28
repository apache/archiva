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
import me.prettyprint.cassandra.service.template.ColumnFamilyUpdater;
import me.prettyprint.hector.api.Serializer;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import org.apache.archiva.metadata.repository.cassandra.model.ColumnNames;
import org.apache.commons.lang3.StringUtils;

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
            SerializerTypeInferer.getSerializer( name ), //
            SerializerTypeInferer.getSerializer( value ) );
    }

    public static String getStringValue( ColumnSlice<String, String> columnSlice, ColumnNames columnName )
    {
        return getStringValue( columnSlice, columnName.toString() );
    }

    public static String getStringValue( ColumnSlice<String, String> columnSlice, String columnName )
    {
        if ( StringUtils.isEmpty( columnName ) )
        {
            return null;
        }

        HColumn<String, String> hColumn = columnSlice.getColumnByName( columnName );
        return hColumn == null ? null : hColumn.getValue();
    }

    public static Long getLongValue( ColumnSlice<String, Long> columnSlice, String columnName )
    {
        if ( StringUtils.isEmpty( columnName ) )
        {
            return null;
        }

        HColumn<String, Long> hColumn = columnSlice.getColumnByName( columnName );
        return hColumn == null ? null : hColumn.getValue();
    }

    public static <T> String getAsStringValue( ColumnSlice<String, T> columnSlice, String columnName )
    {
        StringSerializer ss = StringSerializer.get();
        if ( StringUtils.isEmpty( columnName ) )
        {
            return null;
        }

        HColumn<String, T> hColumn = columnSlice.getColumnByName( columnName );
        return hColumn == null ? null : ss.fromByteBuffer( hColumn.getValueBytes() );
    }

    public static Long getAsLongValue( ColumnSlice<String, String> columnSlice, String columnName )
    {
        LongSerializer ls = LongSerializer.get();
        if ( StringUtils.isEmpty( columnName ) )
        {
            return null;
        }

        HColumn<String, String> hColumn = columnSlice.getColumnByName( columnName );
        return hColumn == null ? null : ls.fromByteBuffer( hColumn.getValueBytes() );
    }

    public static void addInsertion( Mutator<String> mutator, String key, String columnFamily, String columnName,
                                     String value )
    {
        if ( value != null )
        {
            mutator.addInsertion( key, columnFamily, column( columnName, value ) );
        }
    }

    /**
     * null check on the value to prevent {@link java.lang.IllegalArgumentException}
     * @param updater
     * @param columnName
     * @param value
     */
    public static void addUpdateStringValue(ColumnFamilyUpdater<String,String> updater, String columnName, String value )
    {
        if (value == null)
        {
            return;
        }
        updater.setString( columnName, value );

    }

    private CassandraUtils()
    {
        // no-op
    }

}
