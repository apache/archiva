package org.apache.archiva.redback.integration.mail;

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
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.redback.configuration.UserConfiguration;
import org.apache.archiva.redback.keys.AuthenticationKey;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Mail generator component implementation using velocity.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 *
 */
@Service( "mailGenerator#velocity" )
public class VelocityMailGenerator
    implements MailGenerator
{
    private Logger log = LoggerFactory.getLogger( VelocityMailGenerator.class );

    @Inject
    @Named( value = "userConfiguration" )
    private UserConfiguration config;

    // FIXME use the spring directly 
    @Inject
    @Named( value = "velocityEngine#redback" )
    private VelocityEngine velocityEngine;

    public String generateMail( String templateName, AuthenticationKey authkey, String baseUrl )
    {
        VelocityContext context = createVelocityContext( authkey, baseUrl );

        String packageName = getClass().getPackage().getName().replace( '.', '/' );
        String templateFile = packageName + "/template/" + templateName + ".vm";

        StringWriter writer = new StringWriter();

        try
        {
            velocityEngine.mergeTemplate( templateFile, context, writer );
        }
        catch ( ResourceNotFoundException e )
        {
            log.error( "No such template: '{}'.", templateFile );
        }
        catch ( ParseErrorException e )
        {
            log.error( "Unable to generate email for template '" + templateFile + "': " + e.getMessage(), e );
        }
        catch ( MethodInvocationException e )
        {
            log.error( "Unable to generate email for template '" + templateFile + "': " + e.getMessage(), e );
        }
        catch ( Exception e )
        {
            log.error( "Unable to generate email for template '" + templateFile + "': " + e.getMessage(), e );
        }

        return writer.getBuffer().toString();
    }

    private VelocityContext createVelocityContext( AuthenticationKey authkey, String appUrl )
    {
        VelocityContext context = new VelocityContext();

        context.put( "applicationUrl", config.getString( "application.url", appUrl ) );

        String feedback = config.getString( "email.feedback.path" );

        if ( feedback != null )
        {
            if ( feedback.startsWith( "/" ) )
            {
                feedback = appUrl + feedback;
            }

            context.put( "feedback", feedback );
        }

        context.put( "urlPath", config.getString( "email.url.path", "security/login!login.action" ) );

        context.put( "authkey", authkey.getKey() );

        context.put( "accountId", authkey.getForPrincipal() );

        SimpleDateFormat dateformatter = new SimpleDateFormat( config.getString( "application.timestamp" ), Locale.US );

        context.put( "requestedOn", dateformatter.format( authkey.getDateCreated() ) );

        if ( authkey.getDateExpires() != null )
        {
            context.put( "expiresOn", dateformatter.format( authkey.getDateExpires() ) );
        }
        else
        {
            context.put( "expiresOn", "(does not expire)" );
        }
        return context;
    }


    public UserConfiguration getConfig()
    {
        return config;
    }

    public void setConfig( UserConfiguration config )
    {
        this.config = config;
    }

    public VelocityEngine getVelocityEngine()
    {
        return velocityEngine;
    }

    public void setVelocityEngine( VelocityEngine velocityEngine )
    {
        this.velocityEngine = velocityEngine;
    }
}
