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
    this.username=ko.observable(username);

    //private String password;
    this.password=ko.observable(password);
  }

  NetworkProxyViewModel=function(networkProxy, update, networkProxiesViewModel){
    this.update=update;
    this.networkProxy=networkProxy;
    this.networkProxiesViewModel=networkProxiesViewModel;
    var self=this;

    save=function(){

    }

    displayGrid=function(){

    }
  }

  NetworkProxiesViewModel=function(){
    this.networkProxies=ko.observableArray([]);

    var self=this;

    editNetworkProxy=function(networkProxy){

    }

    removeNetworkProxy=function(networkProxy){

    }
  }


  displayNetworkProxies=function(){
    var networkProxiesViewModel = new NetworkProxiesViewModel();
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