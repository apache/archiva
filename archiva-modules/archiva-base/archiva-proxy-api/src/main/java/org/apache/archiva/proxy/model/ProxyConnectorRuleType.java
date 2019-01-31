package org.apache.archiva.proxy.model;
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
 * @author Olivier Lamy
 * @since 1.4-M3
 */
public enum ProxyConnectorRuleType
{
    WHITE_LIST( "whiteList" ), BLACK_LIST( "blackList" );

    private String ruleType;

    private ProxyConnectorRuleType( String ruleType )
    {
        this.ruleType = ruleType;
    }

    public void setRuleType( String ruleType )
    {
        this.ruleType = ruleType;
    }

    public String getRuleType()
    {
        return ruleType;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "ProxyConnectorRuleType" );
        sb.append( "{ruleType='" ).append( ruleType ).append( '\'' );
        sb.append( '}' );
        return sb.toString();
    }
}
