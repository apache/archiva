package $package;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.FileTypes;
import org.apache.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.archiva.consumers.ConsumerException;
import org.apache.archiva.consumers.KnownRepositoryContentConsumer;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.registry.Registry;
import org.codehaus.plexus.registry.RegistryListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.apache.archiva.admin.model.beans.ManagedRepository;

import javax.annotation.PostConstruct;

/**
 * <code>SimpleArtifactConsumer</code>
 * 
 */
@Service("knownRepositoryContentConsumer#simple")
@Scope("prototype")
public class SimpleArtifactConsumer
    extends AbstractMonitoredConsumer
    implements KnownRepositoryContentConsumer, RegistryListener, Initializable
{

    private Logger log = LoggerFactory.getLogger( SimpleArtifactConsumer.class );

    /**
     * default-value="simple-artifact-consumer"
     */
    private String id = "simple-artifact-consumer";

    /**
     *
     */
    private String description = "Simple consumer to illustrate how to consume the contents of a repository.";

    /**
     *
     */
    @Inject
    private FileTypes filetypes;

    /**
     *
     */
    @Inject
    private ArchivaConfiguration configuration;

    private List propertyNameTriggers = new ArrayList();

    private List includes = new ArrayList();

    /** current repository being scanned */
    private ManagedRepository repository;

    public void beginScan( ManagedRepository repository )
        throws ConsumerException
    {
        this.repository = repository;
        log.info( "Beginning scan of repository [" + this.repository.getId() + "]" );
    }

    public void beginScan( ManagedRepository repository, Date whenGathered )
        throws ConsumerException
    {
        this.repository = repository;
        log.info( "Beginning scan of repository [" + this.repository.getId() + "]" );
    }

    public void beginScan( ManagedRepository repository, Date whenGathered, boolean executeOnEntireRepo )
        throws ConsumerException
    {
        this.repository = repository;
        log.info( "Beginning scan of repository [" + this.repository.getId() + "]" );
    }

    public void processFile( String path )
        throws ConsumerException
    {
        log.info( "Processing entry [" + path + "] from repository [" + this.repository.getId() + "]" );
    }

    public void processFile( String path, boolean executeOnEntireRepo )
        throws ConsumerException
    {
        log.info( "Processing entry [" + path + "] from repository [" + this.repository.getId() + "]" );
    }

    public void completeScan()
    {
        log.info( "Finished scan of repository [" + this.repository.getId() + "]" );
    }

    public void completeScan( boolean executeOnEntireRepo )
    {
        log.info( "Finished scan of repository [" + this.repository.getId() + "]" );
    }


    /**
     * Used by archiva to determine if the consumer wishes to process all of a repository's entries or just those that
     * have been modified since the last scan.
     * 
     * @return boolean true if the consumer wishes to process all entries on each scan, false for only those modified
     *         since the last scan
     */
    public boolean isProcessUnmodified()
    {
        return super.isProcessUnmodified();
    }

    public void afterConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        if ( propertyNameTriggers.contains( propertyName ) )
        {
            initIncludes();
        }
    }

    public void beforeConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        /* do nothing */
    }

    private void initIncludes()
    {
        includes.clear();
        includes.addAll( filetypes.getFileTypePatterns( FileTypes.INDEXABLE_CONTENT ) );
    }

    @PostConstruct
    public void initialize()
        throws InitializationException
    {
        propertyNameTriggers = new ArrayList();
        propertyNameTriggers.add( "repositoryScanning" );
        propertyNameTriggers.add( "fileTypes" );
        propertyNameTriggers.add( "fileType" );
        propertyNameTriggers.add( "patterns" );
        propertyNameTriggers.add( "pattern" );

        configuration.addChangeListener( this );

        initIncludes();
    }

    public String getId()
    {
        return this.id;
    }

    public String getDescription()
    {
        return this.description;
    }

    public List getExcludes()
    {
        return null;
    }

    public List getIncludes()
    {
        return this.includes;
    }

    public boolean isPermanent()
    {
        return false;
    }
}
