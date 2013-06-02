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
define("archiva/admin/repository/maven2/main",["jquery",'i18n',"archiva/admin/repository/maven2/repository-groups","archiva/admin/repository/maven2/proxy-connectors-rules","archiva/admin/repository/maven2/proxy-connectors"],
        function() {
            showMenu = function(administrationMenuItems) {
                administrationMenuItems.push(
                        {text: $.i18n.prop('menu.repository.groups'),
                    order:500,
                            id: "menu-repository-groups-list-a",
                            href: "#repositorygroup",
                            redback: "{permissions: ['archiva-manage-configuration']}",
                            func: function() {
                                displayRepositoryGroups();
                            }
                        });
                administrationMenuItems.push({text: $.i18n.prop('menu.repositories'),  order:510, id: "menu-repositories-list-a", href: "#repositorylist", redback: "{permissions: ['archiva-manage-configuration']}", func: function() {
                        displayRepositoriesGrid();
                    }});
                administrationMenuItems.push({text: $.i18n.prop('menu.proxy-connectors'),  order:520, id: "menu-proxy-connectors-list-a", href: "#proxyconnectors", redback: "{permissions: ['archiva-manage-configuration']}", func: function() {
                        displayProxyConnectors();
                    }});
                administrationMenuItems.push({text: $.i18n.prop('menu.proxy-connectors-rules'),  order:530, id: "menu.proxy-connectors-rules-list-a", href: "#proxyconnectorsrules", redback: "{permissions: ['archiva-manage-configuration']}", func: function() {
                        displayProxyConnectorsRules();
                    }});

            };
        }

);