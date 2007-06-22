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

import org.apache.maven.archiva.database.Constraint;

/**
 * Constraint for retrieving artifacts whose sha1 or md5 checksum matches the
 * specified value.
 *
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 */
public class ArtifactsByChecksumConstraint
    extends AbstractDeclarativeConstraint
    implements Constraint
{
    private String whereClause;

    public static final String SHA1_CONDITION = "SHA1";

    public static final String MD5_CONDITION = "MD5";

    public ArtifactsByChecksumConstraint( String desiredChecksum, String condition )
    {
        if ( !condition.equals( SHA1_CONDITION ) && !condition.equals( MD5_CONDITION ) )
        {
            whereClause = "this.checksumSHA1 == desiredChecksum || this.checksumMD5 == desiredChecksum";
        }
        else if ( condition.equals( SHA1_CONDITION ) || condition.equals( MD5_CONDITION ) )
        {
            whereClause = "this.checksum" + condition.trim() + " == desiredChecksum";
        }

        declParams = new String[]{ "String desiredChecksum" };
        params = new Object[]{ desiredChecksum };                
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
