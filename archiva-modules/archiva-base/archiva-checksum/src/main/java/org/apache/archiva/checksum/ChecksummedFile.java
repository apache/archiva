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

import org.apache.archiva.common.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.ValidationException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.archiva.checksum.ChecksumValidationException.ValidationError.BAD_CHECKSUM_FILE;
import static org.apache.archiva.checksum.ChecksumValidationException.ValidationError.BAD_CHECKSUM_FILE_REF;

/**
 * ChecksummedFile
 * <p>Terminology:</p>
 * <dl>
 * <dt>Checksum File</dt>
 * <dd>The file that contains the previously calculated checksum value for the reference file.
 * This is a text file with the extension ".sha1" or ".md5", and contains a single entry
 * consisting of an optional reference filename, and a checksum string.
 * </dd>
 * <dt>Reference File</dt>
 * <dd>The file that is being referenced in the checksum file.</dd>
 * </dl>
 */
public class ChecksummedFile
{

    private static Charset FILE_ENCODING = Charset.forName( "UTF-8" );

    private final Logger log = LoggerFactory.getLogger( ChecksummedFile.class );

    private static final Pattern METADATA_PATTERN = Pattern.compile( "maven-metadata-\\S*.xml" );

    private final Path referenceFile;

    /**
     * Construct a ChecksummedFile object.
     *
     * @param referenceFile
     */
    public ChecksummedFile( final Path referenceFile )
    {
        this.referenceFile = referenceFile;
    }


    public static ChecksumReference getFromChecksumFile( Path checksumFile )
    {
        ChecksumAlgorithm alg = ChecksumAlgorithm.getByExtension( checksumFile );
        ChecksummedFile file = new ChecksummedFile( getReferenceFile( checksumFile ) );
        return new ChecksumReference( file, alg, checksumFile );
    }

    private static Path getReferenceFile( Path checksumFile )
    {
        String fileName = checksumFile.getFileName( ).toString( );
        return checksumFile.resolveSibling( fileName.substring( 0, fileName.lastIndexOf( '.' ) ) );
    }

    /**
     * Calculate the checksum based on a given checksum.
     *
     * @param checksumAlgorithm the algorithm to use.
     * @return the checksum string for the file.
     * @throws IOException if unable to calculate the checksum.
     */
    public String calculateChecksum( ChecksumAlgorithm checksumAlgorithm )
        throws IOException
    {

        Checksum checksum = new Checksum( checksumAlgorithm );
        checksum.update( referenceFile );
        return checksum.getChecksum( );
    }

    /**
     * Creates a checksum file of the provided referenceFile.
     *
     * @param checksumAlgorithm the hash to use.
     * @return the checksum File that was created.
     * @throws IOException if there was a problem either reading the referenceFile, or writing the checksum file.
     */
    public Path createChecksum( ChecksumAlgorithm checksumAlgorithm )
        throws IOException
    {
        Path checksumFile = referenceFile.resolveSibling( referenceFile.getFileName( ) + "." + checksumAlgorithm.getExt( ).get( 0 ) );
        Files.deleteIfExists( checksumFile );
        String checksum = calculateChecksum( checksumAlgorithm );
        Files.write( checksumFile, //
            ( checksum + "  " + referenceFile.getFileName( ).toString( ) ).getBytes( ), //
            StandardOpenOption.CREATE_NEW );
        return checksumFile;
    }

    /**
     * Get the checksum file for the reference file and hash.
     *
     * @param checksumAlgorithm the hash that we are interested in.
     * @return the checksum file to return
     */
    public Path getChecksumFile( ChecksumAlgorithm checksumAlgorithm )
    {
        for ( String ext : checksumAlgorithm.getExt( ) )
        {
            Path file = referenceFile.resolveSibling( referenceFile.getFileName( ) + "." + checksumAlgorithm.getExt( ) );
            if ( Files.exists( file ) )
            {
                return file;
            }
        }
        return referenceFile.resolveSibling( referenceFile.getFileName( ) + "." + checksumAlgorithm.getExt( ).get( 0 ) );
    }

    /**
     * <p>
     * Given a checksum file, check to see if the file it represents is valid according to the checksum.
     * </p>
     * <p>
     * NOTE: Only supports single file checksums of type MD5 or SHA1.
     * </p>
     *
     * @param algorithm the algorithms to check for.
     * @return true if the checksum is valid for the file it represents. or if the checksum file does not exist.
     * @throws IOException if the reading of the checksumFile or the file it refers to fails.
     */
    public boolean isValidChecksum( ChecksumAlgorithm algorithm) throws ChecksumValidationException
    {
        return isValidChecksum( algorithm, false );
    }
    public boolean isValidChecksum( ChecksumAlgorithm algorithm, boolean throwExceptions )
        throws ChecksumValidationException
    {
        return isValidChecksums( Arrays.asList( algorithm ), throwExceptions );
    }

    /**
     * Of any checksum files present, validate that the reference file conforms
     * the to the checksum.
     *
     * @param algorithms the algorithms to check for.
     * @return true if the checksums report that the the reference file is valid, false if invalid.
     */
    public boolean isValidChecksums( List<ChecksumAlgorithm> algorithms) throws ChecksumValidationException
    {
        return isValidChecksums( algorithms, false );
    }

    /**
     * Checks if the checksums are valid for the referenced file.
     * This method throws only exceptions, if throwExceptions is true. Otherwise false will be returned instead.
     * @param algorithms The algorithms to verify
     * @param throwExceptions If true, exceptions will be thrown, otherwise false will be returned, if a exception occurred.
     * @return True, if it is valid, otherwise false.
     * @throws ChecksumValidationException
     */
    public boolean isValidChecksums( List<ChecksumAlgorithm> algorithms, boolean throwExceptions) throws ChecksumValidationException
    {

        List<Checksum> checksums = new ArrayList<>( algorithms.size() );
        // Create checksum object for each algorithm.
        for ( ChecksumAlgorithm checksumAlgorithm : algorithms )
        {
            Path checksumFile = getChecksumFile( checksumAlgorithm );

            // Only add algorithm if checksum file exists.
            if ( Files.exists( checksumFile ) )
            {
                checksums.add( new Checksum( checksumAlgorithm ) );
            }
        }

        // Any checksums?
        if ( checksums.isEmpty( ) )
        {
            // No checksum objects, no checksum files, default to is invalid.
            return false;
        }

        // Parse file once, for all checksums.
        try
        {
            Checksum.update( checksums, referenceFile );
        }
        catch ( ChecksumValidationException e )
        {
            log.warn( "Unable to update checksum:{}", e.getMessage( ) );
            if (throwExceptions) {
                throw e;
            } else {
                return false;
            }
        }

        boolean valid = true;

        // check the checksum files
        try
        {
            for ( Checksum checksum : checksums )
            {
                ChecksumAlgorithm checksumAlgorithm = checksum.getAlgorithm( );
                Path checksumFile = getChecksumFile( checksumAlgorithm );

                String expectedChecksum = parseChecksum( checksumFile, checksumAlgorithm, referenceFile.getFileName( ).toString( ), FILE_ENCODING );

                if ( !checksum.compare( expectedChecksum ) )
                {
                    valid = false;
                }
            }
        }
        catch ( ChecksumValidationException e )
        {
            log.warn( "Unable to read / parse checksum: {}", e.getMessage( ) );
            if (throwExceptions) {
                throw e;
            } else
            {
                return false;
            }
        }

        return valid;
    }

    public Path getReferenceFile( )
    {
        return referenceFile;
    }



    public boolean fixChecksum(ChecksumAlgorithm algorithm) {
        return fixChecksums( Arrays.asList(algorithm) );
    }
    /**
     * Fix or create checksum files for the reference file.
     *
     * @param algorithms the hashes to check for.
     * @return true if checksums were created successfully.
     */
    public boolean fixChecksums( List<ChecksumAlgorithm> algorithms )
    {
        List<Checksum> checksums = new ArrayList<>( algorithms.size() );
        // Create checksum object for each algorithm.
        for ( ChecksumAlgorithm checksumAlgorithm : algorithms )
        {
            checksums.add( new Checksum( checksumAlgorithm ) );
        }

        // Any checksums?
        if ( checksums.isEmpty( ) )
        {
            // No checksum objects, no checksum files, default to is valid.
            return true;
        }

        try
        {
            // Parse file once, for all checksums.
            Checksum.update( checksums, referenceFile );
        }
        catch ( ChecksumValidationException e )
        {
            log.warn( e.getMessage( ), e );
            return false;
        }

        boolean valid = true;

        // check the hash files
        for ( Checksum checksum : checksums )
        {
            ChecksumAlgorithm checksumAlgorithm = checksum.getAlgorithm( );
            try
            {
                Path checksumFile = getChecksumFile( checksumAlgorithm );
                if ( Files.exists( checksumFile ) )
                {
                    String expectedChecksum;
                    try
                    {
                        expectedChecksum = parseChecksum( checksumFile, checksumAlgorithm, referenceFile.getFileName( ).toString( ), FILE_ENCODING );
                    } catch (ChecksumValidationException ex) {
                        expectedChecksum = "";
                    }

                    if ( !checksum.compare( expectedChecksum ) )
                    {
                        // create checksum (again)
                        writeChecksumFile( checksumFile, FILE_ENCODING, checksum.getChecksum( ) );
                    }
                }
                else
                {
                    writeChecksumFile( checksumFile, FILE_ENCODING, checksum.getChecksum( ) );
                }
            }
            catch ( ChecksumValidationException e )
            {
                log.warn( e.getMessage( ), e );
                valid = false;
            }
        }

        return valid;

    }

    private void writeChecksumFile( Path checksumFile, Charset encoding, String checksumHex )
    {
        FileUtils.writeStringToFile( checksumFile, FILE_ENCODING, checksumHex + "  " + referenceFile.getFileName( ).toString( ) );
    }

    private boolean isValidChecksumPattern( String filename, String path )
    {
        // check if it is a remote metadata file

        Matcher m = METADATA_PATTERN.matcher( path );
        if ( m.matches( ) )
        {
            return filename.endsWith( path ) || ( "-".equals( filename ) ) || filename.endsWith( "maven-metadata.xml" );
        }

        return filename.endsWith( path ) || ( "-".equals( filename ) );
    }

    /**
     * Parse a checksum string.
     * <p>
     * Validate the expected path, and expected checksum algorithm, then return
     * the trimmed checksum hex string.
     * </p>
     *
     * @param checksumFile The file where the checksum is stored
     * @param expectedHash The checksum algorithm to check
     * @param expectedPath The filename of the reference file
     * @return
     * @throws IOException
     */
    public String parseChecksum( Path checksumFile, ChecksumAlgorithm expectedHash, String expectedPath, Charset encoding )
        throws ChecksumValidationException
    {
        ChecksumFileContent fc = parseChecksumFile( checksumFile, expectedHash, encoding );
        if ( fc.isFormatMatch() && !isValidChecksumPattern( fc.getFileReference( ), expectedPath ) )
        {
            throw new ChecksumValidationException(BAD_CHECKSUM_FILE_REF,
                "The file reference '" + fc.getFileReference( ) + "' in the checksum file does not match expected file: '" + expectedPath + "'" );
        } else if (!fc.isFormatMatch()) {
            throw new ChecksumValidationException( BAD_CHECKSUM_FILE, "The checksum file content could not be parsed: "+checksumFile );
        }
        return fc.getChecksum( );
    }

    public ChecksumFileContent parseChecksumFile( Path checksumFile, ChecksumAlgorithm expectedHash, Charset encoding )
    {
        ChecksumFileContent fc = new ChecksumFileContent( );
        String rawChecksumString = FileUtils.readFileToString( checksumFile, encoding );
        String trimmedChecksum = rawChecksumString.replace( '\n', ' ' ).trim( );

        // Free-BSD / openssl
        String regex = expectedHash.getType( ) + "\\s*\\(([^)]*)\\)\\s*=\\s*([a-fA-F0-9]+)";
        Matcher m = Pattern.compile( regex ).matcher( trimmedChecksum );
        if ( m.matches( ) )
        {
            fc.setFileReference( m.group( 1 ) );
            fc.setChecksum( m.group( 2 ) );
            fc.setFormatMatch( true );
        }
        else
        {
            // GNU tools
            m = Pattern.compile( "([a-fA-F0-9]+)\\s+\\*?(.+)" ).matcher( trimmedChecksum );
            if ( m.matches( ) )
            {
                fc.setFileReference( m.group( 2 ) );
                fc.setChecksum( m.group( 1 ) );
                fc.setFormatMatch( true );
            }
            else
            {
                fc.setFileReference( "" );
                fc.setChecksum( trimmedChecksum );
                fc.setFormatMatch( false );
            }
        }
        return fc;
    }
}
