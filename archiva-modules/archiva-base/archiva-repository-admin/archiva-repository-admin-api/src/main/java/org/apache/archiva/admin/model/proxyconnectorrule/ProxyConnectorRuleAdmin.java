package org.apache.archiva.admin.model.proxyconnectorrule;
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

import org.apache.archiva.admin.model.AuditInformation;
import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.beans.ProxyConnectorRule;

import java.util.List;

/**
 * @author Olivier Lamy
 * @since 1.4-M3
 */
public interface ProxyConnectorRuleAdmin
{
    /**
     * @return
     * @throws org.apache.archiva.admin.model.RepositoryAdminException
     *
     * @since 1.4-M3
     */
    List<ProxyConnectorRule> getProxyConnectorRules()
        throws RepositoryAdminException;

    /**
     * @param proxyConnectorRule
     * @throws RepositoryAdminException
     * @since 1.4-M3
     */
    void addProxyConnectorRule( ProxyConnectorRule proxyConnectorRule, AuditInformation auditInformation )
        throws RepositoryAdminException;

    /**
     * @param proxyConnectorRule
     * @throws RepositoryAdminException
     * @since 1.4-M3
     */
    void deleteProxyConnectorRule( ProxyConnectorRule proxyConnectorRule, AuditInformation auditInformation )
        throws RepositoryAdminException;

    /**
     * <b>only to update attached proxy connectors to update pattern delete then add</b>
     * @param proxyConnectorRule
     * @throws RepositoryAdminException
     * @since 1.4-M3
     */
    void updateProxyConnectorRule( ProxyConnectorRule proxyConnectorRule, AuditInformation auditInformation )
        throws RepositoryAdminException;
}
