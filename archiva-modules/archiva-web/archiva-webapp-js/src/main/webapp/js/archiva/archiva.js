appendArchivaVersion=function(){
  return "_archivaVersion="+window.archivaRuntimeInfo.version;
}

timestampNoCache=function(){
  if (window.archivaRuntimeInfo.version.match("SNAPSHOT$")=="SNAPSHOT"){
    return "&archivaTimestamp="+window.archivaRuntimeInfo.timestamp+(window.archivaRuntimeInfo.devMode?"&_="+jQuery.now():"");
  }
  return "";
}

appendTemplateUrl=function(){
  return appendArchivaVersion()+timestampNoCache();
}

appendJsNoCacheUrl=function(){
  return appendArchivaVersion()+timestampNoCache();
}

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
                      'archiva.main':['jquery','sammy','jquery.tmpl'],
                      'utils':['jquery','jquery.tmpl'],
                      'archiva.templates': ['jquery','jquery.tmpl','utils'],
                      'redback.templates': ['jquery','jquery.tmpl','utils']
                      },
                paths: {
                    "i18n":"jquery.i18n.properties-1.0.9",
                    "jquery": "jquery-1.8.0.min",
                    "jquery.tmpl": "jquery.tmpl",
                    "utils": "archiva/utils",
                    "startup": "archiva/startup",
                    "jquery.ui": "jquery-ui-1.8.16.custom.min",
                    "jquery.ui.widget": "jquery.ui.widget-1.8.18",
                    "jquery.cookie": "jquery.cookie.1.0.0",
                    "bootstrap": "bootstrap.2.1.0",
                    "choosen": "chosen.jquery-0.9.8",
                    "jquery.validate": "jquery.validate-1.9.0",
                    "jquery.json": "jquery.json-2.3.min",
                    "knockout": "knockout-2.0.0.debug",
                    "knockout.simpleGrid": "knockout.simpleGrid",
                    "knockout.sortable": "knockout-sortable",
                    "jquery.iframe.transport": "jquery.iframe-transport-1.4",
                    "jquery.fileupload": "jquery.fileupload-5.10.0",
                    "jquery.fileupload.ip":"jquery.fileupload-ip-1.0.6",
                    "jquery.fileupload.ui":"jquery.fileupload-ui-6.6.3",
                    "tmpl": "tmpl.min",
                    "prettify": "prettify",
                    "sammy": "sammy.0.7.1",
                    "jqueryFileTree": "jqueryFileTree-1.0.1",
                    "redback": "redback/redback",
                    "redback.roles": "redback/roles",
                    "redback.user": "redback/user",
                    "redback.users": "redback/users",
                    "redback.templates": "redback/redback-tmpl",
                    "archiva.general-admin":"archiva/general-admin",
                    "archiva.templates": "archiva/main-tmpl",
                    "archiva.repositories": "archiva/repositories",
                    "archiva.network-proxies": "archiva/network-proxies",
                    "archiva.proxy-connectors": "archiva/proxy-connectors",
                    "archiva.repository-groups": "archiva/repository-groups",
                    "archiva.artifacts-management": "archiva/artifacts-management",
                    "archiva.search": "archiva/search",
                    "archiva.main": "archiva/main"
                }
            });

            requirejs(['jquery','jquery.tmpl','i18n','sammy','startup','utils','domReady!'], function (domReady) {
                loadi18n(function () {
                  $.ajax({
                      url: "restServices/archivaUiServices/runtimeInfoService/archivaRuntimeInfo/"+usedLang(),
                      dataType: 'json',
                      success:function(data){
                          window.archivaDevMode=data.devMode;
                          window.archivaJavascriptLog=data.javascriptLog;
                          window.archivaRuntimeInfo=data;

                          require(['sammy','jquery','jquery.tmpl','archiva.main','utils','domReady!'],function (domReady) {
                              startArchivaApplication();
                              $("#loadingDiv").hide();
                          })
                      }
                    })
                })
            });
    }
})
