package org.apache.maven.archiva.web.validator;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.opensymphony.xwork.validator.ValidationException;
import com.opensymphony.xwork.validator.ValidatorContext;
import com.opensymphony.xwork.validator.validators.ValidatorSupport;

/**
 *
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 */
public class IntervalValidator
    extends ValidatorSupport
{

    public void validate( Object obj )
        throws ValidationException
    {
        String snapshotsPolicy = ( String ) getFieldValue( "snapshotsPolicy", obj );
        String releasesPolicy = ( String ) getFieldValue( "releasesPolicy", obj );
        Integer snapshotsInterval = ( Integer ) getFieldValue( "snapshotsInterval", obj );
        Integer releasesInterval = ( Integer ) getFieldValue( "releasesInterval", obj );

        ValidatorContext ctxt = getValidatorContext();

        if( !snapshotsPolicy.equals( "interval" ) )
        {
            if( snapshotsInterval.intValue() != 0 )
            {
                ctxt.addActionError( "Snapshots Interval must be set to zero." );
            }
        }
              
        if( !releasesPolicy.equals( "interval" ) )
        {
            if( releasesInterval.intValue() != 0 )
            {
                ctxt.addActionError( "Releases Interval must be set to zero." );
            }
        }

        if( ctxt.hasActionErrors() )
        {
            return;
        }
    }
}
