package org.apache.archiva.admin.model.beans;
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

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Olivier Lamy
 * @since 1.4-M1
 */
public class AbstractRepository
    implements Serializable
{

    private Locale defaultLocale = Locale.getDefault();

    private String type;

    private String id;

    /*
     * @since 3.0.0 as Map
     */
    private Map<Locale, String> name = new HashMap<>(  );

    /*
     * @since 3.0.0 as Map
     */
    private Map<Locale, String> description = new HashMap<>(  );

    private String layout = "default";

    private String indexDirectory;

    /*
     * @since 3.0.0
     */
    private String packedIndexDirectory;

    private String toStringCache = null;


    public AbstractRepository()
    {
        // no op
    }

    public AbstractRepository(Locale defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    public AbstractRepository( Locale defaultLocale,  String id, String name, String layout )
    {
        this.defaultLocale = defaultLocale;
        setId(id);
        setName(name);
        setLayout(layout);
    }

    public AbstractRepository( String id, String name, String layout )
    {
        setId(id);
        setName(name);
        setLayout(layout);
    }

    public String getId()
    {
        return id;
    }

    public void setId( String id )
    {
        this.toStringCache=null;
        this.id = id;
    }

    public String getName()
    {
        return name.get(defaultLocale);
    }

    public Map<String,String> getNames() {
        if (this.name==null) {
            return Collections.emptyMap();
        }
        return this.name.entrySet().stream().collect( Collectors.toMap( e -> e.getKey().toLanguageTag(), Map.Entry::getValue ) );
    }

    public void setName( String name )
    {
        this.toStringCache=null;
        this.name.put(defaultLocale, name);
    }

    public void setName( String languageTag, String name ) {
        this.toStringCache=null;
        final Locale loc = Locale.forLanguageTag( languageTag );
        this.name.put(loc, name);
    }

    public String getLayout()
    {
        return layout;
    }

    public void setLayout( String layout )
    {
        this.toStringCache=null;
        this.layout = layout;
    }



    public String getIndexDirectory()
    {
        return indexDirectory;
    }

    public void setIndexDirectory( String indexDirectory )
    {
        this.toStringCache=null;
        this.indexDirectory = indexDirectory;
    }

    public String getDescription()
    {
        return this.description.get(defaultLocale);
    }

    public Map<String,String> getDescriptions() {
        if (this.description==null) {
            return Collections.emptyMap();
        }
        return this.description.entrySet().stream().filter( e -> e!=null && e.getKey()!=null )
            .collect( Collectors.toMap( e -> e.getKey().toLanguageTag(), e -> e.getValue()==null ? "" : e.getValue() ) );
    }


    public void setDescription( String description )
    {
        this.toStringCache=null;
        this.description.put(defaultLocale, description);
    }

    public void setDescription( String languageTag, String description) {
        this.toStringCache = null;
        final Locale loc = Locale.forLanguageTag( languageTag );
        this.description.put(loc, description);
    }

    @Override
    public int hashCode()
    {
        int result = 17;
        result = 37 * result + ( id != null ? id.hashCode() : 0 );
        return result;
    }

    @Override
    public boolean equals( Object other )
    {
        if ( this == other )
        {
            return true;
        }

        if ( !( other instanceof AbstractRepository ) )
        {
            return false;
        }

        AbstractRepository that = (AbstractRepository) other;
        boolean result = true;
        result = result && ( getId() == null ? that.getId() == null : getId().equals( that.getId() ) );
        return result;
    }

    private String getLocaleString(Map<Locale, String>  map) {
        return map.entrySet().stream().map(entry -> entry.getKey().toLanguageTag()+'='+entry.getValue()).collect( Collectors.joining( ",") );
    }

    public String getType( )
    {
        return type;
    }

    public void setType(String type) {
        toStringCache=null;
        this.type = type;
    }

    public String getPackedIndexDirectory() {
        return packedIndexDirectory;
    }

    public void setPackedIndexDirectory(String packedIndexDirectory) {
        toStringCache=null;
        this.packedIndexDirectory = packedIndexDirectory;
    }

    @Override
    public String toString()
    {
        if (toStringCache!=null) {
            return toStringCache;
        } else
        {
            final StringBuilder sb = new StringBuilder( );
            sb.append( "AbstractRepository" );
            sb.append( "{ id=\"" ).append( id ).append( '"' );
            sb.append( ", type=\"").append(type).append('"');
            sb.append( ", name=\"" ).append( getLocaleString( name ) ).append( '"' );
            sb.append( ", layout=\"" ).append( layout ).append( '"' );
            sb.append( ", indexDirectory=\"" ).append( indexDirectory ).append( '"' );
            sb.append( ", packedIndexDirectory=\"").append(packedIndexDirectory).append('"');
            sb.append( ", description=\"" ).append( getLocaleString( description ) ).append( '"' );
            sb.append( '}' );
            toStringCache=sb.toString( );
            return toStringCache;
        }
    }


}
