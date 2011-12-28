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
$(function() {

  role = function(name,description){
    this.name = name;
    this.description = description;
  }

  displayRolesGrid = function(){
    $("#user-messages").html("");
    $("#main-content").html("");
    $.ajax("restServices/redbackServices/roleManagementService/allRoles",
      {
       type: "GET",
       async: false,
       dataType: 'json',
       success: function(data) {
         var roles = $.map(data.role, function(item) {
             return mapRole(item);
         });

         $("#main-content").html($("#rolesTabs").tmpl());
         $("#main-content #roles-view-tabs-content #roles-view").html($("#rolesGrid").tmpl(data));
         activateRolesGridTab();
       }
      }
    );
  }

  /**
   * @param data Role response from redback rest api
   */
  mapRole=function(data) {
    return new role(data.name, data.description);
  }

  activateRolesGridTab=function(){
    $("#main-content #roles-view-tabs li").removeClass("active");
    $("#main-content #roles-view-tabs-content div").removeClass("active");
    // activate roles grid tab
    $("#main-content #roles-view-tabs-content #roles-view").addClass("active");
    $("#main-content #roles-view-tabs-li-roles-grid").addClass("active");
  }

  activateRoleEditTab=function(){
    $("#main-content #roles-view-tabs li").removeClass("active");
    $("#main-content #roles-view-tabs-content div").removeClass("active");
    // activate role edit tab
    $("#main-content #roles-view-tabs-content #role-edit").addClass("active");
    $("#roles-view-tabs-li-roles-edit").addClass("active");
  }

});