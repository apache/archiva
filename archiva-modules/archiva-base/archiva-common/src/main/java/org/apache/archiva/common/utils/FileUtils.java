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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;

/**
 *
 * Utility class for file manipulation
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class FileUtils
{
    /**
     * Deletes the directory recursively and quietly.
     *
     * @param dir
     */
    public static void deleteQuietly(Path dir) {
        try
        {
            Files.walk(dir)
                .sorted( Comparator.reverseOrder())
                .forEach( file ->  {
                    try
                    {
                        Files.delete( file );
                    }
                    catch ( IOException e )
                    {
                        // Ignore this
                    }

                });
        }
        catch ( IOException e )
        {
            // Ignore this
        }


    }

    public static void deleteDirectory( Path dir ) throws IOException
    {
        if (!Files.isDirectory( dir )) {
            throw new IOException("Given path is not a directory ");
        }
        boolean result = Files.walk(dir)
            .sorted( Comparator.reverseOrder())
            .map( file ->  {
                try
                {
                    Files.delete( file );
                    return Optional.of(Boolean.TRUE);
                }
                catch ( IOException e )
                {
                    return Optional.empty();
                }

            }).allMatch( Optional::isPresent );
        if (!result) {
            throw new IOException("Error during recursive delete of "+dir.toAbsolutePath());
        }
    }
}
