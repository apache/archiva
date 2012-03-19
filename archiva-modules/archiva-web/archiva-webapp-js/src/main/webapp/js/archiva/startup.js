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

  /**
   * return value of a param in the url
   * @param name
   */
  $.urlParam = function(name){
      var results = new RegExp('[\\?&]' + name + '=([^&#]*)').exec(window.location.href);
      if (results) {
        return results[1] || 0;
      }
      return null;
  }

  usedLang=function(){
    var browserLang = $.i18n.browserLang();
    var requestLang = $.urlParam('request_lang');
    if (requestLang) {
      browserLang=requestLang;
    }
    return browserLang;
  }

  appendArchivaVersion=function(){
    return "_archivaVersion="+window.archivaRuntimeInfo.version;
  }

  buildLoadJsUrl=function(srcScript){
    return srcScript+"?"+appendArchivaVersion();
  }

  $.ajaxSetup({
    dataType: 'json'
  });

  loadJs=function(){

    $.ajax(
      {
        url: "restServices/archivaUiServices/runtimeInfoService/archivaRuntimeInfo/"+usedLang(),
        dataType: 'json',
        success:function(data){
            window.archivaDevMode=data.devMode;
            window.archivaJavascriptLog=data.javascriptLog;
            window.archivaRuntimeInfo=data;
            require.config({
                baseUrl: "js/"
              });
            // CacheBust is for dev purpose use false in prod env !
            var options = {
                AlwaysPreserveOrder:true,
                BasePath:"js/",
                explicit_preloading:false,
                CacheBust:window.archivaDevMode
            };
            $LAB.setGlobalDefaults(options);
            $LAB
               .script(buildLoadJsUrl("jquery.tmpl.js")).wait()
               .script(buildLoadJsUrl("archiva/utils.js")).wait()
               .script(buildLoadJsUrl("archiva/i18nload.js")).wait()
               .script("jquery.cookie.1.0.0.js").wait()
               .script(buildLoadJsUrl("knockout-debug.js")).wait()
               .script("jquery-ui-1.8.16.custom.min.js").wait()
               .script(buildLoadJsUrl("jquery.validate.js")).wait()
               .script("jquery.json-2.3.min.js").wait()
               .script(buildLoadJsUrl("archiva/main-tmpl.js")).wait()
               .script(buildLoadJsUrl("archiva/general-admin.js"))
               .script(buildLoadJsUrl("archiva/repositories.js")).wait()
               .script(buildLoadJsUrl("archiva/network-proxies.js")).wait()
               .script(buildLoadJsUrl("archiva/proxy-connectors.js")).wait()
               .script(buildLoadJsUrl("redback/operation.js")).wait()
               .script(buildLoadJsUrl("archiva/repository-groups.js")).wait()
               .script(buildLoadJsUrl("archiva/search.js")).wait()
               .script(buildLoadJsUrl("redback/redback-tmpl.js")).wait()
               .script("chosen.jquery.js" )
               .script("bootstrap.2.0.2.js" )
               .script(buildLoadJsUrl("knockout.simpleGrid.js"))
               .script(buildLoadJsUrl("knockout-sortable.js"))
               //.script("knockout.mapping-latest.debug.js")
               .script(buildLoadJsUrl("redback/user.js")).wait()
               .script(buildLoadJsUrl("redback/users.js")).wait()
               .script(buildLoadJsUrl("redback/redback.js")).wait()
               .script(buildLoadJsUrl("redback/register.js")).wait()
               .script(buildLoadJsUrl("redback/permission.js")).wait()
               .script(buildLoadJsUrl("redback/resource.js")).wait()
               .script(buildLoadJsUrl("redback/roles.js")).wait()
               .script(buildLoadJsUrl("archiva/main.js"));
        }
      })
  }

});