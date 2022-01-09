package org.apache.archiva.rest.api.v2.model;/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.RemoteRepository;

import java.io.Serializable;
import java.util.Locale;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 * @since 3.0
 */
@Schema(description = "Repository data")
public class Repository implements Serializable, RestModel
{
    private static final long serialVersionUID = -4741025877287175182L;

    public static final String CHARACTERISTIC_MANAGED = "managed";
    public static final String CHARACTERISTIC_REMOTE = "remote";
    public static final String CHARACTERISTIC_UNKNOWN = "unknown";

    protected String id;
    protected String name;
    protected String description;
    protected String type;
    protected String characteristic;
    protected String location;
    protected boolean scanned;
    protected String schedulingDefinition;
    protected boolean index;
    protected String layout;

    public Repository( )
    {
    }

    public static Repository of( org.apache.archiva.repository.Repository repository ) {
        Repository newRepo = new Repository( );
        newRepo.setId( repository.getId() );
        newRepo.setName( repository.getName( ) );
        newRepo.setDescription( repository.getDescription( ) );
        newRepo.setLocation( repository.getLocation().toASCIIString() );
        newRepo.setIndex( repository.hasIndex() );
        newRepo.setLayout( repository.getLayout() );
        newRepo.setType( repository.getType().name() );
        newRepo.setScanned( repository.isScanned() );
        newRepo.setSchedulingDefinition( repository.getSchedulingDefinition() );
        if (repository instanceof RemoteRepository ) {
            newRepo.setCharacteristic( CHARACTERISTIC_REMOTE );
        } else if (repository instanceof ManagedRepository ) {
            newRepo.setCharacteristic( CHARACTERISTIC_MANAGED );
        } else {
            newRepo.setCharacteristic( CHARACTERISTIC_UNKNOWN );
        }
        return newRepo;
    }

    public static Repository of( org.apache.archiva.repository.Repository repository, Locale locale ) {
        Locale myLocale;
        if (locale==null) {
            myLocale = Locale.getDefault( );
        } else {
            myLocale = locale;
        }
        String repoName = repository.getName( myLocale );
        if (repoName==null) {
            repoName = repository.getName( );
        }
        String description = repository.getDescription( myLocale );
        if (description==null)  {
            description = repository.getDescription( );
        }
        Repository newRepo = new Repository( );
        newRepo.setId( repository.getId() );
        newRepo.setName( repoName );
        newRepo.setDescription( description );
        newRepo.setLocation( repository.getLocation().toASCIIString() );
        newRepo.setIndex( repository.hasIndex() );
        newRepo.setLayout( repository.getLayout() );
        newRepo.setType( repository.getType().name() );
        newRepo.setScanned( repository.isScanned() );
        newRepo.setSchedulingDefinition( repository.getSchedulingDefinition() );
        if (repository instanceof RemoteRepository ) {
            newRepo.setCharacteristic( CHARACTERISTIC_REMOTE );
        } else if (repository instanceof ManagedRepository ) {
            newRepo.setCharacteristic( CHARACTERISTIC_MANAGED );
        } else {
            newRepo.setCharacteristic( CHARACTERISTIC_UNKNOWN );
        }
        return newRepo;
    }

    @Schema(description = "Category of the repository. Either 'managed' or 'remote'.")
    public String getCharacteristic( )
    {
        return characteristic;
    }

    public void setCharacteristic( String characteristic )
    {
        this.characteristic = characteristic;
    }

    @Schema(description = "Unique identifier of the repository")
    public String getId( )
    {
        return id;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    @Schema(description = "Display name of the repository")
    public String getName( )
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    @Schema(description = "Description of the repository")
    public String getDescription( )
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    @Schema(description = "Repository type")
    public String getType( )
    {
        return type;
    }

    public void setType( String type )
    {
        this.type = type;
    }

    @Schema(description = "The location, where the repository data can be found")
    public String getLocation( )
    {
        return location;
    }

    public void setLocation( String location )
    {
        this.location = location;
    }

    @Schema(description = "True, if this repository is scanned regularly")
    public boolean isScanned( )
    {
        return scanned;
    }

    public void setScanned( boolean scanned )
    {
        this.scanned = scanned;
    }

    @Schema(name="scheduling_definition",description = "Definition of regular scheduled scan")
    public String getSchedulingDefinition( )
    {
        return schedulingDefinition;
    }

    public void setSchedulingDefinition( String schedulingDefinition )
    {
        this.schedulingDefinition = schedulingDefinition;
    }

    @Schema(description = "True, if this is a indexed repository")
    public boolean isIndex( )
    {
        return index;
    }

    public void setIndex( boolean index )
    {
        this.index = index;
    }

    @Schema(description = "Layout type is implementation specific")
    public String getLayout( )
    {
        return layout;
    }

    public void setLayout( String layout )
    {
        this.layout = layout;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( o == null || getClass( ) != o.getClass( ) ) return false;

        Repository that = (Repository) o;

        if ( scanned != that.scanned ) return false;
        if ( index != that.index ) return false;
        if ( id != null ? !id.equals( that.id ) : that.id != null ) return false;
        if ( name != null ? !name.equals( that.name ) : that.name != null ) return false;
        if ( description != null ? !description.equals( that.description ) : that.description != null ) return false;
        if ( type != null ? !type.equals( that.type ) : that.type != null ) return false;
        if ( location != null ? !location.equals( that.location ) : that.location != null ) return false;
        if ( schedulingDefinition != null ? !schedulingDefinition.equals( that.schedulingDefinition ) : that.schedulingDefinition != null )
            return false;
        return layout != null ? layout.equals( that.layout ) : that.layout == null;
    }

    @Override
    public int hashCode( )
    {
        int result = id != null ? id.hashCode( ) : 0;
        result = 31 * result + ( name != null ? name.hashCode( ) : 0 );
        result = 31 * result + ( description != null ? description.hashCode( ) : 0 );
        result = 31 * result + ( type != null ? type.hashCode( ) : 0 );
        result = 31 * result + ( location != null ? location.hashCode( ) : 0 );
        result = 31 * result + ( scanned ? 1 : 0 );
        result = 31 * result + ( schedulingDefinition != null ? schedulingDefinition.hashCode( ) : 0 );
        result = 31 * result + ( index ? 1 : 0 );
        result = 31 * result + ( layout != null ? layout.hashCode( ) : 0 );
        return result;
    }

    @Override
    public String toString( )
    {
        final StringBuilder sb = new StringBuilder( "Repository{" );
        sb.append( "id='" ).append( id ).append( '\'' );
        sb.append( ", name='" ).append( name ).append( '\'' );
        sb.append( ", description='" ).append( description ).append( '\'' );
        sb.append( ", type='" ).append( type ).append( '\'' );
        sb.append( ", location='" ).append( location ).append( '\'' );
        sb.append( ", scanned=" ).append( scanned );
        sb.append( ", schedulingDefinition='" ).append( schedulingDefinition ).append( '\'' );
        sb.append( ", index=" ).append( index );
        sb.append( ", layout='" ).append( layout ).append( '\'' );
        sb.append( '}' );
        return sb.toString( );
    }
}
