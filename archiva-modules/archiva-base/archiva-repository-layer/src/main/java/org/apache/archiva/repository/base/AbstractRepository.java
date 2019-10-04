package org.apache.archiva.repository.base;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import org.apache.archiva.event.Event;
import org.apache.archiva.event.EventHandler;
import org.apache.archiva.event.EventManager;
import org.apache.archiva.event.EventType;
import org.apache.archiva.indexer.ArchivaIndexingContext;
import org.apache.archiva.repository.EditableRepository;
import org.apache.archiva.repository.RepositoryCapabilities;
import org.apache.archiva.repository.RepositoryType;
import org.apache.archiva.repository.UnsupportedFeatureException;
import org.apache.archiva.repository.event.*;
import org.apache.archiva.repository.storage.RepositoryStorage;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.archiva.repository.features.RepositoryFeature;
import org.apache.archiva.repository.features.StagingRepositoryFeature;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.CopyOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Implementation of a repository with the necessary fields for a bare repository.
 * No features are provided. Capabilities and features must be implemented by concrete classes.
 *
 */
public abstract class AbstractRepository implements EditableRepository, EventHandler<RepositoryEvent>
{


    Logger log = LoggerFactory.getLogger(AbstractRepository.class);

    private final AtomicBoolean openStatus = new AtomicBoolean(false);


    private final RepositoryType type;
    private final String id;
    private Map<Locale, String> names = new HashMap<>(  );
    private Map<Locale, String> descriptions = new HashMap<>(  );

    private Locale primaryLocale = new Locale("en_US");
    protected URI location;
    private URI baseUri;
    private Set<URI> failoverLocations = new HashSet<>(  );
    private Set<URI> uFailoverLocations = Collections.unmodifiableSet( failoverLocations );
    private boolean scanned = true;
    String schedulingDefinition = "0 0 02 * * ?";
    private String layout = "default";
    public static final CronDefinition CRON_DEFINITION = CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ);

    private final EventManager eventManager;

    Map<Class<? extends RepositoryFeature<?>>, RepositoryFeature<?>> featureMap = new HashMap<>(  );

    private ArchivaIndexingContext indexingContext;
    private RepositoryStorage storage;

    public AbstractRepository(RepositoryType type, String id, String name, RepositoryStorage repositoryStorage) {
        this.id = id;
        this.names.put( primaryLocale, name);
        this.type = type;
        this.storage = repositoryStorage;
        this.location = repositoryStorage.getLocation();
        this.openStatus.compareAndSet(false, true);
        this.eventManager = new EventManager(this);
    }

    public AbstractRepository(Locale primaryLocale, RepositoryType type, String id, String name, RepositoryStorage repositoryStorage) {
        setPrimaryLocale( primaryLocale );
        this.id = id;
        this.names.put( primaryLocale, name);
        this.type = type;
        this.storage = repositoryStorage;
        this.location = repositoryStorage.getLocation();
        this.openStatus.compareAndSet(false, true);
        this.eventManager = new EventManager(this);
    }

    protected void setPrimaryLocale(Locale locale) {
        this.primaryLocale = locale;
    }

    @Override
    public String getId( )
    {
        return id;
    }

    @Override
    public String getName( )
    {
        return getName( primaryLocale );
    }

    @Override
    public String getName( Locale locale )
    {
        return names.get(locale);
    }

    @Override
    public String getDescription( )
    {
        return getDescription( primaryLocale );
    }

    @Override
    public String getDescription( Locale locale )
    {
        return descriptions.get(primaryLocale);
    }

    @Override
    public RepositoryType getType( )
    {
        return type;
    }

    @Override
    public URI getLocation( )
    {
        return location;
    }

    @Override
    public StorageAsset getLocalPath() {
        return storage.getAsset("");
    }

    @Override
    public Set<URI> getFailoverLocations( )
    {
        return uFailoverLocations;
    }

    @Override
    public boolean isScanned( )
    {
        return scanned;
    }

    @Override
    public String getSchedulingDefinition( )
    {
        return schedulingDefinition;
    }

    @Override
    public abstract boolean hasIndex( );

    @Override
    public String getLayout( )
    {
        return layout;
    }

    @Override
    public abstract RepositoryCapabilities getCapabilities( );

    @SuppressWarnings( "unchecked" )
    @Override
    public <T extends RepositoryFeature<T>> RepositoryFeature<T> getFeature( Class<T> clazz ) throws UnsupportedFeatureException
    {
        if (featureMap.containsKey( clazz )) {
            return (RepositoryFeature<T>) featureMap.get(clazz);
        } else
        {
            throw new UnsupportedFeatureException( "Feature " + clazz + " not supported" );
        }
    }

    @Override
    public <T extends RepositoryFeature<T>> boolean supportsFeature( Class<T> clazz )
    {
        return featureMap.containsKey( clazz );
    }

    @Override
    public Locale getPrimaryLocale( )
    {
        return primaryLocale;
    }

    @Override
    public void setName( Locale locale, String name )
    {
        names.put(locale, name);
    }

    @Override
    public void setDescription( Locale locale, String description )
    {
        descriptions.put(locale, description);
    }

    @Override
    public void setLocation( final URI location )
    {
        if (location!=null && ( this.location == null || !this.location.equals(location))) {
            try {
                updateLocation(location);
            } catch (IOException e) {
                log.error("Could not update location of repository {} to {}", getId(), location, e);
            }
        }
    }

    @Override
    public void updateLocation(URI newLocation) throws IOException {
        storage.updateLocation(newLocation);
        this.location = newLocation;
    }

    @Override
    public void addFailoverLocation( URI location )
    {
        this.failoverLocations.add(location);
    }

    @Override
    public void removeFailoverLocation( URI location )
    {
        this.failoverLocations.remove( location );
    }

    @Override
    public void clearFailoverLocations( )
    {
        this.failoverLocations.clear();
    }

    @Override
    public void setScanned( boolean scanned )
    {
        this.scanned = scanned;
    }

    @Override
    public void setLayout( String layout )
    {
        this.layout = layout;
    }

    @Override
    public void setBaseUri(URI baseUri) {
        this.baseUri = baseUri;
    }

    @Override
    public void setSchedulingDefinition(String cronExpression) {
        if (StringUtils.isNotEmpty( cronExpression ))
        {
            CronParser parser = new CronParser( CRON_DEFINITION );
            parser.parse( cronExpression ).validate( );
        }
        this.schedulingDefinition = cronExpression;
    }

    @SuppressWarnings( "unchecked" )
    protected <T extends RepositoryFeature<T>> void addFeature(RepositoryFeature<T> feature) {
       featureMap.put( (Class<? extends RepositoryFeature<?>>) feature.getClass(), feature);
    }

    @Override
    public void setIndexingContext(ArchivaIndexingContext context) {
        if (this.indexingContext!=null) {

        }
        this.indexingContext = context;
    }

    @Override
    public ArchivaIndexingContext getIndexingContext() {
        return indexingContext;
    }

    @Override
    public void close() {
        if (this.openStatus.compareAndSet(true, false)) {
            ArchivaIndexingContext ctx = getIndexingContext();
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (IOException e) {
                    log.warn("Error during index context close.", e);
                }
                this.indexingContext = null;

            }
            if (supportsFeature(StagingRepositoryFeature.class)) {
                StagingRepositoryFeature sf = getFeature(StagingRepositoryFeature.class).get();
                if (sf.getStagingRepository() != null) {
                    sf.getStagingRepository().close();
                }
            }
        }

    }

    @Override
    public boolean isOpen() {
        return openStatus.get();
    }

    @Override
    public void handle(RepositoryEvent event) {
        // We just rethrow the events
        eventManager.fireEvent(event);
    }

    @Override
    public <T extends Event> void registerEventHandler( EventType<T> eventType, EventHandler<? super T> eventHandler) {
        if (!EventType.isInstanceOf(eventType, RepositoryEvent.ANY)) {
            throw new IllegalArgumentException("Can only register RepositoryEvent Handlers");
        }
        eventManager.registerEventHandler(eventType, eventHandler);
    }

    @Override
    public <T extends Event> void unregisterEventHandler(EventType<T> type, EventHandler<? super T> eventHandler) {
        eventManager.unregisterEventHandler(type, eventHandler);
    }

    @Override
    public StorageAsset getAsset(String path )
    {
        return storage.getAsset(path);
    }

    @Override
    public StorageAsset addAsset( String path, boolean container )
    {
        return storage.addAsset(path, container);
    }

    @Override
    public void removeAsset( StorageAsset asset ) throws IOException
    {
        storage.removeAsset(asset);
    }

    @Override
    public StorageAsset moveAsset( StorageAsset origin, String destination, CopyOption... copyOptions ) throws IOException
    {
        return storage.moveAsset(origin, destination);
    }

    @Override
    public void moveAsset( StorageAsset origin, StorageAsset destination, CopyOption... copyOptions ) throws IOException
    {
        storage.moveAsset( origin, destination, copyOptions );
    }

    @Override
    public StorageAsset copyAsset( StorageAsset origin, String destination, CopyOption... copyOptions ) throws IOException
    {
        return storage.copyAsset(origin, destination);
    }

    @Override
    public void copyAsset( StorageAsset origin, StorageAsset destination, CopyOption... copyOptions ) throws IOException
    {
        storage.copyAsset( origin, destination, copyOptions);
    }

    @Override
    public void consumeData(StorageAsset asset, Consumer<InputStream> consumerFunction, boolean readLock ) throws IOException
    {
        storage.consumeData(asset, consumerFunction, readLock);
    }

    @Override
    public void consumeDataFromChannel( StorageAsset asset, Consumer<ReadableByteChannel> consumerFunction, boolean readLock ) throws IOException
    {
        storage.consumeDataFromChannel( asset, consumerFunction, readLock );
    }

    @Override
    public void writeData( StorageAsset asset, Consumer<OutputStream> consumerFunction, boolean writeLock ) throws IOException
    {
        storage.writeData( asset, consumerFunction, writeLock );
    }

    @Override
    public void writeDataToChannel( StorageAsset asset, Consumer<WritableByteChannel> consumerFunction, boolean writeLock ) throws IOException
    {
        storage.writeDataToChannel( asset, consumerFunction, writeLock );
    }

    protected void setStorage( RepositoryStorage storage) {
        this.storage = storage;
    }

    protected RepositoryStorage getStorage() {
        return storage;
    }
}
