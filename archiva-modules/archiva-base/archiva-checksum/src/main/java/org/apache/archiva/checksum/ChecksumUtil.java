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

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class that handles multiple checksums for a single file.
 */
public class ChecksumUtil {


    static final int BUFFER_SIZE = 32768;

    public static void update(List<Checksum> checksumList, Path file ) throws IOException {
        long fileSize;
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ )) {
            fileSize = channel.size();
            long pos = 0;
            while (pos < fileSize) {
                long bufferSize = Math.min(BUFFER_SIZE, fileSize - pos);
                MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, pos, bufferSize);
                for (Checksum checksum : checksumList) {
                    checksum.update(buffer);
                    buffer.rewind();
                }
                fileSize = channel.size();
                pos += BUFFER_SIZE;
            }
            for (Checksum checksum : checksumList) {
                checksum.finish();
            }
        }
    }

    public static void update(Checksum checksum, Path file)
        throws IOException
    {
        long fileSize;
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ )) {
            fileSize = channel.size();
            long pos = 0;
            while (pos<fileSize)
            {
                long bufferSize = Math.min(BUFFER_SIZE, fileSize-pos);
                MappedByteBuffer buffer = channel.map( FileChannel.MapMode.READ_ONLY, pos, bufferSize);
                checksum.update( buffer );
                buffer.rewind();
                fileSize = channel.size();
                pos += BUFFER_SIZE;
            }
            checksum.finish();
        }
    }

    public static List<Checksum> initializeChecksums(Path file, List<ChecksumAlgorithm> checksumAlgorithms) throws IOException {
        final List<Checksum> checksums = newChecksums(checksumAlgorithms);
        update(checksums, file);
        return checksums;
    }

    /**
     * Returns the list of configured checksum types.
     *
     * @param checksumTypes The list of checksum strings
     * @return The list of checksum objects
     */
    public static List<ChecksumAlgorithm> getAlgorithms(List<String> checksumTypes) {
        return checksumTypes.stream().map(ca ->
                ChecksumAlgorithm.valueOf(ca.toUpperCase())).collect(Collectors.toList());
    }

    public static List<Checksum> newChecksums(List<ChecksumAlgorithm> checksumAlgorithms) {
        return checksumAlgorithms.stream().map( a -> new Checksum(a)).collect(Collectors.toList());
    }

}
