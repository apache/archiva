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

  RepositoryGroup=function(id,repositories){

    var self=this;

    //private String id;
    this.id=ko.observable(id);
    this.id.subscribe(function(newValue){self.modified(true)});

    // private List<String> repositories;
    this.repositories=ko.observableArray(repositories);
    this.repositories.subscribe(function(newValue){self.modified(true)});

    // to store managedRepositories description not sended to server
    this.managedRepositories=ko.observableArray([]);
    this.managedRepositories.subscribe(function(newValue){self.modified(true)});

    this.modified=ko.observable(false);
  }

  RepositoryGroupViewModel=function(repositoryGroup,update,repositoryGroupsViewModel){
    var self = this;
    this.repositoryGroup=repositoryGroup;
    this.update=update;
    this.repositoryGroupsViewModel=repositoryGroupsViewModel;
    this.availableRepositories=ko.observableArray([]);

    for (var i=0;i<repositoryGroupsViewModel.managedRepositories().length;i++){
      if ( $.inArray(repositoryGroupsViewModel.managedRepositories()[i].id(),this.repositoryGroup.repositories())<0){
        this.availableRepositories.push(repositoryGroupsViewModel.managedRepositories()[i]);
      }
    }

    repositoryMoved=function(arg){
      $.log("repositoryMoved:"+arg.sourceIndex+" to " + arg.targetIndex);
      var repositories=[];
      for(var i=0;i<self.repositoryGroup.managedRepositories().length;i++){
        repositories.push(self.repositoryGroup.managedRepositories()[i].id());
      }
      self.repositoryGroup.repositories(repositories);
      self.repositoryGroup.modified(true);
    }
    this.saveRepositoryGroup=function(repositoryGroup){
      if (self.update){
        self.repositoryGroupsViewModel.saveRepositoryGroup(repositoryGroup);
      } else {
        self.repositoryGroupsViewModel.addRepositoryGroup(repositoryGroup);
      }
    }

    this.removeRepository=function(id){
      $.log("removeRepository:"+id);
    }
  }

  RepositoryGroupsViewModel=function(){
    var self=this;
    this.repositoryGroups=ko.observableArray([]);
    this.managedRepositories=ko.observableArray([]);

    this.findManagedRepository=function(id){
      return findManagedRepository(id,self.managedRepositories());
    }
    this.deleteRepositoryGroup=function(repositoryGroup){
      openDialogConfirm(
          function(){self.removeRepositoryGroup(repositoryGroup);window.modalConfirmDialog.modal('hide')},
          $.i18n.prop('ok'), $.i18n.prop('cancel'),
          $.i18n.prop('repository.group.delete.confirm',repositoryGroup.id()),
          $("#repository-group-delete-warning-tmpl").tmpl(self.repositoryGroup));
    }
    this.removeRepositoryGroup=function(repositoryGroup){
      clearUserMessages();
      $.ajax("restServices/archivaServices/repositoryGroupService/deleteRepositoryGroup/"+encodeURIComponent(repositoryGroup.id()),
        {
          type: "GET",
          success: function(data) {
            var message=$.i18n.prop('repository.group.deleted',repositoryGroup.id());
            displaySuccessMessage(message);
            self.repositoryGroups.remove(repositoryGroup);
          },
          error: function(data) {
            var res = $.parseJSON(data.responseText);
            displayRestError(res);
          }
        }
      );
    }

    this.editRepositoryGroup=function(repositoryGroup){
      $.log("editRepositoryGroup:"+repositoryGroup.id()+",repositories:"+repositoryGroup.repositories().length+",managed:"+repositoryGroup.managedRepositories().length);
      var repositoryGroupViewModel=new RepositoryGroupViewModel(repositoryGroup,true,self);
      activateRepositoryGroupEditTab();
      ko.applyBindings(repositoryGroupViewModel,$("#main-content #repository-groups-edit" ).get(0));
      $("#main-content #repository-groups-view-tabs-li-edit a").html($.i18n.prop("edit"));
    }

    this.saveRepositoryGroup=function(repositoryGroup){
        clearUserMessages();
        $.ajax("restServices/archivaServices/repositoryGroupService/updateRepositoryGroup",
          {
            type: "POST",
            contentType: 'application/json',
            data: "{\"repositoryGroup\": " + ko.toJSON(repositoryGroup)+"}",
            dataType: 'json',
            success: function(data) {
              $.log("update repositoryGroup id:"+repositoryGroup.id());
              var message=$.i18n.prop('repository.group.updated',repositoryGroup.id());
              displaySuccessMessage(message);
              repositoryGroup.modified(false);
            },
            error: function(data) {
              var res = $.parseJSON(data.responseText);
              displayRestError(res);
            }
          }
        );

    }

    this.addRepositoryGroup=function(repositoryGroup){
      clearUserMessages();
      $.ajax("restServices/archivaServices/repositoryGroupService/addRepositoryGroup",
        {
          type: "POST",
          contentType: 'application/json',
          data: "{\"repositoryGroup\": " + ko.toJSON(repositoryGroup)+"}",
          dataType: 'json',
          success: function(data) {
            $.log("update repositoryGroup id:"+repositoryGroup.id());
            var message=$.i18n.prop('repository.group.added',repositoryGroup.id());
            displaySuccessMessage(message);
            repositoryGroup.modified(false);
            self.repositoryGroups.push(repositoryGroup);
          },
          error: function(data) {
            var res = $.parseJSON(data.responseText);
            displayRestError(res);
          }
        }
      );

    }

    getManagedRepository=function(id){
      $.log("getManagedRepository:"+id);
      return findManagedRepository(self.managedRepositories());
    }

  }

  displayRepositoryGroups=function(){
    screenChange();
    var mainContent = $("#main-content");
    mainContent.html(mediumSpinnerImg());
    this.repositoryGroupsViewModel=new RepositoryGroupsViewModel();
    var self=this;

    loadManagedRepositories(function(data) {
      self.repositoryGroupsViewModel.managedRepositories(mapManagedRepositories(data));

      $.ajax("restServices/archivaServices/repositoryGroupService/getRepositoriesGroups", {
          type: "GET",
          dataType: 'json',
          success: function(data) {
            var mappedRepositoryGroups=mapRepositoryGroups(data);
            for(var i=0;i<mappedRepositoryGroups.length;i++){
              mappedRepositoryGroups[i]
                  .managedRepositories(self.mapManagedRepositoriesToRepositoryGroup(mappedRepositoryGroups[i]));
              mappedRepositoryGroups[i].modified(false);
              $.log("mappedRepositoryGroups.repositories().length:"+mappedRepositoryGroups[i].repositories().length);
            }
            mainContent.html($("#repositoryGroupsMain").tmpl());
            self.repositoryGroupsViewModel.repositoryGroups(mappedRepositoryGroups);
            $.log("displayRepositoryGroups#applyBindings before");
            ko.applyBindings(repositoryGroupsViewModel,mainContent.find("#repository-groups-view" ).get(0));
            $.log("displayRepositoryGroups#applyBindings after");


            mainContent.find("#repository-groups-view-tabs").on('show', function (e) {
              if ($(e.target).attr("href")=="#repository-groups-edit") {
                var repositoryGroup = new RepositoryGroup();
                var repositoryGroupViewModel=new RepositoryGroupViewModel(repositoryGroup,false,self.repositoryGroupsViewModel);
                activateRepositoryGroupEditTab();
                ko.applyBindings(repositoryGroupViewModel,mainContent.find("#repository-groups-edit" ).get(0));
              }
              if ($(e.target).attr("href")=="#repository-groups-view") {
                mainContent.find("#repository-groups-view-tabs-li-edit a").html($.i18n.prop("add"));
                clearUserMessages();
              }

            });

          }
        }
      );

    });

    this.mapManagedRepositoriesToRepositoryGroup=function(repositoryGroup){
      $.log("mapManagedRepositoriesToRepositoryGroup");
      var managedRepositories=new Array();
      if (!repositoryGroup.repositories()) {
        repositoryGroup.repositories(new Array());
        return managedRepositories;
      }
      for(var i=0;i<repositoryGroup.repositories().length;i++){
        managedRepositories.push(self.repositoryGroupsViewModel.findManagedRepository(repositoryGroup.repositories()[i]));
      }
      $.log("end mapManagedRepositoriesToRepositoryGroup");
      return managedRepositories;
    }

  }



  activateRepositoryGroupsGridTab=function(){
    var mainContent = $("#main-content");
    mainContent.find("#repository-groups-view-tabs-content div[class*='tab-pane']").removeClass("active");
    mainContent.find("#repository-groups-view-tabs li").removeClass("active");

    mainContent.find("#repository-groups-view").addClass("active");
    mainContent.find("#repository-groups-view-tabs-li-grid").addClass("active");
    mainContent.find("#repository-groups-view-tabs-li-edit a").html($.i18n.prop("add"));

  }

  activateRepositoryGroupEditTab=function(){
    var mainContent = $("#main-content");

    mainContent.find("#repository-groups-view-tabs-content div[class*='tab-pane']").removeClass("active");
    mainContent.find("#repository-groups-view-tabs li").removeClass("active");

    mainContent.find("#repository-groups-edit").addClass("active");
    mainContent.find("#repository-groups-view-tabs-li-edit").addClass("active");
  }

  mapRepositoryGroups=function(data){
    if (data == null){
      return new Array();
    }
    var mappedRepositoryGroups = $.map(data.repositoryGroup, function(item) {
      return mapRepositoryGroup(item);
    });
    return mappedRepositoryGroups;
  }

  mapRepositoryGroup=function(data){
    return new RepositoryGroup(data.id, mapStringArray(data.repositories));
  }

});
