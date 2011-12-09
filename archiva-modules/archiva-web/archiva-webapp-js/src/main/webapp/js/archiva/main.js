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



  $.log("main karma");
  customShowError=function(validator, errorMap, errorList) {
    $( "div.clearfix" ).removeClass( "error" );
    $( "span.help-inline" ).remove();
    for ( var i = 0; errorList[i]; i++ ) {
      var error = errorList[i];
      var field = $("#"+error.element.id);
      field.parents( "div.clearfix" ).addClass( "error" );
      field.parent().append( "<span class=\"help-inline\">" + error.message + "</span>" )
    }
  }

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
            return item.name;
          });

          $("[redback-permissions]").each(function(element){
            var bindingValue = $(this).attr("redback-permissions");
            $(this).hide();
            var neededKarmas = $(eval(bindingValue)).toArray();
            var karmaOk = false;
            $(neededKarmas).each(function(value){
              //alert(neededKarmas[value]);
              if ($.inArray(neededKarmas[value],window.redbackModel.operatioNames)>=0) {
                karmaOk = true;
              }
            });
            if (karmaOk == false) {
              $(this).hide();
            } else {
              $(this).show();
            }
          });
        }
      });
    }

  hideElementWithKarma=function(){
    $("[redback-permissions]").each(function(element){
      $(this).hide();
    });
  }




  checkCreateAdminLink=function(){
    $.ajax("restServices/redbackServices/userService/isAdminUserExists", {
      type: "GET",
      dataType: 'json',
      success: function(data) {
        var adminExists = JSON.parse(data);
        if (adminExists == false) {
          $("#create-admin-link").show();
        } else {
          $("#create-admin-link").hide();
        }
      }
    });
  }

  checkSecurityLinks=function(){
    var user = userLogged();
    $.log("checkSecurityLinks, user:"+user);

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



  // handle url with registration link
  $(document).ready(function() {
    var validateMeId = $.urlParam('validateMe');
    if (validateMeId) {
      validateKey(validateMeId);
    }
  });

  $.log("main.js dom ready");
  $('#topbar-menu-container').html($("#topbar-menu"));
  $('#sidebar-content').html($("#main-menu"));
  checkCreateAdminLink();
  hideElementWithKarma();
  checkSecurityLinks();

})
});

