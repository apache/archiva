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
define("archiva/admin/features/networkproxies/main",["jquery","i18n","jquery.tmpl","bootstrap","jquery.validate","knockout"
  ,"knockout.simpleGrid"], function(jquery,i18n,jqueryTmpl,bootstrap,jqueryValidate,ko) {

  showMenu = function(administrationMenuItems) {
      administrationMenuItems.push(
            {  text : $.i18n.prop('menu.network-proxies')          ,order:1000, id: "menu-network-proxies-list-a"        , href: "#networkproxies"       , redback: "{permissions: ['archiva-manage-configuration']}", func: function(){displayNetworkProxies()}}
    );
  }
   
  NetworkProxy=function(id,protocol,host,port,username,password,useNtlm){
    var self=this;
    //private String id;
    this.id = ko.observable(id);
    this.id.subscribe(function(newValue){self.modified(true)});

    //private String protocol = "http";
    this.protocol=ko.observable(protocol);
    this.protocol.subscribe(function(newValue){self.modified(true)});

    //private String host;
    this.host=ko.observable(host);
    this.host.subscribe(function(newValue){self.modified(true)});

    //private int port = 8080;
    this.port=ko.observable(port);
    this.port.subscribe(function(newValue){self.modified(true)});

    //private String username;
    this.username=ko.observable(username?username:"");
    this.username.subscribe(function(newValue){self.modified(true)});

    //private String password;
    this.password=ko.observable(password?password:"");
    this.password.subscribe(function(newValue){self.modified(true)});

    //use NTLM proxy
    this.useNtlm=ko.observable(useNtlm?useNtlm:false);
    this.useNtlm.subscribe(function(newValue){self.modified(true)});

    this.modified=ko.observable(false);
  }

  NetworkProxyViewModel=function(networkProxy, update, networkProxiesViewModel,bulkMode){
    this.update=update;
    this.networkProxy=networkProxy;
    this.networkProxiesViewModel=networkProxiesViewModel;
    var self=this;
    this.bulkMode=false || bulkMode;

    this.save=function(){
      if (!$("#main-content" ).find("#network-proxy-edit-form").valid()){
        return;
      }
      if (!this.bulkMode){
        clearUserMessages();
      }
      if (update){
        $.ajax("restServices/archivaServices/networkProxyService/updateNetworkProxy",
          {
            type: "POST",
            contentType: 'application/json',
            data: ko.toJSON(networkProxy),
            dataType: 'json',
            success: function(data) {
              $.log("update proxy id:"+self.networkProxy.id());
              var message=$.i18n.prop('networkproxy.updated',self.networkProxy.id());
              displaySuccessMessage(message);
              self.networkProxy.modified(false);
              if (!this.bulkMode){
                activateNetworkProxiesGridTab();
              }
            },
            error: function(data) {
              var res = $.parseJSON(data.responseText);
              displayRestError(res);
            }
          }
        );
      } else {

        $.ajax("restServices/archivaServices/networkProxyService/addNetworkProxy",
          {
            type: "POST",
            contentType: 'application/json',
            data: ko.toJSON(networkProxy),
            dataType: 'json',
            success: function(data) {
              self.networkProxy.modified(false);
              self.networkProxiesViewModel.networkProxies.push(self.networkProxy);
              displaySuccessMessage($.i18n.prop('networkproxy.added',self.networkProxy.id()));
              activateNetworkProxiesGridTab();
            },
            error: function(data) {
              var res = $.parseJSON(data.responseText);
              displayRestError(res);
            }
          }
        );

      }
    }

    displayGrid=function(){
      activateNetworkProxiesGridTab();
    }
  }

  NetworkProxiesViewModel=function(){
    this.networkProxies=ko.observableArray([]);

    var self=this;

    this.gridViewModel = null;

    editNetworkProxy=function(networkProxy){
      clearUserMessages();
      $.log("editNetworkProxy");
      var mainContent = $("#main-content");
      mainContent.find("#network-proxies-view-tabs-li-edit a").html($.i18n.prop("edit"));
      var viewModel = new NetworkProxyViewModel(networkProxy,true,self);
      ko.applyBindings(viewModel,mainContent.find("#network-proxies-edit").get(0));
      activateNetworkProxyFormValidation();
      activateNetworkProxyEditTab();
      mainContent.find("#network-proxy-btn-save").attr("disabled","true");
      mainContent.find("#network-proxy-btn-save").button('toggle');
    }

    this.bulkSave=function(){
      return getModifiedNetworkProxies().length>0;
    }

    getModifiedNetworkProxies=function(){
      var prx = $.grep(self.networkProxies(),
          function (networkProxy,i) {
            return networkProxy.modified();
          });
      return prx;
    }


    updateModifiedNetworkProxies=function(){
      var modifiedNetworkProxies = getModifiedNetworkProxies();

      openDialogConfirm(function(){
                          for(var i=0;i<modifiedNetworkProxies.length;i++){
                            var viewModel = new NetworkProxyViewModel(modifiedNetworkProxies[i],true,self,false);
                            viewModel.save();
                          }
                          closeDialogConfirm();
                        },
                        $.i18n.prop('ok'),
                        $.i18n.prop('cancel'),
                        $.i18n.prop('networkproxy.bulk.save.confirm.title'),
                        $.i18n.prop('networkproxy.bulk.save.confirm',modifiedNetworkProxies.length));


    }

    updateNetworkProxy=function(networkProxy){
      var viewModel = new NetworkProxyViewModel(networkProxy,true,self,false);
      viewModel.save();
    }

    removeNetworkProxy=function(networkProxy){
      openDialogConfirm(
          function(){
            $.ajax("restServices/archivaServices/networkProxyService/deleteNetworkProxy/"+encodeURIComponent(networkProxy.id()),
              {
                type: "get",
                success: function(data) {
                  self.networkProxies.remove(networkProxy);
                  clearUserMessages();
                  displaySuccessMessage($.i18n.prop('networkproxy.deleted',networkProxy.id()));
                  activateNetworkProxiesGridTab();
                },
                error: function(data) {
                  var res = $.parseJSON(data.responseText);
                  displayRestError(res);
                },
                complete: function(){
                  closeDialogConfirm();
                }
              }
            )}, $.i18n.prop('ok'), $.i18n.prop('cancel'), $.i18n.prop('networkproxy.delete.confirm',networkProxy.id()),
            $("#network-proxy-delete-warning-tmpl" ).tmpl(networkProxy));
    }
  }


  displayNetworkProxies=function(){
    screenChange();
    var mainContent = $("#main-content");
    mainContent.html(mediumSpinnerImg());



    loadNetworkProxies( function(data) {
        var networkProxiesViewModel = new NetworkProxiesViewModel();
        mainContent.html($("#networkProxiesMain").tmpl());
        mainContent.find("#network-proxies-view-tabs a:first").tab('show');

        mainContent.find("#network-proxies-view-tabs").on('show', function (e) {
          if ($(e.target).attr("href")=="#network-proxies-edit") {
            var viewModel = new NetworkProxyViewModel(new NetworkProxy(),false,networkProxiesViewModel);
            ko.applyBindings(viewModel,$("#main-content" ).find("#network-proxies-edit").get(0));
            activateNetworkProxyFormValidation();
            clearUserMessages();
          }
          if ($(e.target).attr("href")=="#network-proxies-view") {
            $("#main-content" ).find("#network-proxies-view-tabs-li-edit a").html($.i18n.prop("add"));
            clearUserMessages();
          }

        });
        networkProxiesViewModel.networkProxies(mapNetworkProxies(data));
        networkProxiesViewModel.gridViewModel = new ko.simpleGrid.viewModel({
          data: networkProxiesViewModel.networkProxies,
          columns: [
            {
              headerText: $.i18n.prop('identifier'),
              rowText: "id"
            },
            {
              headerText: $.i18n.prop('protocol'),
              rowText: "protocol"
            },
            {
            headerText: $.i18n.prop('host'),
            rowText: "host"
            },
            {
            headerText: $.i18n.prop('port'),
            rowText: "port"
            },
            {
            headerText: $.i18n.prop('username'),
            rowText: "username"
            }
          ],
          pageSize: 5,
          gridUpdateCallBack: function(networkProxy){
            $("#main-content" ).find("#networkProxiesTable [title]").tooltip();
          }
        });
        ko.applyBindings(networkProxiesViewModel,$("#main-content" ).find("#network-proxies-view").get(0));
      }
    )
  }

  loadNetworkProxies=function(successCallbackFn, errorCallbackFn){
    $.ajax("restServices/archivaServices/networkProxyService/getNetworkProxies", {
        type: "GET",
        dataType: 'json',
        success: successCallbackFn,
        error: errorCallbackFn
    });
  }

  activateNetworkProxyFormValidation=function(){
    var editForm=$("#main-content" ).find("#network-proxy-edit-form");
    var validator = editForm.validate({
      rules: {id: {
       required: true,
       remote: {
         url: "restServices/archivaUiServices/dataValidatorService/networkProxyIdNotExists",
         type: "get"
       }
      }},
      showErrors: function(validator, errorMap, errorList) {
       customShowError(editForm,validator,errorMap,errorMap);
      }
    });
    validator.settings.messages["id"]=$.i18n.prop("id.required.or.alreadyexists");
  }

  activateNetworkProxiesGridTab=function(){
    var mainContent = $("#main-content");
    mainContent.find("#network-proxies-view-tabs-li-edit").removeClass("active");
    mainContent.find("#network-proxies-edit").removeClass("active");

    mainContent.find("#network-proxies-view-tabs-li-grid").addClass("active");
    mainContent.find("#network-proxies-view").addClass("active");
    mainContent.find("#network-proxies-view-tabs-li-edit a").html($.i18n.prop("add"));

  }

  activateNetworkProxyEditTab=function(){
    var mainContent = $("#main-content");
    mainContent.find("#network-proxies-view-tabs-li-grid").removeClass("active");
    mainContent.find("#network-proxies-view").removeClass("active");

    mainContent.find("#network-proxies-view-tabs-li-edit").addClass("active");
    mainContent.find("#network-proxies-edit").addClass("active");
  }

  mapNetworkProxy=function(data){
    if (data==null){
      return null;
    }
    return new NetworkProxy(data.id,data.protocol,data.host,data.port,data.username,data.password,data.useNtlm);
  }

  mapNetworkProxies=function(data){
    var mappedNetworkProxies = $.map(data, function(item) {
      return mapNetworkProxy(item);
    });
    return mappedNetworkProxies;
  }

});