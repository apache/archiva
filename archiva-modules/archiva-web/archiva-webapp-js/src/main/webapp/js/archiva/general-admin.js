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

  //-------------------------
  // legacy path part
  //-------------------------

  LegacyArtifactPath=function(path,groupId,artifactId,version,classifier,type,update){
    //private String path;
    this.path=ko.observable(path);

    /**
     * The artifact reference, as " [groupId] :
     * [artifactId] : [version] : [classifier] : [type] ".
     */
    //private String artifact;
    //this.artifact=ko.observable(artifact);
    this.update=update;
    //private String groupId;
    this.groupId=ko.observable(groupId);

    //private String artifactId;
    this.artifactId=ko.observable(artifactId);

    //private String version;
    this.version=ko.observable(version);

    //private String classifier;
    this.classifier=ko.observable(classifier);

    //private String type;
    this.type=ko.observable(type);

    this.modified=ko.observable();

    this.artifact = ko.computed(function() {
      var artifactValue="";
      if (this.groupId()){
        artifactValue+=this.groupId();
      }
      if (this.artifactId()){
        artifactValue+=":"+this.artifactId();
      }
      if (this.version()){
        artifactValue+=":"+this.version();
      }
      if (this.classifier()){
        artifactValue+=":"+this.classifier();
      }
      if (this.type()){
        artifactValue+=":"+this.type();
      }
      return artifactValue;
    }, this);
  }

  mapLegacyArtifactPaths=function(data){
    if (data){
      return $.isArray(data)? $.map(data,function(item){
        return mapLegacyArtifactPath(item);
      }):[mapLegacyArtifactPath(data)];
    }
    return [];
  }

  mapLegacyArtifactPath=function(data){
    return data?new LegacyArtifactPath(data.path,data.groupId,data.artifactId,data.version,data.classifier,data.type):null;
  }

  activateLegacyArtifactPathFormValidation=function(){
    var theForm=$("#main-content #legacy-artifact-paths-edit-form");
    var validator = theForm.validate({
      showErrors: function(validator, errorMap, errorList) {
       customShowError("#main-content #legacy-artifact-paths-edit-form",validator,errorMap,errorMap);
      }
    });
  }

  LegacyArtifactPathViewModel=function(legacyArtifactPath,update,legacyArtifactPathsViewModel){
    var self=this;
    this.update=update;
    this.legacyArtifactPath=legacyArtifactPath;
    this.legacyArtifactPathsViewModel=legacyArtifactPathsViewModel;

    this.display=function(){
      var mainContent=$("#main-content");
      ko.applyBindings(self,mainContent.find("#legacy-artifact-paths-edit" ).get(0));
      mainContent.find("#legacy-artifact-paths-view-tabs-li-edit a").html($.i18n.prop("edit"));
      activateLegacyArtifactPathFormValidation();
      activateLegacyArtifactPathsEditTab();
    }

    displayGrid=function(){
      activateLegacyArtifactPathsGridTab();
    }

    calculatePath=function(){
      var path="";
      if (self.legacyArtifactPath.groupId()){
        path+=self.legacyArtifactPath.groupId()+"/jars/";
      }
      if (self.legacyArtifactPath.artifactId()){
        path+=self.legacyArtifactPath.artifactId();
      }
      if (self.legacyArtifactPath.version()){
        path+="-"+self.legacyArtifactPath.version();
      }
      if (self.legacyArtifactPath.classifier()){
        path+="-"+self.legacyArtifactPath.classifier();
      }
      if (self.legacyArtifactPath.type()){
        path+="."+self.legacyArtifactPath.type();
      }
      self.legacyArtifactPath.path(path);
    }

    this.save=function(){
      var theForm=$("#main-content #legacy-artifact-paths-edit-form");
      if (!theForm.valid()){
        return;
      }
      // do that on server side
      /*if (theForm.find("#artifact" ).val()
          !=theForm.find("#path" ).val()){
        var errorList=[{
          message: $.i18n.prop("path must match artifact"),
    		  element: theForm.find("#path" ).get(0)
        }];
        customShowError("#main-content #legacy-artifact-paths-edit-form", null, null, errorList);
        return;
      }*/
      // TODO call id exists if add ?
      clearUserMessages();
      $.log("save ok");
      if (self.update){
        $.log("update");
      }else {
        $.ajax("restServices/archivaServices/archivaAdministrationService/addLegacyArtifactPath",
          {
            type: "POST",
            contentType: 'application/json',
            data: ko.toJSON(self.legacyArtifactPath),
            dataType: 'json',
            success: function(data) {
              self.legacyArtifactPath.modified(false);
              self.legacyArtifactPathsViewModel.legacyArtifactPaths.push(self.legacyArtifactPath);
              displaySuccessMessage($.i18n.prop('legacy-artifact-path.added',self.legacyArtifactPath.path()));
              activateLegacyArtifactPathsGridTab();
            },
            error: function(data) {
              var res = $.parseJSON(data.responseText);
              displayRestError(res);
            }
          }
        );
      }
    }
  }

  LegacyArtifactPathsViewModel=function(){
    var self=this;
    this.legacyArtifactPaths=ko.observableArray([]);

    this.gridViewModel = new ko.simpleGrid.viewModel({
      data: self.legacyArtifactPaths,
      columns: [
        {
          headerText: $.i18n.prop('legacy-artifact-paths.path'),
          rowText: "path"
        },
        {
          headerText: $.i18n.prop('legacy-artifact-paths.artifact'),
          rowText: "artifact"
        }
      ],
      pageSize: 5,
      gridUpdateCallBack: function(networkProxy){
        $("#main-content #legacy-artifact-paths-table [title]").tooltip();
      }
    });


    editLegacyArtifactPath=function(legacyArtifactPath){
      var legacyArtifactPathViewModel=new LegacyArtifactPathViewModel(legacyArtifactPath,true);
      legacyArtifactPathViewModel.display();
    }

    removeLegacyArtifactPath=function(legacyArtifactPath){

    }

    updateLegacyArtifactPath=function(legacyArtifactPath){

    }

  }

  displayLegacyArtifactPathSupport=function(){
    clearUserMessages();
    var mainContent=$("#main-content");

    mainContent.html($("#legacy-artifact-path-main" ).html());

    $.ajax("restServices/archivaServices/archivaAdministrationService/getLegacyArtifactPaths", {
        type: "GET",
        dataType: 'json',
        success: function(data){
          var legacyArtifactPathsViewModel=new LegacyArtifactPathsViewModel();
          var legacyPaths=mapLegacyArtifactPaths(data);
          $.log("legacyPaths:"+legacyPaths.length);
          legacyArtifactPathsViewModel.legacyArtifactPaths(legacyPaths);
          ko.applyBindings(legacyArtifactPathsViewModel,mainContent.find("#legacy-artifact-paths-view" ).get(0));

          mainContent.find("#legacy-artifact-paths-view-tabs").on('show', function (e) {
            if ($(e.target).attr("href")=="#legacy-artifact-paths-edit") {
              var viewModel = new LegacyArtifactPathViewModel(new LegacyArtifactPath(),false,legacyArtifactPathsViewModel);
              viewModel.display();
              activateLegacyArtifactPathFormValidation();
              clearUserMessages();
            }
            if ($(e.target).attr("href")=="#legacy-artifact-paths-view") {
              mainContent.find("#legacy-artifact-paths-view-tabs-li-edit a").html($.i18n.prop("add"));
              clearUserMessages();
            }

          });


          activateLegacyArtifactPathsGridTab();
        }
    });


  }


  activateLegacyArtifactPathsGridTab=function(){
    var mainContent = $("#main-content");
    mainContent.find("#legacy-artifact-paths-view-tabs-li-edit").removeClass("active");
    mainContent.find("#legacy-artifact-paths-edit").removeClass("active");

    mainContent.find("#legacy-artifact-paths-view-tabs-li-grid").addClass("active");
    mainContent.find("#legacy-artifact-paths-view").addClass("active");
    mainContent.find("#legacy-artifact-paths-view-tabs-li-edit a").html($.i18n.prop("add"));

  }

  activateLegacyArtifactPathsEditTab=function(){
    var mainContent = $("#main-content");
    mainContent.find("#legacy-artifact-paths-view-tabs-li-grid").removeClass("active");
    mainContent.find("#legacy-artifact-paths-view").removeClass("active");

    mainContent.find("#legacy-artifact-paths-view-tabs-li-edit").addClass("active");
    mainContent.find("#legacy-artifact-paths-edit").addClass("active");
  }


});