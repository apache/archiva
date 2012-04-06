package org.codehaus.plexus.redback.struts2.action.admin;

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

import com.opensymphony.xwork2.Preparable;
import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.redback.keys.KeyManager;
import org.codehaus.plexus.redback.management.DataManagementTool;
import org.codehaus.plexus.redback.rbac.RBACManager;
import org.codehaus.plexus.redback.rbac.Resource;
import org.codehaus.plexus.redback.struts2.action.AbstractSecurityAction;
import org.codehaus.plexus.redback.users.UserManager;
import org.codehaus.redback.integration.interceptor.SecureAction;
import org.codehaus.redback.integration.interceptor.SecureActionBundle;
import org.codehaus.redback.integration.interceptor.SecureActionException;
import org.codehaus.redback.integration.role.RoleConstants;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;


/**
 * BackupRestoreAction
 */
@Controller( "backup-restore" )
@Scope( "prototype" )
public class BackupRestoreAction
    extends AbstractSecurityAction
    implements SecureAction, Preparable
{
    public final static String CUSTOM_ERROR = "custom_error";

    /**
     *
     */
    private File applicationHome = new File( "data" );

    /**
     * role-hint="jdo"
     */
    @Inject
    private DataManagementTool dataManagementTool;

    /**
     * role-hint="jdo"
     */
    @Inject
    @Named( value = "rBACManager#jdo" )
    private RBACManager rbacManager;

    /**
     * role-hint="jdo"
     */
    @Inject
    @Named( value = "userManager#jdo" )
    private UserManager userManager;

    /**
     * role-hint="jdo"
     */
    @Inject
    @Named( value = "keyManager#jdo" )
    private KeyManager keyManager;

    private File backupDirectory;

    private String restoreDirectory;

    private List<BackupRecord> previousBackups;

    private boolean confirmed;

    public static final String BACKUP_DIRECTORY = "user-backup-directory";

    public String view()
        throws Exception
    {

        retrievePreviousBackups();

        return SUCCESS;
    }

    public String backup()
        throws Exception
    {

        File backupDirectory = getTimestampedBackupDirectory();
        backupDirectory.mkdirs();

        log.info( "Backing up security database to {}", backupDirectory );
        this.backupDatabase( backupDirectory );

        log.info( "Done backing up security database" );

        return SUCCESS;
    }

    public String restore()
        throws Exception
    {
        if ( StringUtils.isEmpty( restoreDirectory ) )
        {
            addActionError( getText( "backupRestore.backup.empty.error" ) );
            return CUSTOM_ERROR;
        }

        File restoreDirectory = new File( this.restoreDirectory );

        boolean fileExists = restoreDirectory.exists() && restoreDirectory.isDirectory();
        boolean isValidBackup = false;

        if ( fileExists )
        {
            BackupRecord record = new BackupRecord( restoreDirectory );
            isValidBackup = record.isValidBackup();
        }

        if ( !fileExists )
        {
            log.warn( "Backup: " + this.restoreDirectory + " not found." );
            addActionError( getText( "backupRestore.backup.error" ) );
            retrievePreviousBackups();
            return CUSTOM_ERROR;
        }
        else if ( !isValidBackup )
        {
            log.warn( "Backup: " + this.restoreDirectory + " is not a valid backup directory." );
            addActionError( getText( "backupRestore.backup.error" ) );
            retrievePreviousBackups();
            return CUSTOM_ERROR;
        }

        log.info( "Restoring security database from {}", this.restoreDirectory );
        this.eraseDatabase();
        this.restoreDatabase( restoreDirectory );
        log.info( "Done restoring security database" );

        return SUCCESS;
    }


    private void backupDatabase( File backupDirectory )
        throws Exception
    {

        dataManagementTool.backupKeyDatabase( keyManager, backupDirectory );
        dataManagementTool.backupRBACDatabase( rbacManager, backupDirectory );
        dataManagementTool.backupUserDatabase( userManager, backupDirectory );
    }

    private void eraseDatabase()
    {
        dataManagementTool.eraseKeysDatabase( keyManager );
        dataManagementTool.eraseRBACDatabase( rbacManager );
        dataManagementTool.eraseUsersDatabase( userManager );
    }

    private void restoreDatabase( File backupDirectory )
        throws Exception
    {

        dataManagementTool.restoreKeysDatabase( keyManager, backupDirectory );
        dataManagementTool.restoreRBACDatabase( rbacManager, backupDirectory );
        dataManagementTool.restoreUsersDatabase( userManager, backupDirectory );
    }

    public String getRestoreDirectory()
    {
        return restoreDirectory;
    }

    public void setRestoreDirectory( String restoreDirectory )
    {
        this.restoreDirectory = restoreDirectory;
    }

    private File getTimestampedBackupDirectory()
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyyMMdd.HHmmss", Locale.US );
        return new File( this.backupDirectory, dateFormat.format( new Date() ) );
    }

    public File getBackupDirectory()
    {
        return backupDirectory;
    }

    public List<BackupRecord> getPreviousBackups()
    {
        return previousBackups;
    }

    public void prepare()
    {
        backupDirectory = this.getFile( BACKUP_DIRECTORY );
        retrievePreviousBackups();
    }

    private void retrievePreviousBackups()
    {
        previousBackups = new ArrayList<BackupRecord>();
        File[] files = backupDirectory.listFiles();
        if ( files != null )
        {
            for ( int i = 0; i < files.length; i++ )
            {
                File f = files[i];

                if ( f.isDirectory() && !f.getName().startsWith( "." ) )
                {
                    BackupRecord record = new BackupRecord( f );

                    if ( record.isValidBackup() )
                    {
                        previousBackups.add( record );
                    }
                }
            }
        }
        Collections.sort( previousBackups );
    }

    public boolean isConfirmed()
    {
        return confirmed;
    }

    public void setConfirmed( boolean confirmed )
    {
        this.confirmed = confirmed;
    }

    @Override
    public SecureActionBundle initSecureActionBundle()
        throws SecureActionException
    {
        SecureActionBundle bundle = new SecureActionBundle();
        bundle.setRequiresAuthentication( true );
        bundle.addRequiredAuthorization( RoleConstants.USER_MANAGEMENT_MANAGE_DATA, Resource.GLOBAL );
        return bundle;
    }

    public File getFile( String filename )
    {
        if ( filename == null )
        {
            return null;
        }

        File f = null;

        if ( filename != null && filename.length() != 0 )
        {
            f = new File( filename );

            if ( !f.isAbsolute() )
            {
                f = new File( applicationHome, filename );
            }
        }

        try
        {
            return f.getCanonicalFile();
        }
        catch ( IOException e )
        {
            return f;
        }
    }
}
