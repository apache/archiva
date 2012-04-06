package org.codehaus.plexus.redback.users;

/**
 * UserManagerListener 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public interface UserManagerListener
{
    public void userManagerInit( boolean freshDatabase );
    public void userManagerUserAdded( User user );
    public void userManagerUserRemoved( User user );
    public void userManagerUserUpdated( User user );
}
