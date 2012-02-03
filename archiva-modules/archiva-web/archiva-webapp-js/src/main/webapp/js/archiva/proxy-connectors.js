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

  ProxyConnector=function(sourceRepoId,targetRepoId,proxyId,blackListPatterns,whiteListPatterns,policies,properties,
                          disabled,order){
    //private String sourceRepoId;
    this.sourceRepoId=ko.observable(sourceRepoId);
    //private String targetRepoId;
    this.targetRepoId=ko.observable(targetRepoId);
    //private String proxyId;
    this.proxyId=ko.observable(proxyId);
    //private List<String> blackListPatterns;
    this.blackListPatterns=ko.observableArray(blackListPatterns);
    //private List<String> whiteListPatterns;
    this.whiteListPatterns=ko.observableArray(whiteListPatterns);
    //private Map<String, String> policies;
    this.policies=ko.observable(policies);
    //private Map<String, String> properties;
    this.properties=ko.observable(properties);
    //private boolean disabled = false;
    this.disabled=ko.observable(disabled);
    //private int order = 0;
    this.order=ko.observable(order);
  }

  PolicyInformation=function(options,defaultOption,id,name){
    //private List<String> options;
    this.options=ko.observableArray(options);
    //private String defaultOption;
    this.defaultOption=ko.observable(defaultOption);
    //private String id;
    this.id=ko.observable(id);
    //private String name;
    this.name=ko.observable(name);
  }

  ManagedRepositoryConnectorView=function(source,targetRepos){
    this.source=ko.observable(source);
    this.targetRepos=ko.observableArray(targetRepos);
  }

  ProxyConnectorsViewModel=function(){
    this.proxyConnectors=ko.observableArray([]);
    var self=this;
    this.managedRepositoryConnectorViews=ko.observableArray([]);
    this.policyInformations=ko.observableArray([]);
    this.managedRepositories=ko.observableArray([]);
    this.remoteRepositories=ko.observableArray([]);
    editProxyConnector=function(proxyConnector){

    }

    this.findUniqueManagedRepos=function(){
      var sourcesRepos=[];
      //sourceRepoId
      for(i=0;i<self.proxyConnectors().length;i++){
        var curSrcRepo=self.proxyConnectors()[i].sourceRepoId();
        var curTarget=self.proxyConnectors()[i];
        var sourceRepo = $.grep(sourcesRepos,
                                function(srcRepo,idx){
                                  for (j=0;j<sourcesRepos.length;j++){
                                    if (srcRepo.source()==curSrcRepo){
                                      return true;
                                    }
                                  }
                                  return false;
                                }
        );
        if (sourceRepo.length>0){
          sourceRepo[0].targetRepos.push(curTarget);
        } else {
          sourcesRepos.push(new ManagedRepositoryConnectorView(curSrcRepo,[curTarget]));
        }
      }
      return sourcesRepos;
    }

    getManagedRepository=function(id){
      var managedRepository=$.grep(self.managedRepositories(),
                                      function(repo,idx){
                                        return repo.id()==id;
                                      }
                            );
      return ($.isArray(managedRepository) && managedRepository.length>0) ? managedRepository[0]:new ManagedRepository();
    }

    getRemoteRepository=function(id){
      var remoteRepository=$.grep(self.remoteRepositories(),
                                      function(repo,idx){
                                        return repo.id()==id;
                                      }
                            );
      return ($.isArray(remoteRepository) && remoteRepository.length>0) ? remoteRepository[0]:new RemoteRepository();
    }

    this.getProxyConnector=function(sourceRepoId,targetRepoId){
      var proxyConnector=$.grep(self.proxyConnectors(),
                                      function(proxyConnector,idx){
                                        return proxyConnector.sourceRepoId()==sourceRepoId
                                            && proxyConnector.targetRepoId==targetRepoId;
                                      }
                                  );
      return ($.isArray(proxyConnector) && proxyConnector.length>0) ? proxyConnector[0]:new ProxyConnector();
    }

    showSettings=function(sourceRepoId,targetRepoId){
      //proxy-connectors-grid-remoterepo-settings-edit-internal-central
      var targetImgId="#proxy-connectors-grid-remoterepo-settings-edit-"+sourceRepoId+"-"+targetRepoId;
      //proxy-connectors.grid-remoterepo-settings-content-internal-central
      var targetContentId="#proxy-connectors-grid-remoterepo-settings-content-"+sourceRepoId+"-"+targetRepoId;
      $(targetContentId).html("");
      $(targetContentId).append($("#proxy-connectors-remote-settings-popover-tmpl").tmpl(self.getProxyConnector(sourceRepoId,targetRepoId)));
      $(targetImgId).attr("data-content",$(targetContentId).html());
      $(targetImgId).popover(
          {
            placement: "left",
            html: true,
            title: "popover-title"
          }
      );

      $(targetImgId).popover('show');

    }

    this.displayGrid=function(){
      self.managedRepositoryConnectorViews(this.findUniqueManagedRepos());
      $.log("uniqueManagedRepos:"+self.managedRepositoryConnectorViews().length);
      this.gridViewModel = new ko.simpleGrid.viewModel({
        data: self.managedRepositoryConnectorViews,
        pageSize: 5,
        gridUpdateCallBack: function(){
          $("#main-content #proxyConnectorsTable [title]").tooltip();
        }
      });
      this.gridViewModel.getManagedRepository=getManagedRepository;
      ko.applyBindings(this,$("#main-content #proxyConnectorsTable").get(0));
      removeSmallSpinnerImg("#main-content");
      $("#main-content #proxy-connectors-view-tabs a:first").tab('show');
    }

  }

  // FIXME use various callback to prevent async false !!

  displayProxyConnectors=function(){
    $("#main-content").html($("#proxyConnectorsMain").tmpl());
    $("#main-content").append(smallSpinnerImg());

    this.proxyConnectorsViewModel = new ProxyConnectorsViewModel();
    var self=this;

    $.ajax("restServices/archivaServices/managedRepositoriesService/getManagedRepositories", {
        type: "GET",
        dataType: 'json',
        async: false,
        success: function(data) {
          self.proxyConnectorsViewModel.managedRepositories(mapManagedRepositories(data));
        }
    });

    $.ajax("restServices/archivaServices/remoteRepositoriesService/getRemoteRepositories", {
        type: "GET",
        dataType: 'json',
        async: false,
        success: function(data) {
          self.proxyConnectorsViewModel.remoteRepositories(mapRemoteRepositories(data));
        }
    });

    $.ajax("restServices/archivaServices/proxyConnectorService/allPolicies", {
        type: "GET",
        dataType: 'json',
        async: false,
        success: function(data) {
          self.proxyConnectorsViewModel.policyInformations(mapPolicyInformations(data));
        }
      }
    );

    $.ajax("restServices/archivaServices/proxyConnectorService/getProxyConnectors", {
        type: "GET",
        dataType: 'json',
        success: function(data) {
          self.proxyConnectorsViewModel.proxyConnectors(mapProxyConnectors(data));
          self.proxyConnectorsViewModel.displayGrid();
        }
      }
    );

  }

  mapProxyConnector=function(data){
    if (data==null){
      return null;
    }
    return new ProxyConnector(data.sourceRepoId,data.targetRepoId,data.proxyId,mapStringArray(data.blackListPatterns),
                              mapStringArray(data.whiteListPatterns),data.policies,data.properties,
                              data.disabled,data.order);
  }

  mapProxyConnectors=function(data){
    var mappedProxyConnectors = $.map(data.proxyConnector, function(item) {
      return mapProxyConnector(item);
    });
    return mappedProxyConnectors;
  }

  mapPolicyInformation=function(data){
    if (data==null){
      return null;
    }
    return new PolicyInformation(mapStringArray(data.options),data.defaultOption,data.id,data.name);
  }

  mapPolicyInformations=function(data){
    return $.map(data.policyInformation, function(item) {
              return mapPolicyInformation(item);
           });
  }

});