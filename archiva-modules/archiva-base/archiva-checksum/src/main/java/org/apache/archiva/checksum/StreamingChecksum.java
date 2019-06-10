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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * Class that handles checksums with streams.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class StreamingChecksum
{
    static final int BUFFER_SIZE=4096;

    public static void updateChecksums( InputStream input, List<ChecksumAlgorithm> algorithms, List<OutputStream> checksumOutput) {
        List<Checksum> checksums = algorithms.stream().map(a -> new Checksum( a )).collect( Collectors.toList());
        byte[] buffer = new byte[BUFFER_SIZE];
        int read;
        try
        {
            while ( ( read = input.read( buffer ) ) >= 0 )
            {
                for (Checksum cs : checksums ) {
                    cs.update( buffer, 0, read );
                }
            }
            int minIndex = Math.min(algorithms.size(), checksums.size());
            for (int csIndex = 0; csIndex<minIndex; csIndex++) {
                Checksum cs = checksums.get(csIndex);
                cs.finish();
                OutputStream os =checksumOutput.get(csIndex);
                if (os!=null)
                {
                    os.write( cs.getChecksum( ).getBytes( ) );
                }
            }

        } catch ( IOException e ) {

        }
    }
}
