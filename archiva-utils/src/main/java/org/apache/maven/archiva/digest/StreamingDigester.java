package org.apache.maven.archiva.digest;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.InputStream;

/**
 * Gradually create a digest for a stream.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public interface StreamingDigester
{
    String ROLE = StreamingDigester.class.getName();

    /**
     * Get the algorithm used for the checksum.
     *
     * @return the algorithm
     */
    String getAlgorithm();

    /**
     * Reset the hashcode calculation algorithm.
     * Only useful when performing incremental hashcodes based on repeated use of {@link #update(InputStream)}
     *
     * @throws DigesterException if there was a problem with the internal message digest
     */
    void reset()
        throws DigesterException;

    /**
     * Calculate the current checksum.
     *
     * @return the current checksum.
     * @throws DigesterException if there was a problem computing the hashcode.
     */
    String calc()
        throws DigesterException;

    /**
     * Update the checksum with the content of the input stream.
     *
     * @param is the input stream
     * @throws DigesterException if there was a problem computing the hashcode.
     */
    void update( InputStream is )
        throws DigesterException;

}
