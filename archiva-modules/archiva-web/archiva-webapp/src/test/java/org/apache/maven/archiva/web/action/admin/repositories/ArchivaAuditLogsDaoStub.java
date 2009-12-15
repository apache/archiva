package org.apache.maven.archiva.web.action.admin.repositories;

import java.util.List;

import org.apache.maven.archiva.database.ArchivaAuditLogsDao;
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.database.Constraint;
import org.apache.maven.archiva.database.ObjectNotFoundException;
import org.apache.maven.archiva.model.ArchivaAuditLogs;

public class ArchivaAuditLogsDaoStub
    implements ArchivaAuditLogsDao
{

    public void deleteAuditLogs( ArchivaAuditLogs logs )
        throws ArchivaDatabaseException
    {
        // TODO Auto-generated method stub
    }

    public List<ArchivaAuditLogs> queryAuditLogs( Constraint constraint )
        throws ObjectNotFoundException, ArchivaDatabaseException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public ArchivaAuditLogs saveAuditLogs( ArchivaAuditLogs logs )
    {
        // TODO Auto-generated method stub
        return null;
    }

}
