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
require(["text!archiva/templates/menu.html","text!archiva/templates/topbar.html","text!archiva/templates/message.html",
          "text!archiva/templates/modal.html","text!archiva/templates/grids-generics.html","text!archiva/templates/repositories.html"],
  function(menu,topbar,message,modal,grids_generics,repositories) {

    // template loading
    $.tmpl( menu ).appendTo("#html-fragments");
    $.tmpl( topbar ).appendTo("#html-fragments");
    $("#html-fragments").append(message);
    $.tmpl( modal ).appendTo("#html-fragments");
    $("#html-fragments").append(grids_generics);
    $("#html-fragments").append(repositories);
    $.log("main-tmpl.js menu loaded");
  }
);