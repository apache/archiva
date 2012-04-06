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

import java.io.File;
import java.util.Date;

/**
 * A record of a backup directory for displaying the backup/restore page.
 */
public class BackupRecord
    implements Comparable<BackupRecord>
{
    private final File directory;

    private final Date date;

    private final boolean userDatabase;

    public BackupRecord( File directory )
    {
        this.directory = directory;

        this.date = new Date( directory.lastModified() );

        this.userDatabase = new File( directory, "users.xml" ).exists();
    }

    public File getDirectory()
    {
        return directory;
    }

    public Date getDate()
    {
        return date;
    }

    public boolean isUserDatabase()
    {
        return userDatabase;
    }

    public boolean isValidBackup()
    {
        return userDatabase;
    }

    public int compareTo( BackupRecord record )
    {
        return record.date.compareTo( this.date );
    }
}
