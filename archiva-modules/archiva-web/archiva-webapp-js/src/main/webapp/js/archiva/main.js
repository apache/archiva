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

    //$LAB.script("main-tmpl.js").wait(function(){
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
       * return a user see user.js if user logged otherwise null
       */
      userLogged=function() {
        // call restServices/redbackServices/loginService/isLogged to know
        // if a session exists and check the cookie
        var userLogged = true;
        $.ajax("restServices/redbackServices/loginService/isLogged", {
          type: "GET",
          async: false,
          success: function(data) {
            userLogged = JSON.parse(data);
          }
        });
        if (userLogged == false)
        {
          return null;
        }
        return jQuery.parseJSON($.cookie('redback_login'));
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


      $('#topbar-menu-container').html($("#topbar-menu"));
      $('#sidebar-content').html($("#main-menu"));

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

      checkCreateAdminLink();
      hideElementWithKarma();

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

      checkSecurityLinks();


      /**
       * display a success message
       * @param text the success text
       * @param idToAppend the id to append the success box
       */
      displaySuccessMessage=function(text,idToAppend){
        var textId = idToAppend ? $("#"+idToAppend) : $("#user-messages");
        $.tmpl($("#alert-message-success").html(), { "message" : text }).appendTo( textId );
        $(textId).focus();
      }

      clearUserMessages=function(idToAppend){
        var textId = idToAppend ? $("#"+idToAppend) : $("#user-messages");
        $(textId).html('');
      }

      /**
       * display an error message
       * @param text the success text
       * @param idToAppend the id to append the success box
       */
      displayErrorMessage=function(text,idToAppend){
        var textId = idToAppend ? $("#"+idToAppend) : $("#user-messages");
        $.tmpl($("#alert-message-error").html(), { "message" : text }).appendTo( textId );
        $(textId).focus();
      }

      /**
       * display a warning message
       * @param text the success text
       * @param idToAppend the id to append the success box
       */
      displayWarningMessage=function(text,idToAppend){
        var textId = idToAppend ? $("#"+idToAppend) : $("#user-messages");
        $.tmpl($("#alert-message-warning").html(), { "message" : text }).appendTo( textId );
        $(textId).focus();
      }

      screenChange=function(){
        $("#main-content").html("");
        clearUserMessages();
      }

      /**
       * clear all input text and password found in the the selector
       * @param selectorStr
       */
      clearForm=function(selectorStr){
        $(selectorStr+" input[type='text']").each(function(ele){
          $(this).val("");
        });
        $(selectorStr+" input[type='password']").each(function(ele){
          $(this).val("");
        });

      }
    });
  //});

});

