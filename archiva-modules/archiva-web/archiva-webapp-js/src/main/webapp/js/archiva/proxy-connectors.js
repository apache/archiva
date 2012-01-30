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

    editProxyConnector=function(proxyConnector){

    }

    this.findUniqueManagedRepos=function(){
      var sourcesRepos=[];
      //sourceRepoId
      for(i=0;i<self.proxyConnectors().length;i++){
        var curSrcRepo=self.proxyConnectors()[i].sourceRepoId();
        var curTarget=self.proxyConnectors()[i].targetRepoId();
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

    this.displayGrid=function(){
      self.managedRepositoryConnectorViews(this.findUniqueManagedRepos());
      $.log("uniqueManagedRepos:"+self.managedRepositoryConnectorViews().length);
      this.gridViewModel = new ko.simpleGrid.viewModel({
        data: self.managedRepositoryConnectorViews,
        pageSize: 5,
        gridUpdateCallBack: function(){
          $("#main-content #proxyConnectorsTable [title]").twipsy();
        }
      });
      ko.applyBindings(this,$("#main-content #proxyConnectorsTable").get(0));
      removeSmallSpinnerImg("#main-content");
      $("#main-content #proxy-connectors-view-tabs").tabs();
    }
  }

  displayProxyConnectors=function(){
    $("#main-content").html($("#proxyConnectorsMain").tmpl());
    $("#main-content").append(smallSpinnerImg());

    var proxyConnectorsViewModel = new ProxyConnectorsViewModel();

    $.ajax("restServices/archivaServices/proxyConnectorService/getProxyConnectors", {
        type: "GET",
        dataType: 'json',
        success: function(data) {
          proxyConnectorsViewModel.proxyConnectors(mapProxyConnectors(data));
          proxyConnectorsViewModel.displayGrid();
        }
      }
    );

    $.ajax("restServices/archivaServices/proxyConnectorService/allPolicies", {
        type: "GET",
        dataType: 'json',
        success: function(data) {
          mapPolicyInformations(data);
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