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

appendArchivaVersion=function(){
  return "_archivaVersion="+window.archivaRuntimeInfo.version;
};

timestampNoCache=function(){
  if (window.archivaRuntimeInfo.version && window.archivaRuntimeInfo.version.match("SNAPSHOT$")){
    return "&archivaTimestamp="+window.archivaRuntimeInfo.timestamp+(window.archivaRuntimeInfo.devMode?"&_="+jQuery.now():"");
  }
  return "";
};

appendTemplateUrl=function(){
  return appendArchivaVersion()+timestampNoCache();
};

appendJsNoCacheUrl=function(){
  return appendArchivaVersion()+timestampNoCache();
};

$.ajax({
    url: "restServices/archivaUiServices/runtimeInfoService/archivaRuntimeInfo/en",
    dataType: 'json',
    cache: false,
    success:function(data){

      window.archivaDevMode=data.devMode;
      window.archivaJavascriptLog=data.javascriptLog;
      window.archivaRuntimeInfo=data;

      requirejs.config({
          baseUrl: "js/",
          urlArgs: ""+appendJsNoCacheUrl(),
          shim: {
                'sammy':['jquery','jquery.tmpl'],
                'archiva.main':['jquery','jquery.ui','sammy','jquery.tmpl','utils','i18n'],
                'utils':['jquery','jquery.tmpl','i18n'],
                'archiva.templates': ['jquery','jquery.tmpl','utils'],
                'redback.templates': ['jquery','jquery.tmpl','utils']
                },
          paths: {
              "i18n":"jquery.i18n.properties-1.0.9",
              "jquery": "jquery-1.11.1.min",
              "jquery.tmpl": "jquery.tmpl",
              "utils": "archiva/utils",
              "startup": "archiva/startup",
              "jquery.ui": "jquery-ui-1.10.4.min",
              "jquery.ui.widget": "jquery.ui.widget-1.9.2",
              "jquery.cookie": "jquery.cookie.1.4.1",
              "bootstrap": "bootstrap.2.2.2",
              "jquery.validate": "jquery.validate.1.11.1.min",
              "jquery.json": "jquery.json-2.3.min",
              "knockout": "knockout-2.3.0.debug",
              "knockout.simpleGrid": "knockout.simpleGrid",
              "knockout.select2": "knockout.select2",
              "knockout.sortable": "knockout-sortable.0.7.2",
              "jquery.iframe.transport": "jquery.iframe-transport-1.4",
              "jquery.fileupload": "jquery.fileupload-5.10.0",
              "jquery.fileupload.ip":"jquery.fileupload-ip-1.0.6",
              "jquery.fileupload.ui":"jquery.fileupload-ui-6.6.3",
              "tmpl": "tmpl.min",
              "purl": "purl-2.2.1",
              "prettify": "prettify",
              "sammy": "sammy.0.7.4",
              "select2": "select2.min-3.2",
              "jqueryFileTree": "jqueryFileTree-1.0.1",
              "d3": "d3.v3.min",
              "typeahead": "typeahead.min.0.9.3",
              "hogan": "hogan-2.0.0",
              "redback": "redback/redback",
              "redback.roles": "redback/roles",
              "redback.user": "redback/user",
              "redback.users": "redback/users",
              "redback.templates": "redback/redback-tmpl",
              "archiva.cookie-information":"archiva/cookie-information",
              "archiva.templates": "archiva/main-tmpl",
              "archiva.artifacts-management": "archiva/artifacts-management",
              "archiva.search": "archiva/search",
              "archiva.docs": "archiva/docs",
              "archiva.main": "archiva/main"
          }
      });
      
      
 
  
  
      requirejs(['jquery','jquery.tmpl','jquery.ui','i18n','sammy','startup','utils','domReady!','archiva.main','archiva.cookie-information'], function () {
        loadi18n(function () {
          $.ajax({
            url: "restServices/archivaUiServices/runtimeInfoService/archivaRuntimeInfo/"+usedLang(),
            dataType: 'json',
            success:function(data){
              window.archivaDevMode=data.devMode;
              window.archivaJavascriptLog=data.javascriptLog;
              window.archivaRuntimeInfo=data;

              window.cookieInformation=mapCookieInformation(data.cookieInformation);

              require(['sammy','jquery','jquery.ui','i18n','jquery.tmpl','archiva.main','utils','domReady!'],function () {
                  startArchivaApplication();
                  $("#loadingDiv").hide();
                  drawQuickSearchAutocomplete();
              })
            }
          })
        });
      });
    }
});
