package org.codehaus.plexus.redback.management;

/*
 * Copyright 2006 The Apache Software Foundation.
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

import org.codehaus.plexus.redback.keys.KeyManager;
import org.codehaus.plexus.redback.rbac.RBACManager;
import org.codehaus.plexus.redback.rbac.RbacManagerException;
import org.codehaus.plexus.redback.users.UserManager;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;

/**
 * Data management tool API.
 */
public interface DataManagementTool
{
    /**
     * Plexus role.
     */
    String ROLE = DataManagementTool.class.getName();

    void backupRBACDatabase( RBACManager manager, File backupDirectory )
        throws RbacManagerException, IOException, XMLStreamException;

    void backupUserDatabase( UserManager manager, File backupDirectory )
        throws IOException, XMLStreamException;

    void backupKeyDatabase( KeyManager manager, File backupDirectory )
        throws IOException, XMLStreamException;

    void restoreRBACDatabase( RBACManager manager, File backupDirectory )
        throws IOException, XMLStreamException, RbacManagerException;

    void restoreUsersDatabase( UserManager manager, File backupDirectory )
        throws IOException, XMLStreamException;

    void restoreKeysDatabase( KeyManager manager, File backupDirectory )
        throws IOException, XMLStreamException;

    void eraseRBACDatabase( RBACManager manager );

    void eraseUsersDatabase( UserManager manager );

    void eraseKeysDatabase( KeyManager manager );
}
