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
define("archiva.templates",["text!templates/archiva/menu.html",
          "text!templates/archiva/message.html",
          "text!templates/archiva/modal.html",
          "text!templates/archiva/grids-generics.html",
          "text!templates/archiva/repositories.html",
          "text!templates/archiva/network-proxies.html",
          "text!templates/archiva/proxy-connectors.html",
          "text!templates/archiva/repository-groups.html",
          "text!templates/archiva/search.html",
          "text!templates/archiva/general-admin.html",
          "text!templates/archiva/artifacts-management.html",
          "jquery.tmpl","utils"],
  function(menu,message,modal,grids_generics,repositories,network_proxies,proxies_connectors,
           repository_groups,search,general_admin,artifacts_management) {

    var htmlFragment=$("#html-fragments");
    // template loading
    htmlFragment.append(menu);
    htmlFragment.append(message);
    $.tmpl( modal ).appendTo(htmlFragment);
    htmlFragment.append(grids_generics);
    htmlFragment.append(repositories);
    htmlFragment.append(network_proxies);
    htmlFragment.append(proxies_connectors);
    htmlFragment.append(repository_groups);
    htmlFragment.append(search);
    htmlFragment.append(general_admin);
    htmlFragment.append(artifacts_management);
    $.log("main-tmpl.js loaded");
  }
);