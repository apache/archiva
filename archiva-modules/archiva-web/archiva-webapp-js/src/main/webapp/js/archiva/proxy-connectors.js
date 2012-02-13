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

  ProxyConnector=function(sourceRepoId,targetRepoId,proxyId,blackListPatterns,whiteListPatterns,policiesEntries,propertiesEntries,
                          disabled,order){
    var self=this;

    //private String sourceRepoId;
    this.sourceRepoId=ko.observable(sourceRepoId);
    this.sourceRepoId.subscribe(function(newValue){
      self.modified(true);
    });

    //private String targetRepoId;
    this.targetRepoId=ko.observable(targetRepoId);
    this.targetRepoId.subscribe(function(newValue){
      self.modified(true);
    });

    //private String proxyId;
    this.proxyId=ko.observable(proxyId);
    this.proxyId.subscribe(function(newValue){
      self.modified(true);
    });

    //private List<String> blackListPatterns;
    this.blackListPatterns=ko.observableArray(blackListPatterns==null?[]:blackListPatterns);
    this.blackListPatterns.subscribe(function(newValue){
      self.modified(true);
    });

    //private List<String> whiteListPatterns;
    this.whiteListPatterns=ko.observableArray(whiteListPatterns==null?[]:whiteListPatterns);
    this.whiteListPatterns.subscribe(function(newValue){
      self.modified(true);
    });

    //private List<PropertyEntry> policiesEntries;
    this.policiesEntries=ko.observableArray(policiesEntries==null?new Array():policiesEntries);
    this.policiesEntries.subscribe(function(newValue){
      self.modified(true);
    });

    //private List<PropertyEntry> properties;
    this.propertiesEntries=ko.observableArray(propertiesEntries==null?new Array():propertiesEntries);
    this.propertiesEntries.subscribe(function(newValue){
      self.modified(true);
    });

    //private boolean disabled = false;
    this.disabled=ko.observable(disabled);
    this.disabled.subscribe(function(newValue){
      self.modified(true);
    });

    //private int order = 0;
    this.order=ko.observable(order?order:0);
    this.order.subscribe(function(newValue){
      self.modified(true);
    });

    this.modified=ko.observable(false);
    //this.modified.subscribe(function(newValue){$.log("ProxyConnector modified:"+newValue)});

    this.updatePolicyEntry=function(key,value){
      for(i=0;i<policiesEntries.length;i++){
        if (policiesEntries[i].key==key){
          policiesEntries[i].value=value;
          self.modified(true);
        }
      }
    }

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

  ProxyConnectorViewModel=function(proxyConnector,update,proxyConnectorsViewModel){
    var self=this;
    this.proxyConnector=proxyConnector;
    this.proxyConnectorsViewModel=proxyConnectorsViewModel;
    this.update=update;
    this.modified=ko.observable(false);

    isUpdate=function(){
      return self.update;
    }

    getSelectedPolicyOption=function(id){
      var policiesEntries=self.proxyConnector.policiesEntries();
      if (policiesEntries!=null){
        for (i=0;i<policiesEntries.length;i++){
          var curKey = $.isFunction(policiesEntries[i].key)? policiesEntries[i].key():policiesEntries[i].key;
          if (id==curKey){
            return $.isFunction(policiesEntries[i].value)? policiesEntries[i].value():policiesEntries[i].value;
          }
        }
      }
      return "";
    }

    changePolicyOption=function(id){
      var value = $("#main-content #policy-"+id + " option:selected").val();
      self.proxyConnector.updatePolicyEntry(id,value);
    }


    getPolicyOptions=function(id){
      var policyInformations=self.proxyConnectorsViewModel.policyInformations();
      for(i=0;i<policyInformations.length;i++){
        if (policyInformations[i].id()==id){
          return policyInformations[i].options();
        }
      }
    }



    addBlacklistPattern=function(){
      var pattern = $("#main-content #blacklist-value").val();
      var tab =  self.proxyConnector.blackListPatterns();
      tab.push(pattern);
      self.proxyConnector.blackListPatterns(tab);
      self.proxyConnector.modified(true);
    }

    removeBlacklistPattern=function(pattern){
      self.proxyConnector.blackListPatterns.remove(pattern);
      self.proxyConnector.modified(true);
    }

    addWhitelistPattern=function(){
      var pattern = $("#main-content #whitelist-value").val();
      var tab =  self.proxyConnector.whiteListPatterns();
      tab.push(pattern);
      self.proxyConnector.whiteListPatterns(tab);
      self.proxyConnector.modified(true);

    }

    removeWhitelistPattern=function(pattern){
      self.proxyConnector.whiteListPatterns.remove(pattern);
      self.proxyConnector.modified(true);
    }

    this.save=function(){
      //FIXME data controls !!!
      clearUserMessages();
      // update is delete then add
      if (this.update){
        $.ajax("restServices/archivaServices/proxyConnectorService/updateProxyConnector",
          {
            type: "POST",
            data: "{\"proxyConnector\": " + ko.toJSON(self.proxyConnector)+"}",
            contentType: 'application/json',
            dataType: 'json',
            success: function(data) {
              displaySuccessMessage($.i18n.prop('proxyconnector.updated'));
              activateProxyConnectorsGridTab();
              self.proxyConnector.modified(false);
            },
            error: function(data) {
              var res = $.parseJSON(data.responseText);
              displayRestError(res);
            }
          }
        );
      } else {

        $.ajax("restServices/archivaServices/proxyConnectorService/addProxyConnector",
          {
            type: "POST",
            data: "{\"proxyConnector\": " + ko.toJSON(self.proxyConnector)+"}",
            contentType: 'application/json',
            dataType: 'json',
            success: function(data) {
              displaySuccessMessage($.i18n.prop('proxyconnector.added'));
              activateProxyConnectorsGridTab();
              self.proxyConnector.modified(false);
              self.proxyConnectorsViewModel.proxyConnectors.push(self.proxyConnector);
            },
            error: function(data) {
              var res = $.parseJSON(data.responseText);
              displayRestError(res);
            }
          }
        );
      }
    }

    this.deleteProperty=function(key){
      for(i=0;i<self.proxyConnector.propertiesEntries().length;i++){
        var entry=self.proxyConnector.propertiesEntries()[i];
        if (entry.key()==key()){
          self.proxyConnector.propertiesEntries.remove(entry);
          self.proxyConnector.modified(true);
        }
      }

    }

    this.addProperty=function(){
      var mainContent=$("#main-content");
      var key=mainContent.find("#property-key").val();
      var value=mainContent.find("#property-value").val();
      var oldTab = self.proxyConnector.propertiesEntries();
      oldTab.push(new Entry(key,value));
      self.proxyConnector.propertiesEntries(oldTab);
      mainContent.find("#property-key").val("");
      mainContent.find("#property-value").val("");
      self.proxyConnector.modified(true);
    }

    displayGrid=function(){
      activateProxyConnectorsGridTab();
    }
  }

  ProxyConnectorsViewModel=function(){
    var self=this;
    this.proxyConnectors=ko.observableArray([]);
    this.proxyConnectors.subscribe(function(newValue){
      $.log("ProxyConnectorsViewModel#proxyConnectors modified")
      self.proxyConnectors().sort(function(a,b){
        if ( a.sourceRepoId()== b.sourceRepoId()) return a.order() - b.order();
        return (a.sourceRepoId() > b.sourceRepoId())? -1:1;
      });
    });
    this.policyInformations=ko.observableArray([]);
    this.managedRepositories=ko.observableArray([]);
    this.remoteRepositories=ko.observableArray([]);
    this.networkProxies=ko.observableArray([]);


    this.bulkSave=function(){
      return getModifiedProxyConnectors().length>0;
    }

    getModifiedProxyConnectors=function(){
      var prx = $.grep(self.proxyConnectors(),
          function (proxyConnector,i) {
            return proxyConnector.modified();
          });
      return prx;
    }

    this.updateModifiedProxyConnectors=function(){
      var modifiedProxyConnectors = getModifiedProxyConnectors();

      openDialogConfirm(function(){
                          for(i=0;i<modifiedProxyConnectors.length;i++){
                            var viewModel = new ProxyConnectorViewModel(modifiedProxyConnectors[i],true,self,false);
                            viewModel.save();
                          }
                          closeDialogConfirm();
                        },
                        $.i18n.prop('ok'),
                        $.i18n.prop('cancel'),
                        $.i18n.prop('proxy-connectors.bulk.save.confirm.title'),
                        $.i18n.prop('proxy.connector.bulk.save.confirm',modifiedProxyConnectors.length));
    }

    updateProxyConnector=function(proxyConnector){
      var viewModel = new ProxyConnectorViewModel(proxyConnector,true,self,false);
      viewModel.save();
    }

    editProxyConnector=function(proxyConnector){
      var proxyConnectorViewModel=new ProxyConnectorViewModel(proxyConnector,true,self);
      var mainContent = $("#main-content");
      mainContent.find("#proxy-connectors-edit").html($("#proxy-connector-edit-form-tmpl").tmpl());
      ko.applyBindings(proxyConnectorViewModel,mainContent.find("#proxy-connectors-edit").get(0));
      activateProxyConnectorsEditTab();
      mainContent.find("#proxy-connectors-view-tabs-li-edit a").html($.i18n.prop("edit"));
    }

    deleteProxyConnector=function(proxyConnector){

      openDialogConfirm(
          function(){
            clearUserMessages();
            removeProxyConnector(proxyConnector,function(){
            displaySuccessMessage($.i18n.prop('proxyconnector.removed'));
            self.proxyConnectors.remove(proxyConnector);
            closeDialogConfirm();
          })}, $.i18n.prop('ok'), $.i18n.prop('cancel'), $.i18n.prop('proxyconnector.delete.confirm'),null);


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

    getProxyConnector=function(sourceRepoId,targetRepoId){
      var proxyConnector=$.grep(self.proxyConnectors(),
                                      function(proxyConnector,idx){
                                        return proxyConnector.sourceRepoId()==sourceRepoId
                                            && proxyConnector.targetRepoId==targetRepoId;
                                      }
                                  );
      var res = ($.isArray(proxyConnector) && proxyConnector.length>0) ? proxyConnector[0]:new ProxyConnector();
      return res;
    }

    showSettings=function(proxyConnector,targetContentStartId, targetImgStartId){
      //proxyConnector=getProxyConnector(proxyConnector.sourceRepoId(),proxyConnector.targetRepoId());
      var targetContent = $( (targetContentStartId?targetContentStartId:"#proxy-connectors-grid-remoterepo-settings-content-")
                                +proxyConnector.sourceRepoId()+"-"+proxyConnector.targetRepoId());
      targetContent.html("");
      targetContent.append($("#proxy-connectors-remote-settings-popover-tmpl")
                               .tmpl({
                                    proxyConnectorsViewModel: self,
                                    proxyConnector:ko.toJS(proxyConnector)
                                    }));

      var targetImg = $((targetImgStartId?targetImgStartId:"#proxy-connectors-grid-remoterepo-settings-edit-")
                            +proxyConnector.sourceRepoId()+"-"+proxyConnector.targetRepoId());
      targetImg.attr("data-content",targetContent.html());
      targetImg.popover(
          {
            placement: "left",
            html: true
          }
      );

      targetImg.popover('show');

    }

    this.findPolicyInformationName=function(id){
      for(i=0;i<self.policyInformations().length;i++){
        if (id==self.policyInformations()[i].id()){
          return self.policyInformations()[i].name();
        }
      }
      return null;
    }

    orderChangeAware=function(proxyConnector){
      return findProxyConnectorsWithSourceId(proxyConnector).length>1;
    }

    findProxyConnectorsWithSourceId=function(proxyConnector){
      return $.grep(self.proxyConnectors(),function(curProxyConnector,idx){
                                                  return curProxyConnector.sourceRepoId()==proxyConnector.sourceRepoId();
                                                }
                                            );
    }

    displayOrderEdit=function(proxyConnector){
      var proxyConnectors=findProxyConnectorsWithSourceId(proxyConnector);
      $.log("displayOrderEdit:"+proxyConnector.sourceRepoId()+",number:"+proxyConnectors.length);

      var managedRepository = getManagedRepository(proxyConnector.sourceRepoId());
      var proxyConnectorEditOrderViewModel=new ProxyConnectorEditOrderViewModel(proxyConnectors,self,managedRepository);
      ko.applyBindings(proxyConnectorEditOrderViewModel,$("#main-content #proxy-connector-edit-order").get(0));
      activateProxyConnectorsEditOrderTab();
    }

    this.displayGrid=function(){
      this.gridViewModel = new ko.simpleGrid.viewModel({
        data: self.proxyConnectors,
        pageSize: 5,
        gridUpdateCallBack: function(){
          $("#main-content #proxyConnectorsTable [title]").tooltip();
        }
      });
      var mainContent = $("#main-content");

      ko.applyBindings(this,mainContent.find("#proxy-connectors-view").get(0));
      removeSmallSpinnerImg("#main-content");
      mainContent.find("#proxy-connectors-view-tabs #proxy-connectors-view-tabs-a-network-proxies-grid").tab('show');

      mainContent.find("#proxy-connectors-view-tabs").on('show', function (e) {

        if ($(e.target).attr("href")=="#proxy-connectors-edit") {
          var proxyConnector=new ProxyConnector();
          var defaultPolicies=new Array();
          // populate with defaut policies options
          for (i=0;i<self.policyInformations().length;i++){
            defaultPolicies.push(new Entry(self.policyInformations()[i].id(),self.policyInformations()[i].defaultOption));
          }
          proxyConnector.policiesEntries(defaultPolicies);
          var proxyConnectorViewModel=new ProxyConnectorViewModel(proxyConnector,false,self);
          mainContent.find("#proxy-connectors-edit").html($("#proxy-connector-edit-form-tmpl").tmpl());
          ko.applyBindings(proxyConnectorViewModel,mainContent.find("#proxy-connectors-edit").get(0));
        }
        if ($(e.target).attr("href")=="#proxy-connectors-view") {
          $("#proxy-connectors-view-tabs-a-network-proxies-grid").html($.i18n.prop("proxy-connectors.grid.tab.title"));
          mainContent.find("#proxy-connectors-view-tabs-li-edit a").html($.i18n.prop("add"));
        }
        if ($(e.target).attr("href")=="#proxy-connectors-edit-order") {
          activateProxyConnectorsEditOrderTab();
        }

      });
    }

  }

  ProxyConnectorEditOrderViewModel=function(proxyConnectors,proxyConnectorsViewModel,managedRepository){
    var self=this;
    this.proxyConnectors=ko.observableArray(proxyConnectors);
    this.proxyConnectorsViewModel=proxyConnectorsViewModel;
    this.managedRepository=managedRepository;
    proxyConnectorMoved=function(arg){
      $.log("proxyConnectorMoved:"+arg.sourceIndex+" to " + arg.targetIndex);
      // if only 1 move just update two whereas update all with the new order
      if (arg.targetIndex-arg.sourceIndex==1){
        self.proxyConnectors()[arg.targetIndex].order(arg.targetIndex+1);
        self.proxyConnectors()[arg.sourceIndex].order(arg.sourceIndex+1);
      } else {
        for (i=0;i<self.proxyConnectors().length;i++){
          self.proxyConnectors()[i].order(i+1);
        }
      }
    }

    this.findRemoteRepository=function(id){
      $.log("findRemoteRepository:"+id());
      for(i=0;i<self.proxyConnectorsViewModel.remoteRepositories().length;i++){
        if (self.proxyConnectorsViewModel.remoteRepositories()[i].id()==id()){
          return self.proxyConnectorsViewModel.remoteRepositories()[i];
        }
      }
      return null;
    }

    this.updateModifiedProxyConnectors=function(){
      self.proxyConnectorsViewModel.updateModifiedProxyConnectors();
    }

  }

  displayProxyConnectors=function(){
    clearUserMessages();
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

  activateProxyConnectorsGridTab=function(){
    var mainContent = $("#main-content");
    mainContent.find("#proxy-connectors-view-tabs-content div[class*='tab-pane']").removeClass("active");
    mainContent.find("#proxy-connectors-view-tabs li").removeClass("active");

    mainContent.find("#proxy-connectors-view").addClass("active");
    mainContent.find("#proxy-connectors-view-tabs-li-grid").addClass("active");
    mainContent.find("#proxy-connectors-view-tabs-li-edit a").html($.i18n.prop("add"));

  }

  activateProxyConnectorsEditTab=function(){
    var mainContent = $("#main-content");

    mainContent.find("#proxy-connectors-view-tabs-content div[class*='tab-pane']").removeClass("active");
    mainContent.find("#proxy-connectors-view-tabs li").removeClass("active");

    mainContent.find("#proxy-connectors-edit").addClass("active");
    mainContent.find("#proxy-connectors-view-tabs-li-edit").addClass("active");
  }

  activateProxyConnectorsEditOrderTab=function(){
    var mainContent = $("#main-content");

    mainContent.find("#proxy-connectors-view-tabs-content div[class*='tab-pane']").removeClass("active");
    mainContent.find("#proxy-connectors-view-tabs li").removeClass("active");

    mainContent.find("#proxy-connector-edit-order").addClass("active");
    mainContent.find("#proxy-connectors-view-tabs-li-edit-order").addClass("active");
  }

  mapProxyConnector=function(data){
    if (data==null){
      return null;
    }
    var policiesEntries = data.policiesEntries == null ? []: $.each(data.policiesEntries,function(item){
      return new Entry(item.key, item.value);
    });
    if (!$.isArray(policiesEntries)){
      policiesEntries=[];
    }
    var propertiesEntries = data.propertiesEntries == null ? []: $.each(data.propertiesEntries,function(item){
          return new Entry(item.key, item.value);
        });
    if (!$.isArray(propertiesEntries)){
      propertiesEntries=[];
    }
    return new ProxyConnector(data.sourceRepoId,data.targetRepoId,data.proxyId,mapStringArray(data.blackListPatterns),
                              mapStringArray(data.whiteListPatterns),policiesEntries,propertiesEntries,
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

  removeProxyConnector=function(proxyConnector,fnSuccessCallback){
    clearUserMessages();
    var url="restServices/archivaServices/proxyConnectorService/removeProxyConnector?";
    url += "sourceRepoId="+encodeURIComponent(proxyConnector.sourceRepoId());
    url += "&targetRepoId="+encodeURIComponent(proxyConnector.targetRepoId());
    $.ajax(url,
      {
        type: "GET",
        contentType: 'application/json',
        success: function(data) {
          fnSuccessCallback();
        },
        error: function(data) {
          var res = $.parseJSON(data.responseText);
          displayRestError(res);
        }
      }
    );

  }

});
