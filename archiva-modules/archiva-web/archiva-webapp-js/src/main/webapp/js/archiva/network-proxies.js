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


  displayNetworkProxies=function(){

  }

  NetworkProxy=function(id,protocol,host,port,username,password){
    //private String id;
    this.id = ko.observable(id);

    //private String protocol = "http";
    this.protocol=ko.observable(protocol);

    //private String host;
    this.host=ko.observable(host);

    //private int port = 8080;
    this.port=ko.observable(port);

    //private String username;
    this.username=ko.observable(username?username:"");

    //private String password;
    this.password=ko.observable(password?password:"");
  }

  NetworkProxyViewModel=function(networkProxy, update, networkProxiesViewModel){
    this.update=update;
    this.networkProxy=networkProxy;
    this.networkProxiesViewModel=networkProxiesViewModel;
    var self=this;

    save=function(){
      if (!$("#main-content #network-proxy-edit-form").valid()){
        return;
      }
      clearUserMessages();
      if (update){
        $.ajax("restServices/archivaServices/networkProxyService/updateNetworkProxy",
          {
            type: "POST",
            contentType: 'application/json',
            data: "{\"networkProxy\": " + ko.toJSON(networkProxy)+"}",
            dataType: 'json',
            success: function(data) {
              displaySuccessMessage($.i18n.prop('networkproxy.updated'));
              activateNetworkProxiesGridTab();
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
            data: "{\"networkProxy\": " + ko.toJSON(networkProxy)+"}",
            dataType: 'json',
            success: function(data) {
              self.networkProxiesViewModel.networkProxies.push(self.networkProxy);
              displaySuccessMessage($.i18n.prop('networkproxy.added'));
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
      $.log("editNetworkProxy");
      $("#main-content #network-proxies-edit a").html($.i18n.prop("edit"));
      var viewModel = new NetworkProxyViewModel(networkProxy,true,self);
      ko.applyBindings(viewModel,$("#main-content #network-proxies-edit").get(0));
      activateNetworkProxyFormValidation();
      activateNetworkProxyEditTab();
    }

    removeNetworkProxy=function(networkProxy){
      openDialogConfirm(
          function(){$.ajax("restServices/archivaServices/networkProxyService/deleteNetworkProxy/"+encodeURIComponent(networkProxy.id()),
              {
                type: "get",
                success: function(data) {
                  self.networkProxies.remove(networkProxy);
                  clearUserMessages();
                  displaySuccessMessage($.i18n.prop('networkproxy.deleted'));
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
            )}, $.i18n.prop('ok'), $.i18n.prop('cancel'), $.i18n.prop('networkproxy.delete.confirm',networkProxy.id()),null);
    }
  }


  displayNetworkProxies=function(){
    clearUserMessages();
    $("#main-content").html(mediumSpinnerImg());
    $("#main-content").html($("#networkProxiesMain").tmpl());
    $("#main-content #network-proxies-view-tabs").tabs();

    var networkProxiesViewModel = new NetworkProxiesViewModel();

    $("#main-content #network-proxies-view-tabs").bind('change', function (e) {
      if ($(e.target).attr("href")=="#network-proxies-edit") {
        var viewModel = new NetworkProxyViewModel(new NetworkProxy(),false,networkProxiesViewModel);
        ko.applyBindings(viewModel,$("#main-content #network-proxies-edit").get(0));
        activateNetworkProxyFormValidation();
      }
      if ($(e.target).attr("href")=="#network-proxies-view") {
        $("#main-content #network-proxies-edit a").html($.i18n.prop("add"));
      }

    });



    $.ajax("restServices/archivaServices/networkProxyService/getNetworkProxies", {
        type: "GET",
        dataType: 'json',
        success: function(data) {
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
            gridUpdateCallBack: function(){
              $("#main-content #networkProxiesTable [title]").twipsy();
            }
          });
          ko.applyBindings(networkProxiesViewModel,$("#main-content #networkProxiesTable").get(0));
        }
      }
    );
  }

  activateNetworkProxyFormValidation=function(){
    $("#main-content #network-proxy-edit-form").validate({
               showErrors: function(validator, errorMap, errorList) {
                 customShowError(validator,errorMap,errorMap);
               }
              });
  }

  activateNetworkProxiesGridTab=function(){
    $("#main-content #network-proxies-view-tabs-li-edit").removeClass("active");
    $("#main-content #network-proxies-edit").removeClass("active");

    $("#main-content #network-proxies-view-tabs-li-grid").addClass("active");
    $("#main-content #network-proxies-view").addClass("active");
    $("#main-content #network-proxies-view-tabs-li-edit a").html($.i18n.prop("add"));
  }

  activateNetworkProxyEditTab=function(){
    $("#main-content #network-proxies-view-tabs-li-grid").removeClass("active");
    $("#main-content #network-proxies-view").removeClass("active");

    $("#main-content #network-proxies-view-tabs-li-edit").addClass("active");
    $("#main-content #network-proxies-edit").addClass("active");

  }

  mapNetworkProxy=function(data){
    if (data==null){
      return null;
    }
    return new NetworkProxy(data.id,data.protocol,data.host,data.port,data.username,data.password);
  }

  mapNetworkProxies=function(data){
    var mappedNetworkProxies = $.map(data.networkProxy, function(item) {
      return mapNetworkProxy(item);
    });
    return mappedNetworkProxies;
  }

});