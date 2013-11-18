package org.apache.archiva.metadata.repository.cassandra.model;

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


import com.netflix.astyanax.serializers.AbstractSerializer;
import com.netflix.astyanax.serializers.ComparatorType;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;


/**
 * For Huge String we use a deflate compression
 * @author Olivier Lamy
 * @since 2.0.0
 */
public class DeflateStringSerializer
    extends AbstractSerializer<String>
{

    private Logger logger = LoggerFactory.getLogger( getClass() );

    private static final String UTF_8 = "UTF-8";

    private static final DeflateStringSerializer instance = new DeflateStringSerializer();

    private static final Charset charset = Charset.forName( UTF_8 );

    public static DeflateStringSerializer get()
    {
        return instance;
    }

    @Override
    public ByteBuffer toByteBuffer( String obj )
    {
        if ( obj == null )
        {
            return null;
        }

        try
        {
            byte[] bytes = compressWithDeflate( StringUtils.getBytesUtf8( obj ) );
            return ByteBuffer.wrap( bytes );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( "Fail to compress column data", e );
        }
    }

    @Override
    public String fromByteBuffer( ByteBuffer byteBuffer )
    {
        if ( byteBuffer == null )
        {
            return null;
        }

        ByteBuffer dup = byteBuffer.duplicate();
        try
        {
            String str = getFromDeflateBytes( dup.array() );
            return str;
        }
        catch ( IOException e )
        {
            throw new RuntimeException( "Fail to decompress column data", e );
        }

    }

    public String getFromDeflateBytes( byte[] bytes )
        throws IOException
    {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream( bytes );
        InflaterInputStream inflaterInputStream = new InflaterInputStream( byteArrayInputStream );
        return IOUtils.toString( inflaterInputStream );
    }

    public byte[] compressWithDeflate( byte[] unCompress )
        throws IOException
    {
        try
        {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            DeflaterOutputStream out = new DeflaterOutputStream( buffer, new Deflater( Deflater.BEST_COMPRESSION ) );
            out.write( unCompress );
            out.finish();
            ByteArrayInputStream bais = new ByteArrayInputStream( buffer.toByteArray() );
            byte[] res = IOUtils.toByteArray( bais );
            return res;
        }
        catch ( IOException e )
        {
            logger.debug( "IOException in compressStringWithDeflate", e );
            throw e;
        }

    }

    @Override
    public ComparatorType getComparatorType()
    {
        return ComparatorType.BYTESTYPE;
    }

    @Override
    public ByteBuffer fromString( String str )
    {
        return instance.fromString( str );
    }

    @Override
    public String getString( ByteBuffer byteBuffer )
    {
        return instance.getString( byteBuffer );
    }

}
