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
import org.apache.archiva.repository.features.RepositoryFeature;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of a repository with the necessary fields for a bare repository.
 * No features are provided. Capabilities and features must be implemented by concrete classes.
 *
 */
public abstract class AbstractRepository implements EditableRepository
{

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
    String schedulingDefinition = "0 0 02 * *";
    private String layout;
    public static final CronDefinition CRON_DEFINITION = CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ);

    public AbstractRepository(RepositoryType type, String id, String name) {
        this.id = id;
        this.names.put( primaryLocale, name);
        this.type = type;
    }

    public AbstractRepository(Locale primaryLocale, RepositoryType type, String id, String name) {
        setPrimaryLocale( primaryLocale );
        this.id = id;
        this.names.put( primaryLocale, name);
        this.type = type;
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

    public URI getAbsoluteLocation() {
        return baseUri.resolve( location );
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

    @Override
    public <T extends RepositoryFeature<T>> RepositoryFeature<T> getFeature( Class<T> clazz ) throws UnsupportedFeatureException
    {
        throw new UnsupportedFeatureException( "Feature "+clazz+" not supported"  );
    }

    @Override
    public <T extends RepositoryFeature<T>> boolean supportsFeature( Class<T> clazz )
    {
        return false;
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
}
