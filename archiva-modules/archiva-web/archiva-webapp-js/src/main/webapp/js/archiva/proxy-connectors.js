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
    var self=this;

    //private String sourceRepoId;
    this.sourceRepoId=ko.observable(sourceRepoId);
    this.sourceRepoId.subscribe(function(newValue){
      $.log("modify sourceRepo:"+newValue);
      self.modified(true);
    });

    //private String targetRepoId;
    this.targetRepoId=ko.observable(targetRepoId);
    this.targetRepoId.subscribe(function(newValue){
      $.log("modify targetRepo:"+newValue);
      self.modified(true);
    });

    //private String proxyId;
    this.proxyId=ko.observable(proxyId);
    this.proxyId.subscribe(function(newValue){
      $.log("modify proxyId");
      self.modified(true);
    });

    //private List<String> blackListPatterns;
    this.blackListPatterns=ko.observableArray(blackListPatterns==null?[]:blackListPatterns);
    this.blackListPatterns.subscribe(function(newValue){
      $.log("modify blackListPatterns");
      self.modified(true);
    });

    //private List<String> whiteListPatterns;
    this.whiteListPatterns=ko.observableArray(whiteListPatterns==null?[]:whiteListPatterns);
    this.whiteListPatterns.subscribe(function(newValue){
      $.log("modify whiteListPatterns");
      self.modified(true);
    });

    //private Map<String, String> policies;
    this.policies=ko.observableArray(policies==null?[]:policies);
    this.policies.subscribe(function(newValue){
      $.log("modify policies");
      self.modified(true);
    });

    //private Map<String, String> properties;
    this.properties=ko.observableArray(properties==null?new Array():properties);
    this.properties.subscribe(function(newValue){
      $.log("properties modified");
      self.modified(true);
    });

    //private boolean disabled = false;
    this.disabled=ko.observable(disabled);
    this.disabled.subscribe(function(newValue){
      $.log("modify disabled");
      self.modified(true);
    });

    //private int order = 0;
    this.order=ko.observable(order);
    this.order.subscribe(function(newValue){
      $.log("modify order");
      self.modified(true);
    });

    this.modified=ko.observable(false);
    this.modified.subscribe(function(newValue){$.log("ProxyConnector modified:"+newValue)});
  }

  PolicyInformation=function(options,defaultOption,id,name){

    var self=this;
    this.modified=ko.observable(false);

    //private List<String> options;
    this.options=ko.observableArray(options);
    this.options.subscribe(function(newValue){self.modified(true)});

    //private String defaultOption;
    this.defaultOption=ko.observable(defaultOption);
    this.defaultOption.subscribe(function(newValue){self.modified(true)});

    //private String id;
    this.id=ko.observable(id);
    this.id.subscribe(function(newValue){self.modified(true)});

    //private String name;
    this.name=ko.observable(name);
    this.name.subscribe(function(newValue){self.modified(true)});

  }

  ManagedRepositoryConnectorView=function(source,targetRepos){
    var self=this;
    this.modified=ko.observable(false);


    this.source=ko.observable(source);
    this.targetRepos=ko.observableArray(targetRepos);
  }

  ProxyConnectorViewModel=function(proxyConnector,update,proxyConnectorsViewModel){
    var self=this;
    this.proxyConnector=proxyConnector;
    this.proxyConnectorsViewModel=proxyConnectorsViewModel;
    this.update=update;
    this.modified=ko.observable(false);
    getSelectedPolicyOption=function(id,updateVal){
      $.log("getSelectedPolicyOption:"+id+","+updateVal);
      if (!update){
        // we are on add mode so use default option
        var policyInformations=self.proxyConnectorsViewModel.policyInformations();
        for (i=0;i<policyInformations.length;i++){
          if (policyInformations[i].id()==id) {
            return policyInformations[i].defaultOption;
          }
        }
      }
      var policies=self.proxyConnector().policies();
      if (policies!=null){
        for (i=0;i<policies().length;i++){
          if (id==policies()[i].key()){
            return policies()[i].value();
          }
        }
      }
      return "";
    }
    getPolicyOptions=function(id){
      var policyInformations=self.proxyConnectorsViewModel.policyInformations();
      for(i=0;i<policyInformations.length;i++){
        if (policyInformations[i].id()==id){
          return policyInformations[i].options();
        }
      }
    }

    deleteProperty=function(key){
      $.log("delete property key:"+key());
      for(i=0;i<self.proxyConnector.properties().length;i++){
        var entry=self.proxyConnector.properties()[i];
        if (entry.key()==key()){
          self.proxyConnector.properties.remove(entry);
        }
      }

    }

    addProperty=function(){
      var mainContent=$("#main-content");
      var key=mainContent.find("#property-key").val();
      var value=mainContent.find("#property-value").val();
      var oldTab = self.proxyConnector.properties();
      oldTab.push(new Entry(key,value));
      self.proxyConnector.properties(oldTab);
    }

    addBlacklistPattern=function(){
      var pattern = $("#main-content #blacklist-value").val();
      var tab =  self.proxyConnector.blackListPatterns();
      tab.push(pattern);
      self.proxyConnector.blackListPatterns(tab);

    }

    removeBlacklistPattern=function(pattern){
      self.proxyConnector.blackListPatterns.remove(pattern);
    }

    addWhitelistPattern=function(){
      var pattern = $("#main-content #whitelist-value").val();
      var tab =  self.proxyConnector.whiteListPatterns();
      tab.push(pattern);
      self.proxyConnector.whiteListPatterns(tab);

    }

    removeWhitelistPattern=function(pattern){
      self.proxyConnector.whiteListPatterns.remove(pattern);
    }
  }

  ProxyConnectorsViewModel=function(){
    var self=this;
    this.proxyConnectors=ko.observableArray([]);
    this.managedRepositoryConnectorViews=ko.observableArray([]);
    this.policyInformations=ko.observableArray([]);
    this.managedRepositories=ko.observableArray([]);
    this.remoteRepositories=ko.observableArray([]);
    this.networkProxies=ko.observableArray([]);

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
      var targetContent = $("#proxy-connectors-grid-remoterepo-settings-content-"+sourceRepoId+"-"+targetRepoId);
      targetContent.html("");
      targetContent.append($("#proxy-connectors-remote-settings-popover-tmpl")
                               .tmpl(self.getProxyConnector(sourceRepoId,targetRepoId)));

      var targetImg = $("#proxy-connectors-grid-remoterepo-settings-edit-"+sourceRepoId+"-"+targetRepoId);
      targetImg.attr("data-content",targetContent.html());
      targetImg.popover(
          {
            placement: "left",
            html: true,
            title: "popover-title"
          }
      );

      targetImg.popover('show');

    }

    this.displayGrid=function(){
      self.managedRepositoryConnectorViews(this.findUniqueManagedRepos());
      this.gridViewModel = new ko.simpleGrid.viewModel({
        data: self.managedRepositoryConnectorViews,
        pageSize: 5,
        gridUpdateCallBack: function(){
          $("#main-content #proxyConnectorsTable [title]").tooltip();
        }
      });
      var mainContent = $("#main-content");

      ko.applyBindings(this,mainContent.find("#proxyConnectorsTable").get(0));
      removeSmallSpinnerImg("#main-content");
      mainContent.find("#proxy-connectors-view-tabs #proxy-connectors-view-tabs-a-network-proxies-grid").tab('show');

      mainContent.find("#proxy-connectors-view-tabs").on('show', function (e) {

        if ($(e.target).attr("href")=="#proxy-connectors-edit") {
          var proxyConnector=new ProxyConnector();
          var proxyConnectorViewModel=new ProxyConnectorViewModel(proxyConnector,false,self);
          mainContent.find("#proxy-connectors-edit").html($("#proxy-connector-edit-form-tmpl").tmpl());
          ko.applyBindings(proxyConnectorViewModel,mainContent.find("#proxy-connectors-edit").get(0));
        }
        if ($(e.target).attr("href")=="#proxy-connectors-view") {
          $("#proxy-connectors-view-tabs-a-network-proxies-grid").html($.i18n.prop("proxy-connectors.grid.tab.title"));

        }

      });
    }

  }

  displayProxyConnectors=function(){
    var mainContent = $("#main-content");
    mainContent.html($("#proxyConnectorsMain").tmpl());
    mainContent.append(smallSpinnerImg());

    this.proxyConnectorsViewModel = new ProxyConnectorsViewModel();
    var self=this;

    loadManagedRepositories(function(data) {
      self.proxyConnectorsViewModel.managedRepositories(mapManagedRepositories(data));

      loadRemoteRepositories(function(data) {
        self.proxyConnectorsViewModel.remoteRepositories(mapRemoteRepositories(data));

        loadNetworkProxies(function(data) {
          self.proxyConnectorsViewModel.networkProxies(mapNetworkProxies(data));

          loadAllPolicies( function(data) {
            self.proxyConnectorsViewModel.policyInformations(mapPolicyInformations(data));

            loadAllProxyConnectors( function(data) {
              self.proxyConnectorsViewModel.proxyConnectors(mapProxyConnectors(data));
              self.proxyConnectorsViewModel.displayGrid();
            });

          });

        });

      });

    });

  }

  loadAllPolicies=function(successCallBackFn,errorCallBackFn){
    $.ajax("restServices/archivaServices/proxyConnectorService/allPolicies", {
        type: "GET",
        dataType: 'json',
        success: successCallBackFn,
        error: errorCallBackFn
      }
    );
  }

  loadAllProxyConnectors=function(successCallBackFn,errorCallBackFn){
    $.ajax("restServices/archivaServices/proxyConnectorService/getProxyConnectors", {
      type: "GET",
      dataType: 'json',
      success: successCallBackFn,
      error: errorCallBackFn
     });
  }

  mapProxyConnector=function(data){
    if (data==null){
      return null;
    }
    var policies = data.policies == null ? []: $.each(data.policies,function(item){
      return new Entry(item.key, item.value);
    });
    if (!$.isArray(policies)){
      policies=[];
    }
    var properties = data.properties == null ? []: $.each(data.properties,function(item){
          return new Entry(item.key, item.value);
        });
    if (!$.isArray(properties)){
      properties=[];
    }
    return new ProxyConnector(data.sourceRepoId,data.targetRepoId,data.proxyId,mapStringArray(data.blackListPatterns),
                              mapStringArray(data.whiteListPatterns),policies,properties,
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
