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

require(["jquery","i18n","js/archiva/utils.js"],
function() {


  appendArchivaVersion=function(){
    return "_archivaVersion="+window.archivaRuntimeInfo.version;
  }

  buildLoadJsUrl=function(srcScript){
    return srcScript+"?"+appendArchivaVersion()+"&_"+jQuery.now();
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
                baseUrl: "js/",
                "paths": {
                    "i18n":"jquery.i18n.properties-1.0.9",
                    "jquery": "jquery-1.7.2",
                    "redback": buildLoadJsUrl("redback/redback.js"),
                    "utils":  buildLoadJsUrl("archiva/utils.js"),
                    "i18nLoad":  buildLoadJsUrl("archiva/i18nload.js"),
                    "jquerytmpl":  buildLoadJsUrl("jquery.tmpl.js"),
                    "jquery_ui": "jquery-ui-1.8.16.custom.min"
                }
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
               .script("knockout-2.0.0.debug.js").wait()
               //.script("jquery-ui-1.8.16.custom.min.js").wait()
               .script("jquery.validate-1.9.0.js").wait()
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
               .script("chosen.jquery-0.9.7.js" )
               .script("bootstrap.2.0.2.js" )
               .script(buildLoadJsUrl("knockout.simpleGrid.js"))
               .script(buildLoadJsUrl("knockout-sortable.js"))
               //.script("jquery.iframe-transport-1.4.js").wait()
               //.script("jquery.fileupload-5.10.0.js").wait()
               //.script("jquery.fileupload-ip-1.0.6.js").wait()
               //.script("jquery.fileupload-ui-6.6.3.js" ).wait()
               .script(buildLoadJsUrl("redback/user.js")).wait()
               .script(buildLoadJsUrl("redback/users.js")).wait()
               //.script(buildLoadJsUrl("redback/redback.js")).wait()
               .script(buildLoadJsUrl("redback/register.js")).wait()
               .script(buildLoadJsUrl("redback/permission.js")).wait()
               .script(buildLoadJsUrl("redback/resource.js")).wait()
               .script(buildLoadJsUrl("redback/roles.js")).wait()
               .script(buildLoadJsUrl("archiva/main.js"));
        }
      })
  }




});