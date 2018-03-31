package org.apache.archiva.checksum;

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

import javax.xml.bind.DatatypeConverter;

/**
 * Hex - simple hex conversions. 
 *
 *
 */
public class Hex
{

    public static String encode( byte[] data )
    {
        try
        {
            return DatatypeConverter.printHexBinary( data ).trim( ).toLowerCase( );
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    public static String encode( String raw )
    {
        return encode( raw.getBytes() );
    }

    public static byte[] decode( String data ) {
        try
        {
            return DatatypeConverter.parseHexBinary( data.trim( ) );
        } catch (IllegalArgumentException e) {
            return new byte[0];
        }
    }
}
