package org.apache.archiva.redback.management;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.redback.users.UserManager;
import org.apache.archiva.redback.keys.KeyManager;
import org.apache.archiva.redback.rbac.RBACManager;
import org.apache.archiva.redback.rbac.RbacManagerException;
import org.apache.archiva.redback.users.UserManagerException;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;

/**
 * Data management tool API.
 */
public interface DataManagementTool
{

    void backupRBACDatabase( RBACManager manager, File backupDirectory )
        throws RbacManagerException, IOException, XMLStreamException;

    void backupUserDatabase( UserManager manager, File backupDirectory )
        throws IOException, XMLStreamException, UserManagerException;

    void backupKeyDatabase( KeyManager manager, File backupDirectory )
        throws IOException, XMLStreamException;

    void restoreRBACDatabase( RBACManager manager, File backupDirectory )
        throws IOException, XMLStreamException, RbacManagerException;

    void restoreUsersDatabase( UserManager manager, File backupDirectory )
        throws IOException, XMLStreamException, UserManagerException;

    void restoreKeysDatabase( KeyManager manager, File backupDirectory )
        throws IOException, XMLStreamException;

    void eraseRBACDatabase( RBACManager manager );

    void eraseUsersDatabase( UserManager manager );

    void eraseKeysDatabase( KeyManager manager );
}
