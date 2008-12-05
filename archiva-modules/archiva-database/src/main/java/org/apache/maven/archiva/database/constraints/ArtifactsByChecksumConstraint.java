package org.apache.maven.archiva.database.constraints;

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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.database.Constraint;

/**
 * Constraint for retrieving artifacts whose sha1 or md5 checksum matches the
 * specified value.
 *
 */
public class ArtifactsByChecksumConstraint
    extends AbstractDeclarativeConstraint
    implements Constraint
{
    private String whereClause;

    public static final String SHA1 = "SHA1";

    public static final String MD5 = "MD5";

    /**
     * Create constraint for checksum (without providing type)
     *
     * @param desiredChecksum the checksum (either SHA1 or MD5)
     */
    public ArtifactsByChecksumConstraint( String desiredChecksum )
    {
        this( desiredChecksum, null );
    }

    /**
     * Create constraint for specific checksum.
     *
     * @param desiredChecksum the checksum (either SHA1 or MD5)
     * @param type            the type of checksum (either {@link #SHA1} or {@link #MD5})
     */
    public ArtifactsByChecksumConstraint( String desiredChecksum, String type )
    {
        if ( StringUtils.isEmpty( type ) )
        {
            // default for no specified type.
            whereClause = "this.checksumSHA1 == desiredChecksum || this.checksumMD5 == desiredChecksum";
        }
        else if ( !type.equals( SHA1 ) && !type.equals( MD5 ) )
        {
            // default for type that isn't recognized.
            whereClause = "this.checksumSHA1 == desiredChecksum || this.checksumMD5 == desiredChecksum";
        }
        else if ( type.equals( SHA1 ) || type.equals( MD5 ) )
        {
            // specific type.
            whereClause = "this.checksum" + type.trim() + " == desiredChecksum";
        }

        declParams = new String[]{"String desiredChecksum"};
        params = new Object[]{desiredChecksum.toLowerCase()};
    }

    public String getSortColumn()
    {
        return "groupId";
    }

    public String getWhereCondition()
    {
        return whereClause;
    }
}
