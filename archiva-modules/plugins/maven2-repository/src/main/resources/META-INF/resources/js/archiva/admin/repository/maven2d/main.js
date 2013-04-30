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
define("archiva/admin/repository/maven2d/main",["jquery",'i18n'],
        function() {
            showMenu = function(administrationMenuItems) {
                administrationMenuItems.push(
                        {text: "dummymaven",
                            id: "menu-repository-groups-list-aa",
                            href: "#repositorygroupd",
                            redback: "{permissions: ['archiva-manage-configuration']}",
                            func: function() {
                                alert('Ah! We are the ruthless Pilou-Pilou warriors. I know not very archiva but it not final yet');
                            }
                        });

            };
        }

);