package org.apache.archiva.admin.model.proxyconnector;
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

import org.apache.archiva.admin.model.beans.ProxyConnector;

import java.util.Comparator;

/**
 * @author Olivier Lamy
 * @since 1.4-M1
 */
public class ProxyConnectorOrderComparator
    implements Comparator<ProxyConnector>
{
    private static ProxyConnectorOrderComparator INSTANCE = new ProxyConnectorOrderComparator();

    @Override
    public int compare( ProxyConnector o1, ProxyConnector o2 )
    {
        if ( o1 == null && o2 == null )
        {
            return 0;
        }

        // Ensure null goes to end of list.
        if ( o1 == null && o2 != null )
        {
            return 1;
        }

        if ( o1 != null && o2 == null )
        {
            return -1;
        }

        // Ensure 0 (unordered) goes to end of list.
        if ( o1.getOrder() == 0 && o2.getOrder() != 0 )
        {
            return 1;
        }

        if ( o1.getOrder() != 0 && o2.getOrder() == 0 )
        {
            return -1;
        }

        return o1.getOrder() - o2.getOrder();
    }

    public static ProxyConnectorOrderComparator getInstance()
    {
        return INSTANCE;
    }
}
