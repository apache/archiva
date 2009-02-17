package org.apache.archiva.repository;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import org.apache.archiva.repository.api.MutableResourceContext;
import org.apache.archiva.repository.api.RepositoryManager;
import org.apache.archiva.repository.api.RepositoryManagerException;
import org.apache.archiva.repository.api.RepositoryManagerWeight;
import org.apache.archiva.repository.api.ResourceContext;
import org.apache.archiva.repository.api.Status;
import org.apache.archiva.repository.api.SystemRepositoryManager;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.RepositoryContentFactory;
import org.apache.maven.archiva.repository.RepositoryException;
import org.apache.maven.archiva.repository.content.ManagedLegacyRepositoryContent;
import org.apache.maven.archiva.repository.content.RepositoryRequest;
import org.apache.maven.archiva.repository.layout.LayoutException;

@RepositoryManagerWeight(8000)
public class LegacyRepositoryManager implements RepositoryManager
{
    private final ArchivaConfiguration archivaConfiguration;
    private final SystemRepositoryManager systemRepositoryManager;
    private final RepositoryContentFactory repositoryFactory;
    private final RepositoryRequest repositoryRequest;

    public LegacyRepositoryManager(ArchivaConfiguration archivaConfiguration,
            SystemRepositoryManager systemRepositoryManager,
            RepositoryContentFactory repositoryFactory,
            RepositoryRequest repositoryRequest)
    {
        this.archivaConfiguration = archivaConfiguration;
        this.systemRepositoryManager = systemRepositoryManager;
        this.repositoryFactory = repositoryFactory;
        this.repositoryRequest = repositoryRequest;
    }

    public ResourceContext handles(ResourceContext context)
    {
        final ManagedRepositoryContent repositoryContent = getManagedRepositoryContent(context);
        if (repositoryContent != null)
        {
            if (isLegacyRepository(repositoryContent) || repositoryRequest.isLegacy(context.getLogicalPath()))
            {
                final MutableResourceContext resourceContext = new MutableResourceContext(context);

                try
                {
                    final String nativePath = repositoryRequest.toNativePath(resourceContext.getLogicalPath(), repositoryContent);
                    resourceContext.setLogicalPath(nativePath);
                    return resourceContext;
                }
                catch (LayoutException e)
                {
                    return null;
                }
            }
        }
        return null;
    }

    public boolean read(ResourceContext context, OutputStream os)
    {
        return systemRepositoryManager.read(context, os);
    }

    public boolean write(ResourceContext context, InputStream is)
    {
        return systemRepositoryManager.write(context, is);
    }

    public List<Status> stat(ResourceContext context)
    {
        if (!(context instanceof MutableResourceContext))
        {
            throw new RepositoryManagerException("handles() should have returned a MutableResourceContext");
        }

        return systemRepositoryManager.stat(context);
    }

    public boolean exists(String repositoryId)
    {
        return systemRepositoryManager.exists(repositoryId);
    }

    private ManagedRepositoryContent getManagedRepositoryContent(ResourceContext context)
    {
        try
        {
            return repositoryFactory.getManagedRepositoryContent(context.getRepositoryId());
        }
        catch (RepositoryException e)
        {
            return null;
        }
    }
    
    private boolean isLegacyRepository(ManagedRepositoryContent repositoryContent)
    {
        if (repositoryContent instanceof ManagedLegacyRepositoryContent)
        {
            return true;
        }
        return false;
    }
}
