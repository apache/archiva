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
require( ["order!jquery" ,"order!redback/redback"],
function($) {

$(function() {

  // define a container object with various datas
  window.archivaModel = {};

  $.log("devMode:"+window.archivaDevMode);

  // no cache for ajax queries as we get datas from servers so preventing caching !!
  jQuery.ajaxSetup( {
    cache: false//!window.archivaDevMode
  } );



  /**
   * reccord a cookie for session with the logged user
   * @param user see user.js
   */
  reccordLoginCookie=function(user) {
    $.cookie('redback_login', ko.toJSON(user));
  }

  getUserFromLoginCookie=function(){
    return $.parseJSON($.cookie('redback_login'));
  }

  deleteLoginCookie=function(){
    $.cookie('redback_login', null);
  }

  logout=function(){
    deleteLoginCookie();
    $("#login-link").show();
    $("#register-link").show();
    $("#logout-link").hide();
    $("#change-password-link").hide();
    hideElementWithKarma();
    screenChange();
    $("#main-content").html("");
    $.ajax({
      url: 'restServices/redbackServices/loginService/logout'
    });
  }

  decorateMenuWithKarma=function(user) {
    var username = user.username;
    // we can receive an observable user so take if it's a function or not
    if ($.isFunction(username)){
      username = user.username();
    }
    var url = 'restServices/redbackServices/userService/getCurrentUserOperations';
    $.ajax({
      url: url,
      success: function(data){
        var mappedOperations = $.map(data.operation, function(item) {
            return mapOperation(item);
        });
        window.redbackModel.operatioNames = $.map(mappedOperations, function(item){
          return item.name();
        });

        $("#topbar-menu-container [redback-permissions]").each(function(element){
          checkElementKarma(this);
        });
        $("#sidebar-content [redback-permissions]").each(function(element){
          checkElementKarma(this);
        });
      }
    });
  }

  checkElementKarma=function(element){
    var bindingValue = $(element).attr("redback-permissions");
    $(element).hide();
    var neededKarmas = $(eval(bindingValue)).toArray();
    var karmaOk = false;
    $(neededKarmas).each(function(value){
      if ($.inArray(neededKarmas[value],window.redbackModel.operatioNames)>=0) {
        karmaOk = true;
      }
    });
    if (karmaOk == false) {
      $(element).hide();
    } else {
      $(element).show();
    }
  }

  hideElementWithKarma=function(){
    $("#topbar-menu-container [redback-permissions]").each(function(element){
      $(this).hide();
    });

    $("#sidebar-content [redback-permissions]").each(function(element){
      $(this).hide();
    });
    $.log("hideElementWithKarma");
  }

  userLoggedCallbackFn=function(user){
    $.log("userLoggedCallbackFn:"+ (user?user.username:null));

    if (!user) {
      $("#login-link").show();
      $("#register-link").show();
      $("#change-password-link").hide();
    } else {
      $("#change-password-link").show();
      $("#logout-link").show();
      $("#register-link").hide();
      $("#login-link").hide();
      decorateMenuWithKarma(user);
    }
  }

  checkSecurityLinks=function(){
    userLogged(userLoggedCallbackFn);
  }

  checkCreateAdminLink=function(){
    $.ajax("restServices/redbackServices/userService/isAdminUserExists", {
      type: "GET",
      dataType: 'json',
      success: function(data) {
        var adminExists = JSON.parse(data);
        if (adminExists == false) {
          $("#create-admin-link").show();
          $("#login-link").hide();
          $("#register-link").hide();
        } else {
          $("#create-admin-link").hide();
        }
        $.log("adminExists:"+adminExists);
      }
    });
  }

  // handle url with registration link
  $(document).ready(function() {
    var validateMeId = $.urlParam('validateMe');
    if (validateMeId) {
      validateKey(validateMeId);
    }
  });

  startArchivaApplication = function(){
    $.log("startArchivaApplication");
    $('#topbar-menu-container').html($("#topbar-menu"));
    $('#sidebar-content').html($("#main-menu"));

    hideElementWithKarma();
    checkSecurityLinks();
    checkCreateAdminLink();
    $('#footer-content').html($('#footer-tmpl').tmpl(window.archivaRuntimeInfo));

    // create handlers on menu entries to add class active on click
    var alinkNodes=$("#sidebar-content #main-menu").find("li a");
    alinkNodes.on("click",function(){
      alinkNodes.parent("li").removeClass("active");
      $(this).parent("li").addClass("active");
    })

  }
  startArchivaApplication();
})
});

