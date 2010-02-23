package org.apache.archiva.metadata.model;

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

public class MailingList
{
    private String mainArchiveUrl;

    private List<String> otherArchives;

    private String name;

    private String postAddress;

    private String subscribeAddress;

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
}
