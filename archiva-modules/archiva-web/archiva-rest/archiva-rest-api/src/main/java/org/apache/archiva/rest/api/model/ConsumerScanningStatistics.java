package org.apache.archiva.rest.api.model;
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

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * @author Olivier Lamy
 */
@XmlRootElement( name = "consumerScanningStatistics" )
public class ConsumerScanningStatistics
    implements Serializable
{
    private String consumerKey;

    private long count;

    private long time;

    private String average;

    public ConsumerScanningStatistics()
    {
        // no op
    }

    public String getConsumerKey()
    {
        return consumerKey;
    }

    public void setConsumerKey( String consumerKey )
    {
        this.consumerKey = consumerKey;
    }

    public long getCount()
    {
        return count;
    }

    public void setCount( long count )
    {
        this.count = count;
    }

    public long getTime()
    {
        return time;
    }

    public void setTime( long time )
    {
        this.time = time;
    }

    public String getAverage()
    {
        return average;
    }

    public void setAverage( String average )
    {
        this.average = average;
    }
}
