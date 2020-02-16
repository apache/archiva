package org.apache.archiva.common.utils;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Utility class for file manipulation
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class FileUtils {
    private static final Logger log = LoggerFactory.getLogger(FileUtils.class);

    /**
     * Deletes the directory recursively and quietly.
     *
     * @param dir
     */
    public static void deleteQuietly(Path dir) {
        try(Stream<Path> stream = Files.walk(dir)) {
            stream
                .sorted( Comparator.reverseOrder() )
                .forEach(file -> {
                        try {
                            Files.delete(file);
                        } catch (IOException e) {
                            // Ignore this
                            if (log.isDebugEnabled()) {
                                log.debug( "Exception during file delete: {}", e.getMessage( ), e );
                            }
                        }

                    });
        } catch (IOException e) {
            // Ignore this
        }


    }

    public static IOStatus deleteDirectoryWithStatus(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            IOStatus status = new IOStatus( );
            status.addError( dir, new FileNotFoundException( "Directory not found " + dir ) );
            return status;
        }
        if (!Files.isDirectory(dir)) {
            IOStatus status = new IOStatus( );
            status.addError(dir, new IOException("Given path is not a directory " + dir));
        }
        try( Stream<Path> stream = Files.walk(dir)) {
                return stream
                    .sorted( Comparator.reverseOrder() )
                .map(file ->
                {
                    try {
                        Files.delete(file);
                        return new FileStatus( file, StatusResult.DELETED );
                    } catch (UncheckedIOException e) {
                        log.warn("File could not be deleted {}", file);
                        return new FileStatus( file, e.getCause( ) );
                    }
                    catch ( IOException e )
                    {
                        return new FileStatus( file, e );
                    }
                }).collect( IOStatus::new, IOStatus::accumulate, IOStatus::combine );
        } catch (UncheckedIOException e) {
            throw new IOException("File deletion failed ", e);
        }
    }

    public static void deleteDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        if (!Files.isDirectory(dir)) {
            throw new IOException("Given path is not a directory " + dir);
        }
        boolean result = true;
        try(Stream<Path> stream = Files.walk(dir)) {
            result = stream
                .sorted( Comparator.reverseOrder() )
                .map(file ->
                    {
                        try {
                            Files.delete(file);
                            return Optional.of(Boolean.TRUE);
                        } catch (UncheckedIOException | IOException e) {
                            log.warn("File could not be deleted {}: {}", file, e.getMessage());
                            return Optional.empty();
                        }

                    }).allMatch(Optional::isPresent);
        } catch (UncheckedIOException e) {
            throw new IOException("File deletion failed ", e);
        }
        if (!result) {
            throw new IOException("Error during recursive delete of " + dir.toAbsolutePath());
        }
    }

    public static String readFileToString(Path file, Charset encoding) {
        try {
            return new String(Files.readAllBytes(file), encoding);
        } catch (IOException e) {
            log.error("Could not read from file {}", file);
            return "";
        }
    }

    public static void writeStringToFile(Path file, Charset encoding, String value) {
        try {
            Files.write(file, value.getBytes(encoding), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            log.error("Could not write to file {}", file);
        }
    }

    /**
     * Return the base directory
     *
     * @return
     */
    public static String getBasedir() {
        String basedir = System.getProperty("basedir");
        if (basedir == null) {
            basedir = Paths.get("").toAbsolutePath().toString();
        }

        return basedir;
    }

    /**
     * This checks, if the given child is a absolute path. If this is the case
     * the relative path is used.
     *
     * @param parent The parent directory
     * @param child  The child
     * @return The path parent/child
     */
    public Path resolveNonAbsolute(Path parent, String child) {
        Path childPath = Paths.get(child);
        if (childPath.isAbsolute()) {
            return parent.resolve(childPath.getNameCount() > 0 ? childPath.subpath(0, childPath.getNameCount()) : Paths.get(""));
        } else {
            return parent.resolve(child);
        }
    }

    public static void unzipFileEntry(ZipFile file, ZipEntry entry, Path destinationPath) throws IOException {
        InputStream is = file.getInputStream(entry);
        BufferedInputStream bis = new BufferedInputStream(is);
        Path uncompressedFilePath = destinationPath.resolve(entry.getName());
        Path parentPath = uncompressedFilePath.getParent();
        if (!Files.exists(parentPath)) {
            Files.createDirectories(parentPath);
        }
        Files.createFile(uncompressedFilePath);
        OutputStream fileOutput = Files.newOutputStream(uncompressedFilePath);
        while (bis.available() > 0) {
            fileOutput.write(bis.read());
        }
        fileOutput.close();
    }


    /**
     * Unzips a file into a destination directory. It does not update the modification time according the
     * the date in the zip file. All subdirectories will be created if the zip file contains a directory structure.
     *
     * @param zipFile the path to the zip file
     * @param destinationPath the destination path where the files should be extracted.
     * @throws IOException if an error occurs during extract.
     */
    public static void unzip(Path zipFile, Path destinationPath) throws IOException {
        try (ZipFile file = new ZipFile(zipFile.toFile())) {
            file.stream().forEach(e -> {
                try {
                    if (e.isDirectory()) {
                        Files.createDirectories(destinationPath.resolve(e.getName()));
                    } else {
                        unzipFileEntry(file, e, destinationPath);
                    }
                } catch (IOException ex) {
                    log.error("Error occured during unzip of zipFile={}, entry={}. Message: {}", zipFile, e.getName(), ex.getMessage());
                }
            });
        }
    }
}
