package org.codehaus.redback.integration.reports;

/*
 * Copyright 2005-2006 The Codehaus.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.codehaus.plexus.redback.system.SecuritySystem;
import org.codehaus.plexus.redback.users.User;
import org.codehaus.plexus.redback.users.UserManager;
import org.codehaus.redback.integration.util.UserComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * CsvUserList
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
@Service( "report#userlist-csv" )
public class CsvUserList
    implements Report
{
    private Logger log = LoggerFactory.getLogger( CsvUserList.class );

    @Inject
    private SecuritySystem securitySystem;

    private Map<String, String> fields;

    public CsvUserList()
    {
        fields = new HashMap<String, String>();
        fields.put( "username", "User Name" );
        fields.put( "fullName", "Full Name" );
        fields.put( "email", "Email Address" );
        fields.put( "permanent", "Permanent User" );
        fields.put( "locked", "Locked User" );
        fields.put( "validated", "Validated User" );
        fields.put( "passwordChangeRequired", "Must Change Password On Next Login" );
        fields.put( "countFailedLoginAttempts", "Failed Login Attempts" );
        fields.put( "lastPasswordChange", "Last Password Change" );
        fields.put( "accountCreationDate", "Date Created" );
        fields.put( "lastLoginDate", "Date Last Logged In" );
    }

    public String getId()
    {
        return "userlist";
    }

    public String getMimeType()
    {
        return "text/csv";
    }

    public String getName()
    {
        return "User List";
    }

    public String getType()
    {
        return "csv";
    }

    public void writeReport( OutputStream os )
        throws ReportException
    {
        UserManager userManager = securitySystem.getUserManager();

        List<User> allUsers = userManager.getUsers();

        Collections.sort( allUsers, new UserComparator( "username", true ) );

        PrintWriter out = new PrintWriter( os );

        writeCsvHeader( out );

        Iterator<User> itUsers = allUsers.iterator();
        while ( itUsers.hasNext() )
        {
            User user = (User) itUsers.next();
            writeCsvRow( out, user );
        }

        out.flush();
    }

    private void writeCsvHeader( PrintWriter out )
    {
        boolean hasPreviousField = false;
        for ( String heading : fields.values() )
        {
            if ( hasPreviousField )
            {
                out.print( "," );
            }
            out.print( escapeCell( heading ) );
            hasPreviousField = true;
        }
        out.println();
    }

    @SuppressWarnings( "unchecked" )
    private void writeCsvRow( PrintWriter out, User user )
        throws ReportException
    {
        try
        {
            boolean hasPreviousField = false;
            Map<String, Object> propMap = PropertyUtils.describe( user );
            for ( String propName : fields.keySet() )
            {
                Object propValue = propMap.get( propName );

                if ( hasPreviousField )
                {
                    out.print( "," );
                }

                if ( propValue != null )
                {
                    out.print( escapeCell( propValue.toString() ) );
                }
                hasPreviousField = true;
            }
            out.println();
        }
        catch ( IllegalAccessException e )
        {
            String emsg = "Unable to produce " + getName() + " report.";
            log.error( emsg, e );
            throw new ReportException( emsg, e );
        }
        catch ( InvocationTargetException e )
        {
            String emsg = "Unable to produce " + getName() + " report.";
            log.error( emsg, e );
            throw new ReportException( emsg, e );
        }
        catch ( NoSuchMethodException e )
        {
            String emsg = "Unable to produce " + getName() + " report.";
            log.error( emsg, e );
            throw new ReportException( emsg, e );
        }
    }

    private String escapeCell( String cell )
    {
        return "\"" + StringEscapeUtils.escapeJava( cell ) + "\"";
    }
}
