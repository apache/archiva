package org.codehaus.redback.rest.api.model;
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

import org.apache.commons.lang.StringUtils;
import org.codehaus.redback.rest.api.services.RedbackServiceException;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Olivier Lamy
 * @since 1.4
 */
@XmlRootElement( name = "redbackRestError" )
public class RedbackRestError
    implements Serializable
{

    private List<ErrorMessage> errorMessages = new ArrayList<ErrorMessage>( 1 );

    public RedbackRestError()
    {
        // no op
    }

    public RedbackRestError( RedbackServiceException e )
    {
        errorMessages.addAll( e.getErrorMessages() );
        if ( e.getErrorMessages().isEmpty() && StringUtils.isNotEmpty( e.getMessage() ) )
        {
            errorMessages.add( new ErrorMessage( e.getMessage(), null ) );
        }
    }


    public List<ErrorMessage> getErrorMessages()
    {
        return errorMessages;
    }

    public void setErrorMessages( List<ErrorMessage> errorMessages )
    {
        this.errorMessages = errorMessages;
    }

    public void addErrorMessage( ErrorMessage errorMessage )
    {
        this.errorMessages.add( errorMessage );
    }
}
