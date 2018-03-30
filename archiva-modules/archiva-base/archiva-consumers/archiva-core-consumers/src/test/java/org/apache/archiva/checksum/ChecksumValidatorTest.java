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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */

public class ChecksumValidatorTest
{

    @Test
    public void isValidChecksum( ) throws URISyntaxException, ChecksumValidationException
    {
        ChecksumValidator validator = new ChecksumValidator();
        String fileName = "checksum/checksumTest1.txt";
        List<String> exts = Arrays.asList( "md5", "sha1", "sha2", "sha3", "sha5" );
        for(String ext : exts)
        {
            Path hashFile = Paths.get( Thread.currentThread( ).getContextClassLoader( ).getResource( fileName + "."+ext ).toURI( ) );
            assertTrue( validator.isValidChecksum( hashFile ) );
        }
        fileName = "checksum/checksumTest2.txt";
        for(String ext : exts)
        {
            Path hashFile = Paths.get( Thread.currentThread( ).getContextClassLoader( ).getResource( fileName + "."+ext ).toURI( ) );
            assertTrue( validator.isValidChecksum( hashFile ) );
        }
    }

    @Test
    public void isInValidChecksum( ) throws URISyntaxException, ChecksumValidationException
    {
        ChecksumValidator validator = new ChecksumValidator();
        String fileName = "checksum/checksumTest3.txt";
        List<String> exts = Arrays.asList( "md5", "sha1", "sha2", "sha3", "sha5" );
        for(String ext : exts)
        {
            Path hashFile = Paths.get( Thread.currentThread( ).getContextClassLoader( ).getResource( fileName + "."+ext ).toURI( ) );
            assertFalse( validator.isValidChecksum( hashFile ) );
        }
    }

    @Test
    public void isInvalidExtension( ) throws URISyntaxException, ChecksumValidationException
    {
        ChecksumValidator validator = new ChecksumValidator();
        String fileName = "checksum/checksumTest1.txt";
        String ext = "md8";
        try
        {
            Path hashFile = Paths.get( Thread.currentThread( ).getContextClassLoader( ).getResource( fileName + "." + ext ).toURI( ) );
            validator.isValidChecksum( hashFile );
        } catch (ChecksumValidationException e) {
            assertEquals(ChecksumValidationException.ValidationError.INVALID_FORMAT, e.getErrorType());
        }
    }

    @Test
    public void computeFileDoesNotExist( ) throws URISyntaxException, ChecksumValidationException
    {
        ChecksumValidator validator = new ChecksumValidator();
        String fileName = "checksum/checksumTest4.txt";
        String ext = "md5";
        try
        {
            Path hashFile = Paths.get( Thread.currentThread( ).getContextClassLoader( ).getResource( fileName + "." + ext ).toURI( ) );
            validator.isValidChecksum( hashFile );
        } catch (ChecksumValidationException e) {
            assertEquals(ChecksumValidationException.ValidationError.READ_ERROR, e.getErrorType());
        }
    }

    @Test
    public void checksumFileDoesNotExist( ) throws URISyntaxException, ChecksumValidationException
    {
        ChecksumValidator validator = new ChecksumValidator();
        String fileName = "checksumTest5.txt";
        String ext = "md5";
        try
        {
            Path sibling = Paths.get( Thread.currentThread( ).getContextClassLoader( ).getResource( "checksum/checksumTest1.txt." + ext ).toURI( ) );
            Path hashFile = sibling.getParent().resolve(fileName);
            validator.isValidChecksum( hashFile );
        } catch (ChecksumValidationException e) {
            assertEquals(ChecksumValidationException.ValidationError.FILE_NOT_FOUND, e.getErrorType());
        }
    }

    @Test
    public void computeHash( ) throws URISyntaxException, NoSuchAlgorithmException, IOException, ChecksumValidationException
    {
        ChecksumValidator validator = new ChecksumValidator();
        Map<String, String> hashes = new HashMap<>( );
        hashes.put("md5","079fe13e970ae7311172df6657f36892");
        hashes.put("sha1", "01e14abba5401e1a63be468f9c3b723167f27dc8");
        hashes.put("sha2", "ae7278e7bdfd8d7c06f9b1932ddccdddb0061a58a893aec3f00932e53ef9c794");
        hashes.put("sha3", "a52efc629f256cd2b390f080ab7e23fc706ab9e2c8948cea2bd8504a70894f69f44f48e83c889edc82b40b673b575bad");
        hashes.put("sha5", "b2340bbf150403725fdf6a6f340a8a33bb9526bad7e0220f1dfea67d5a06217bc1d5c3a773b083ed8c9f5352c94ecc6da2a6d8a33ad0347566f0acc55e042fde");
        Path hashFile = Paths.get( Thread.currentThread( ).getContextClassLoader( ).getResource( "checksum/checksumTest1.txt").toURI( ) );

        for (String key : hashes.keySet()) {
            byte[] expectedSum = validator.convertFromHex( hashes.get(key) );
            byte[] computedSum = validator.computeHash( hashFile, key );
            assertArrayEquals( expectedSum, computedSum );
        }
    }

    @Test
    public void readHashFile( )
    {
    }
}