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
define("archiva.docs",["jquery","i18n","jquery.tmpl","bootstrap"], function() {

  displayRestDocs=function(){
    $.log("displayRestDocs");
    screenChange();
    $("#main-content" ).html($("#rest_docs").tmpl());
  }

  goToArchivaRestDoc=function(target){
    goToRestDoc(target,"rest-docs/rest-docs-archiva-rest-api");
  }

  goToArchivaRestUiDoc=function(target){

    goToRestDoc(target,"rest-docs/rest-docs-archiva-ui");
  }

  goToRedbackRestDoc=function(target){

    goToRestDoc(target,"rest-docs/rest-docs-redback-rest-api");
  }

  goToRestDoc=function(target,rootPath){
    var mainContent = $("#main-content");
    mainContent.find("#rest_docs_content" ).html(mediumSpinnerImg());
    $.ajax({
      url:rootPath+"/"+target,
      type:"get",
      dataType: "html",
      success: function(data){
        mainContent.html($("#rest_docs").tmpl());
        if(target.endsWith(".xsd")){
          mainContent.find("#rest_docs_content" ).html("<code id='xsd_content'></code>");
          mainContent.find("#xsd_content" ).html(data);
        } else {
          mainContent.find("#rest_docs_content" ).html(data);
        }

        prettyPrint();
      }
    });
  }

  displayRedbackRestDocs=function(){
    window.sammyArchivaApplication.setLocation("#rest-docs-redback-rest-api/index.html");
  }

  displayArchivaRestUIDocs=function(){
    window.sammyArchivaApplication.setLocation("#rest-docs-archiva-ui/index.html");
  }

  displayArchivaRestDocs=function(){
    window.sammyArchivaApplication.setLocation("#rest-docs-archiva-rest-api/index.html");
  }

  loadRestDocs=function(docType, fullPath){
    $.log("loadRestDocs:"+docType+","+fullPath);
    //if (docType=='rest-docs-archiva-rest-api'){
      $.ajax({
        url:fullPath,
        type:"get",
        dataType: "html",
        success: function(data){
          $("#main-content" ).find("#rest_docs_content" ).html(data);
          prettyPrint();
        }
      });
    //}
  }

  displayUsersDocs=function(){
    $.log("displayUsersDocs");
    window.open("");
  }

});