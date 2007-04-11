package org.apache.maven.archiva.proxy.policy;

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

import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.io.File;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

/**
 * ArtifactUpdatePolicy - tests the local file to see if the transfer should
 * occur or not.
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role-hint="artifact-update"
 */
public class ArtifactUpdatePolicy
    extends AbstractLogEnabled
    implements PrefetchPolicy
{
    /**
     * The DISABLED policy means that the artifact retrieval isn't even attempted,
     * let alone updated locally.
     */
    public static final String DISABLED = "disabled";

    /**
     * <p>
     * The DAILY policy means that the artifact retrieval occurs only if one of
     * the following conditions are met...
     * </p>
     * <ul>
     *   <li>The local artifact is not present.</li>
     *   <li>The local artifact has a last modified timestamp older than (now - 1 day).</li>
     * </ul>
     */
    public static final String DAILY = "daily";

    /**
     * <p>
     * The HOURLY policy means that the artifact retrieval occurs only if one of
     * the following conditions are met...
     * </p>
     * <ul>
     *   <li>The local artifact is not present.</li>
     *   <li>The local artifact has a last modified timestamp older than (now - 1 hour).</li>
     * </ul>
     */
    public static final String HOURLY = "hourly";

    /**
     * The ONCE policy means that the artifact retrieval occurs only if the
     * local artifact is not present.  This means that the retreival can only
     * occur once.
     */
    public static final String ONCE = "once";

    private Set validPolicyCodes = new HashSet();

    public ArtifactUpdatePolicy()
    {
        validPolicyCodes.add( DISABLED );
        validPolicyCodes.add( DAILY );
        validPolicyCodes.add( HOURLY );
        validPolicyCodes.add( ONCE );
    }

    public boolean applyPolicy( String policyCode, File localFile )
    {
        if ( !validPolicyCodes.contains( policyCode ) )
        {
            // No valid code? false it is then.
            getLogger().error( "Unknown policyCode [" + policyCode + "]" );
            return false;
        }

        if ( DISABLED.equals( policyCode ) )
        {
            // Disabled means no.
            return false;
        }

        if ( !localFile.exists() )
        {
            // No file means it's ok.
            return true;
        }

        if ( ONCE.equals( policyCode ) )
        {
            // File exists, but policy is once.
            return false;
        }

        if ( DAILY.equals( policyCode ) )
        {
            Calendar cal = Calendar.getInstance();
            cal.add( Calendar.DAY_OF_MONTH, -1 );
            Calendar fileCal = Calendar.getInstance();
            fileCal.setTimeInMillis( localFile.lastModified() );

            return cal.after( fileCal );
        }

        if ( HOURLY.equals( policyCode ) )
        {
            Calendar cal = Calendar.getInstance();
            cal.add( Calendar.HOUR, -1 );
            Calendar fileCal = Calendar.getInstance();
            fileCal.setTimeInMillis( localFile.lastModified() );

            return cal.after( fileCal );
        }

        getLogger().error( "Unhandled policyCode [" + policyCode + "]" );
        return false;
    }
}
