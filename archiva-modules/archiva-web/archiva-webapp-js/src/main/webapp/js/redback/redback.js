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

  // define a container object with various datas
  window.redbackModel = {userOperationNames:null,key:null,i18n:$.i18n.map,rolesViewModel:null};

  // unbinding
  $("#user-create-form-cancel-button").on("click", function(){
    $('#user-create').hide();
  });


  $("#user-create").on("submit",function(){
    //nothing
  });

  /**
   * call successFn on success with passing user object coming from cookie
   */
  userLogged=function(successFn) {
    // call restServices/redbackServices/loginService/isLogged to know
    // if a session exists and check the cookie
    $.log("userLogged");
    var userLogged = true;
    $.ajax("restServices/redbackServices/loginService/isLogged", {
      type: "GET",
      success: function(data) {
        userLogged = JSON.parse(data);
        if (successFn){
          successFn(userLogged == false ? null : jQuery.parseJSON($.cookie('redback_login')));
        }
      }
    });
  }

});