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

    this.modified=ko.observable(false);
  }

  RepositoryGroupViewModel=function(repositoryGroup,update,repositoryGroupsViewModel){
    this.repositoryGroup=repositoryGroup;
    this.update=update;
    this.repositoryGroupsViewModel=null;
  }

  RepositoryGroupsViewModel=function(){
    var self=this;
    this.repositoryGroups=ko.observableArray([]);
    this.managedRepositories=ko.observableArray([]);

    this.findManagedRepository=function(id){
      return findManagedRepository(id,self.managedRepositories());
    }

    this.deleteRepositoryGroup=function(repositoryGroup){
      $.log("deleteRepositoryGroup:"+repositoryGroup.id());
    }

    this.editRepositoryGroup=function(repositoryGroup){
      $.log("editRepositoryGroup:"+repositoryGroup.id());
      var repositoryGroupViewModel=new RepositoryGroupViewModel(repositoryGroup,true,self);
      activateRepositoryGroupEditTab();
      ko.applyBindings(repositoryGroupViewModel,$("#main-content #repository-groups-edit" ).get(0));
    }

    repositoryMoved=function(){
      $.log("repositoryMoved");
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
            mainContent.html($("#repositoryGroupsMain").tmpl());
            self.repositoryGroupsViewModel.repositoryGroups(mappedRepositoryGroups);
            //ko.applyBindings(repositoryGroupViewModel,mainContent.find("#repository-groups-table" ).get(0));
            ko.applyBindings(repositoryGroupsViewModel,mainContent.find("#repository-groups-view" ).get(0));

          }
        }
      );

    });


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
