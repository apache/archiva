package org.codehaus.redback.rest.services.mock;
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

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.internet.MimeMessage;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Olivier Lamy
 */
@XmlRootElement( name = "emailMessage" )
public class EmailMessage
    implements Serializable
{
    private List<String> tos = new ArrayList<String>();

    private String from;

    private String subject;

    private String text;

    public EmailMessage()
    {
        // no op
    }

    public EmailMessage( MimeMessage mimeMessage )
        throws Exception
    {
        this.from = mimeMessage.getFrom()[0].toString();
        for ( Address address : mimeMessage.getRecipients( Message.RecipientType.TO ) )
        {
            tos.add( address.toString() );
        }
        this.setSubject( mimeMessage.getSubject() );
        this.text = (String) mimeMessage.getContent();
    }

    public List<String> getTos()
    {
        return tos;
    }

    public void setTos( List<String> tos )
    {
        this.tos = tos;
    }

    public String getFrom()
    {
        return from;
    }

    public void setFrom( String from )
    {
        this.from = from;
    }

    public String getSubject()
    {
        return subject;
    }

    public void setSubject( String subject )
    {
        this.subject = subject;
    }

    public String getText()
    {
        return text;
    }

    public void setText( String text )
    {
        this.text = text;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "EmailMessage" );
        sb.append( "{tos=" ).append( tos );
        sb.append( ", from='" ).append( from ).append( '\'' );
        sb.append( ", subject='" ).append( subject ).append( '\'' );
        sb.append( ", text='" ).append( text ).append( '\'' );
        sb.append( '}' );
        return sb.toString();
    }
}
