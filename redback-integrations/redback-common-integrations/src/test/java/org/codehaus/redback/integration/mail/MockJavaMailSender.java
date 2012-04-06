package org.codehaus.redback.integration.mail;

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

import java.util.ArrayList;
import java.util.List;

import javax.mail.internet.MimeMessage;

import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * @author <a href="mailto:olamy@apache.org">olamy</a>
 * @since 26 sept. 2008
 * @version $Id$
 */
public class MockJavaMailSender
    extends JavaMailSenderImpl
    implements JavaMailSender
{

    List<MimeMessage> receivedEmails = new ArrayList<MimeMessage>();
    
    /**
     * 
     */
    public MockJavaMailSender()
    {
       
    }

    @Override
    public void send( MimeMessage mimeMessage )
        throws MailException
    {
        receivedEmails.add( mimeMessage );
    }
    
    public List<MimeMessage> getReceivedEmails()
    {
        return receivedEmails;
    }

}
