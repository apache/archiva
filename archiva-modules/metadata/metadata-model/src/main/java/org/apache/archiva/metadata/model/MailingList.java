package org.apache.archiva.metadata.model;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.List;

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

/**
 * Information about the available mailing lists for communicating with the project.
 *
 * TODO considering moving this to a facet - avoid referring to it externally
 */
@XmlRootElement(name = "mailingList")
public class MailingList
    implements Serializable
{
    /**
     * The primary archive URL for this mailing list.
     */
    private String mainArchiveUrl;

    /**
     * A list of other URLs to archives of the mailing list.
     */
    private List<String> otherArchives;

    /**
     * The name of the mailing list, eg. <i>Archiva Developers List</i>.
     */
    private String name;

    /**
     * The email address to post a new message to the mailing list, if applicable.
     */
    private String postAddress;

    /**
     * The email address to send a message to to subscribe to the mailing list, if applicable.
     */
    private String subscribeAddress;

    /**
     * The email address to send a message to to unsubscribe from the mailing list, if applicable.
     */
    private String unsubscribeAddress;

    public void setMainArchiveUrl( String mainArchiveUrl )
    {
        this.mainArchiveUrl = mainArchiveUrl;
    }

    public String getMainArchiveUrl()
    {
        return mainArchiveUrl;
    }

    public void setOtherArchives( List<String> otherArchives )
    {
        this.otherArchives = otherArchives;
    }

    public List<String> getOtherArchives()
    {
        return otherArchives;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public void setPostAddress( String postAddress )
    {
        this.postAddress = postAddress;
    }

    public void setSubscribeAddress( String subscribeAddress )
    {
        this.subscribeAddress = subscribeAddress;
    }

    public void setUnsubscribeAddress( String unsubscribeAddress )
    {
        this.unsubscribeAddress = unsubscribeAddress;
    }

    public String getSubscribeAddress()
    {
        return subscribeAddress;
    }

    public String getUnsubscribeAddress()
    {
        return unsubscribeAddress;
    }

    public String getPostAddress()
    {
        return postAddress;
    }

    public String getName()
    {
        return name;
    }

    @Override
    public String toString()
    {
        return "MailingList{" +
            "mainArchiveUrl='" + mainArchiveUrl + '\'' +
            ", otherArchives=" + otherArchives +
            ", name='" + name + '\'' +
            ", postAddress='" + postAddress + '\'' +
            ", subscribeAddress='" + subscribeAddress + '\'' +
            ", unsubscribeAddress='" + unsubscribeAddress + '\'' +
            '}';
    }
}
