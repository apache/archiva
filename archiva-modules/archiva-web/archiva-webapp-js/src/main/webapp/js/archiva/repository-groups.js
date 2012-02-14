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

  RepositoryGroupViewModel=function(){
    this.repositoryGroups=ko.observableArray([]);
    this.managedRepositories=ko.observableArray([]);

    findManagedRepository=function(id){
      return findManagedRepository(id,self.managedRepositories());
    }
  }

  displayRepositoryGroups=function(){
    screenChange();
    var mainContent = $("#main-content");
    mainContent.html(mediumSpinnerImg());
    this.repositoryGroupViewModel=new RepositoryGroupViewModel();
    var self=this;

    loadManagedRepositories(function(data) {
      self.repositoryGroupViewModel.managedRepositories(mapManagedRepositories(data));

      $.ajax("restServices/archivaServices/repositoryGroupService/getRepositoriesGroups", {
          type: "GET",
          dataType: 'json',
          success: function(data) {
            var mappedRepositoryGroups=mapRepositoryGroups(data);
            mainContent.html($("#repositoryGroupsMain").tmpl());
            self.repositoryGroupViewModel.repositoryGroups(mappedRepositoryGroups);
            //ko.applyBindings(repositoryGroupViewModel,mainContent.find("#repository-groups-table" ).get(0));
            ko.applyBindings(repositoryGroupViewModel,mainContent.get(0));

          }
        }
      );

    });


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
