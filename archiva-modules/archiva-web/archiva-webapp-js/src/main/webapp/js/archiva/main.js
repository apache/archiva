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

  logout=function(screenChange){
    deleteLoginCookie();
    $("#login-link").show();
    $("#register-link").show();
    $("#logout-link").hide();
    $("#change-password-link").hide();
    hideElementWithKarma();
    if (screenChange) screenChange();
    $("#main-content").html("");
    $.ajax({
      url: 'restServices/redbackServices/loginService/logout'
    });
  }

  // handle url with registration link
  var checkUrlParams=function() {
    var validateMeId = $.urlParam('validateMe');
    if (validateMeId) {
      validateKey(validateMeId);
      return;
    }
    var resetPassword= $.urlParam('resetPassword');
    if (resetPassword){
      resetPasswordForm(resetPassword);
      return;
    }

    var browse = $.urlParam('browse');
    if (browse){
      displayBrowseGroupId(browse);
      return;
    }

    var artifact= $.urlParam("artifact");
    // format groupId:artifactId org.apache.maven.plugins:maven-jar-plugin
    //
    if (artifact){
      if ( artifact.indexOf(':')>=0){
        var splitted = artifact.split(':');
        displayBrowseArtifactDetail(splitted[0],splitted[1],null,null);
        return;
      }
    }

    var screen = $.urlParam('screen');

    if(screen){
      if(screen=='network-proxies'&& hasKarma('archiva-manage-configuration')){
        displayNetworkProxies();
        return;
      }
      if(screen=='proxy-connectors'&& hasKarma('archiva-manage-configuration')){
        displayProxyConnectors();
        return;
      }
      if(screen=="legacy-artifact-path-support"&& hasKarma('archiva-manage-configuration')){
        displayLegacyArtifactPathSupport();
        return;
      }
      if (screen=='repository-scanning'&& hasKarma('archiva-manage-configuration')){
        displayRepositoryScanning();
        return;
      }
      if (screen=='network-configuration'&& hasKarma('archiva-manage-configuration')){
        displayNetworkConfiguration();
        return;
      }
      if (screen=='system-status'&& hasKarma('archiva-manage-configuration')){
        displaySystemStatus();
        return;
      }
      if (screen=='repositories-management'&& hasKarma('archiva-manage-configuration')){
        displayRepositoriesGrid();
        return;
      }
    }

    // by default display search screen
    displaySearch();
  }

  hasKarma=function(karmaName){
    return $.inArray(karmaName,window.redbackModel.operatioNames)>=0;
  }

  decorateMenuWithKarma=function(user) {
    var username = user.username;
    $.log("decorateMenuWithKarma");
    // we can receive an observable user so take if it's a function or not
    if ($.isFunction(username)){
      username = user.username();
    }
    var url = 'restServices/redbackServices/userService/getCurrentUserOperations';
    $.ajax({
      url: url,
      success: function(data){
        var mappedOperations = $.map(data, function(item) {
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
        checkUrlParams();
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
      checkUrlParams();
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
        var adminExists = data;
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

    $( "#quick-search-autocomplete" ).autocomplete({
      minLength: 3,
			source: function(request, response){
        $.get("restServices/archivaServices/searchService/quickSearch?queryString="+encodeURIComponent(request.term),
           function(data) {
             var res = mapArtifacts(data);
             var uniqId = [];
             var uniqArtifactIds=[];
             for (var i= 0;i<res.length;i++){
               if ( $.inArray(res[i].artifactId,uniqId)<0){
                 uniqId.push(res[i].artifactId);
                 uniqArtifactIds.push(res[i]);
               }
             }
             response(uniqArtifactIds);
           }
        );
      },
      select: function( event, ui ) {
        $.log("select artifactId:"+ui.item.artifactId);
        // user can be in a non search view so init the search view first
        displaySearch(function(){
          var searchViewModel = new SearchViewModel();
          var searchRequest = new SearchRequest();
          searchRequest.artifactId(ui.item.artifactId);
          searchViewModel.searchRequest(searchRequest);
          searchViewModel.externalAdvancedSearch();
        });
      }
		}).data( "autocomplete" )._renderItem = function( ul, item ) {
							return $( "<li></li>" )
								.data( "item.autocomplete", item )
								.append( "<a>" + item.artifactId + "</a>" )
								.appendTo( ul );
						};;
  }



  startArchivaApplication();



})
});

