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

import javax.inject.Inject;
import javax.mail.internet.MimeMessage;
import javax.ws.rs.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * a rest which contains some methods to do some assert
 *
 * @author Olivier Lamy
 */
public class DefaultServicesAssert
    implements ServicesAssert
{

    @Inject
    MockJavaMailSender mockJavaMailSender;

    public List<EmailMessage> getEmailMessageSended()
        throws Exception
    {
        List<EmailMessage> emailMessages = new ArrayList<EmailMessage>();
        for ( MimeMessage mimeMessage : mockJavaMailSender.getSendedEmails() )
        {
            emailMessages.add( new EmailMessage( mimeMessage ) );
        }
        return emailMessages;
    }


}
