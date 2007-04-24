package org.apache.maven.archiva.web.action.admin;

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

import com.opensymphony.xwork.ModelDriven;
import com.opensymphony.xwork.Preparable;
import com.opensymphony.xwork.Validateable;

import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.InvalidConfigurationException;
import org.apache.maven.archiva.indexer.RepositoryIndexException;
import org.apache.maven.archiva.indexer.RepositoryIndexSearchException;
//import org.apache.maven.archiva.repositories.ActiveManagedRepositories;
import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.codehaus.plexus.registry.RegistryException;
import org.codehaus.plexus.scheduler.CronExpressionValidator;
import org.codehaus.plexus.security.rbac.Resource;
import org.codehaus.plexus.security.ui.web.interceptor.SecureAction;
import org.codehaus.plexus.security.ui.web.interceptor.SecureActionBundle;
import org.codehaus.plexus.security.ui.web.interceptor.SecureActionException;
import org.codehaus.plexus.xwork.action.PlexusActionSupport;

import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * Configures the application.
 *
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="configureAction"
 */
public class ConfigureAction
    extends PlexusActionSupport
    implements ModelDriven, Preparable, Validateable, SecureAction
{
    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration archivaConfiguration;

//    /**
//     * @plexus.requirement
//     */
//    private ActiveManagedRepositories activeRepositories;

    /**
     * The configuration.
     */
    private Configuration configuration;
    
    private CronExpressionValidator cronValidator;

    private String second = "0";

    private String minute = "0";

    private String hour = "*";

    private String dayOfMonth = "*";

    private String month = "*";

    private String dayOfWeek = "?";

    private String year;

    private String lastIndexingTime;

    public void validate()
    {
        //validate cron expression
        cronValidator = new CronExpressionValidator();

        if ( !cronValidator.validate( getCronExpression() ) )
        {
            addActionError( "Invalid Cron Expression" );
        }
    }

    public String execute()
        throws IOException, RepositoryIndexException, RepositoryIndexSearchException, InvalidConfigurationException,
        RegistryException
    {
        // TODO: if this didn't come from the form, go to configure.action instead of going through with re-saving what was just loaded
        // TODO: if this is changed, do we move the index or recreate it?
//        configuration.setDataRefreshCronExpression( getCronExpression() );

        // Normalize the path
//        File file = new File( configuration.getIndexPath() );
//        configuration.setIndexPath( file.getCanonicalPath() );
//        if ( !file.exists() )
//        {
//            file.mkdirs();
//            // TODO: error handling when this fails, or is not a directory!
//        }

        // Just double checking that our validation routines line up with what is expected in the configuration
//        assert configuration.isValid();

        archivaConfiguration.save( configuration );

        // TODO: if the repository has changed, we need to check if indexing is needed!

        addActionMessage( "Successfully saved configuration" );

        return SUCCESS;
    }

    public String input()
    {
//        String[] cronEx = configuration.getDataRefreshCronExpression().split( " " );
        String[] cronEx = new String[]{"0","0","*","*","*","*","*"};
        int i = 0;

        while ( i < cronEx.length )
        {
            switch ( i )
            {
                case 0:
                    second = cronEx[i];
                    break;
                case 1:
                    minute = cronEx[i];
                    break;
                case 2:
                    hour = cronEx[i];
                    break;
                case 3:
                    dayOfMonth = cronEx[i];
                    break;
                case 4:
                    month = cronEx[i];
                    break;
                case 5:
                    dayOfWeek = cronEx[i];
                    break;
                case 6:
                    year = cronEx[i];
                    break;
            }
            i++;
        }

//        if ( activeRepositories.getLastDataRefreshTime() != 0 )
//        {
//            lastIndexingTime = new Date( activeRepositories.getLastDataRefreshTime() ).toString();
//        }
//        else
        {
            lastIndexingTime = "Never been run.";
        }

        return INPUT;
    }

    public Object getModel()
    {
        return configuration;
    }

    public void prepare()
    {
        configuration = archivaConfiguration.getConfiguration();
    }

    public String getLastIndexingTime()
    {
        return lastIndexingTime;
    }

    public void setLastIndexingTime( String lastIndexingTime )
    {
        this.lastIndexingTime = lastIndexingTime;
    }

    public String getSecond()
    {
        return second;
    }

    public void setSecond( String second )
    {
        this.second = second;
    }

    public String getMinute()
    {
        return minute;
    }

    public void setMinute( String minute )
    {
        this.minute = minute;
    }

    public String getHour()
    {
        return hour;
    }

    public void setHour( String hour )
    {
        this.hour = hour;
    }

    public String getDayOfMonth()
    {
        return dayOfMonth;
    }

    public void setDayOfMonth( String dayOfMonth )
    {
        this.dayOfMonth = dayOfMonth;
    }

    public String getYear()
    {
        return year;
    }

    public void setYear( String year )
    {
        this.year = year;
    }

    public String getMonth()
    {
        return month;
    }

    public void setMonth( String month )
    {
        this.month = month;
    }

    public String getDayOfWeek()
    {
        return dayOfWeek;
    }

    public void setDayOfWeek( String dayOfWeek )
    {
        this.dayOfWeek = dayOfWeek;
    }

    private String getCronExpression()
    {
        return ( second + " " + minute + " " + hour + " " + dayOfMonth + " " + month + " " + dayOfWeek + " " +
            year ).trim();
    }

    public SecureActionBundle getSecureActionBundle()
        throws SecureActionException
    {
        SecureActionBundle bundle = new SecureActionBundle();

        bundle.setRequiresAuthentication( true );
        bundle.addRequiredAuthorization( ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION, Resource.GLOBAL );

        return bundle;
    }
}
