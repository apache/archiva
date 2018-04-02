package org.apache.archiva.repository;

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
import org.apache.archiva.common.utils.PathUtil;
import org.apache.archiva.indexer.ArchivaIndexingContext;
import org.apache.archiva.repository.features.RepositoryFeature;
import org.apache.archiva.repository.features.StagingRepositoryFeature;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of a repository with the necessary fields for a bare repository.
 * No features are provided. Capabilities and features must be implemented by concrete classes.
 *
 */
public abstract class AbstractRepository implements EditableRepository, RepositoryEventListener
{


    Logger log = LoggerFactory.getLogger(AbstractRepository.class);

    private final RepositoryType type;
    private final String id;
    private Map<Locale, String> names = new HashMap<>(  );
    private Map<Locale, String> descriptions = new HashMap<>(  );

    private Locale primaryLocale = new Locale("en_US");
    private URI location;
    private URI baseUri;
    private Set<URI> failoverLocations = new HashSet<>(  );
    private Set<URI> uFailoverLocations = Collections.unmodifiableSet( failoverLocations );
    private boolean scanned = true;
    String schedulingDefinition = "0 0 02 * * ?";
    private String layout = "default";
    public static final CronDefinition CRON_DEFINITION = CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ);
    private List<RepositoryEventListener> listeners = new ArrayList<>();


    Map<Class<? extends RepositoryFeature<?>>, RepositoryFeature<?>> featureMap = new HashMap<>(  );

    protected Path repositoryBase;
    private ArchivaIndexingContext indexingContext;

    public AbstractRepository(RepositoryType type, String id, String name, Path repositoryBase) {
        this.id = id;
        this.names.put( primaryLocale, name);
        this.type = type;
        this.repositoryBase=repositoryBase;
    }

    public AbstractRepository(Locale primaryLocale, RepositoryType type, String id, String name, Path repositoryBase) {
        setPrimaryLocale( primaryLocale );
        this.id = id;
        this.names.put( primaryLocale, name);
        this.type = type;
        this.repositoryBase=repositoryBase;
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
    public Path getLocalPath() {
        Path localPath;
        if (StringUtils.isEmpty(getLocation().getScheme()) || "file".equals(getLocation().getScheme()) ) {
            localPath = PathUtil.getPathFromUri(getLocation());
            if (localPath.isAbsolute()) {
                return localPath;
            } else {
                return repositoryBase.resolve(localPath);
            }
        } else {
            return repositoryBase.resolve(getId());
        }
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
    public void setLocation( URI location )
    {
        this.location = location;
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
        CronParser parser = new CronParser(CRON_DEFINITION);
        parser.parse(cronExpression).validate();
        this.schedulingDefinition = cronExpression;
    }

    @SuppressWarnings( "unchecked" )
    protected <T extends RepositoryFeature<T>> void addFeature(RepositoryFeature<T> feature) {
       featureMap.put( (Class<? extends RepositoryFeature<?>>) feature.getClass(), feature);
    }

    @Override
    public void setIndexingContext(ArchivaIndexingContext context) {
        this.indexingContext = context;
    }

    @Override
    public ArchivaIndexingContext getIndexingContext() {
        return indexingContext;
    }

    @Override
    public void close() {
        ArchivaIndexingContext ctx = getIndexingContext();
        if (ctx!=null) {
            try {
                ctx.close();
            } catch (IOException e) {
                log.warn("Error during index context close.",e);
            }
        }
        if (supportsFeature(StagingRepositoryFeature.class)) {
            StagingRepositoryFeature sf = getFeature(StagingRepositoryFeature.class).get();
            if (sf.getStagingRepository()!=null) {
                sf.getStagingRepository().close();
            }
        }
        clearListeners();
    }

    @Override
    public <T> void raise(RepositoryEvent<T> event) {
        for(RepositoryEventListener listener : listeners) {
            listener.raise(event);
        }
    }

    public void addListener(RepositoryEventListener listener) {
        if (!this.listeners.contains(listener)) {
            this.listeners.add(listener);
        }
    }

    public void removeListener(RepositoryEventListener listener) {
        this.removeListener(listener);
    }

    public void clearListeners() {
        this.listeners.clear();
    }

}
