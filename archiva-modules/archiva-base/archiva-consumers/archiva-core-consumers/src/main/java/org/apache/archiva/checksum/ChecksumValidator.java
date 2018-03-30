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

import javax.xml.bind.DatatypeConverter;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.archiva.checksum.ChecksumValidationException.ValidationError.*;

/**
 * Class for validating checksums.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class ChecksumValidator
{
    private final int NOT_INITALIZED = 0;
    private final int INITIALIZING = 1;
    private final int INITIALIZED = 2;
    private AtomicInteger status = new AtomicInteger( NOT_INITALIZED );
    private static final Map<String, String> supportedTypes = new HashMap<>(  );

    public ChecksumValidator() {
        init();
    }

    private void init() {
        int val;
        if (status.compareAndSet( NOT_INITALIZED, INITIALIZING ))
        {
            try
            {
                supportedTypes.put( "md5", "MD5" );
                supportedTypes.put( "sha1", "SHA-1" );
                supportedTypes.put( "sha-1", "SHA-1" );
                supportedTypes.put( "sha2", "SHA-256" );
                supportedTypes.put( "sha256", "SHA-256" );
                supportedTypes.put( "sha-256", "SHA-256" );
                supportedTypes.put( "sha3", "SHA-384" );
                supportedTypes.put( "sha384", "SHA-384" );
                supportedTypes.put( "sha-384", "SHA-384" );
                supportedTypes.put( "sha5", "SHA-512" );
                supportedTypes.put( "sha512", "SHA-512" );
                supportedTypes.put( "sha-512", "SHA-512" );
            } finally
            {
                status.set(INITIALIZED);
            }
        } else if ((val = status.intValue())!=INITIALIZED) {
            do
            {
                try
                {
                    Thread.currentThread().sleep(100);
                    val = status.intValue();
                }
                catch ( InterruptedException e )
                {
                    // Ignore
                }
            } while(val!=INITIALIZED);
        }
    }

    public boolean isValidChecksum(Path checksumFile) throws ChecksumValidationException
    {
        String fileName = checksumFile.getFileName().toString();
        if (!Files.exists( checksumFile )) {
            throw new ChecksumValidationException( FILE_NOT_FOUND, "Checksum file does not exist: "+checksumFile );
        }
        String extension = fileName.substring( fileName.lastIndexOf( '.' )+1).toLowerCase();
        String digestType = this.supportedTypes.get(extension);
        if (digestType==null) {
            throw new ChecksumValidationException( INVALID_FORMAT, "The extension '"+extension+"' ist not known." );
        }
        Path checkFile = null;
        try
        {
            MessageDigest md = MessageDigest.getInstance( digestType );
            checkFile = getCheckFile( checksumFile );
            byte[] computedChecksum = computeHash( checkFile, md );
            byte[] readChecksum = readHashFile( checksumFile );
            return md.isEqual( computedChecksum, readChecksum );
        }
        catch ( NoSuchAlgorithmException e )
        {
            throw new ChecksumValidationException( DIGEST_ERROR, "The digest is not supported "+digestType  );
        }
        catch ( IOException e )
        {
            throw new ChecksumValidationException( READ_ERROR, "Error while computing the checksum of "+checkFile+": "+e.getMessage(), e);
        }
    }

    private Path getCheckFile(Path checksumFile) {
        String fileName = checksumFile.getFileName().toString();
        String newName = fileName.substring(0, fileName.lastIndexOf('.'));
        return checksumFile.getParent().resolve(newName);
    }

    public Set<String> getSupportedExtensions() {
        return supportedTypes.keySet();
    }

    public byte[] computeHash(Path file, MessageDigest digest) throws IOException
    {
        byte[] result;
        try(FileChannel inChannel = FileChannel.open( file, StandardOpenOption.READ ))
        {
            MappedByteBuffer buffer = inChannel.map( FileChannel.MapMode.READ_ONLY, 0, inChannel.size( ) );
            digest.update( buffer );
            result = digest.digest( );
        }
        return result;
    }

    public byte[] computeHash(Path file, String type) throws ChecksumValidationException, NoSuchAlgorithmException, IOException
    {
        if (!supportedTypes.containsKey( type )) {
            throw new ChecksumValidationException( INVALID_FORMAT );
        }
        return computeHash( file, MessageDigest.getInstance( supportedTypes.get(type) ) );
    }

    public byte[] readHashFile(Path file) throws IOException
    {
        StringBuilder sb = new StringBuilder(  );
        try(BufferedReader reader = Files.newBufferedReader( file, StandardCharsets.US_ASCII )){
            int ci;
            while((ci = reader.read()) != -1) {
                char c = (char)ci;
                if (Character.isWhitespace( c )) {
                    break;
                } else {
                    sb.append(c);
                }
            }
            return convertFromHex( sb.toString() );
        }
    }

    protected String convertToHex(byte[] array) {
        return DatatypeConverter.printHexBinary( array ).trim().toLowerCase();
    }

    protected byte[] convertFromHex(String checksum) {
        return DatatypeConverter.parseHexBinary( checksum.trim().toLowerCase() );
    }


}
