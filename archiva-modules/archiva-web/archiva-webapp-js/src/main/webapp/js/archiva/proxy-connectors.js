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

  ProxyConnectorsViewModel=function(){
    this.proxyConnectors=ko.observableArray([]);
    var self=this;

    editProxyConnector=function(proxyConnector){

    }

    this.displayGrid=function(){
      var sourcesRepos=[];
      //sourceRepoId
      for(i=0;i<self.proxyConnectors().length;i++){
        var curSrcRepo=self.proxyConnectors()[i].sourceRepoId();
        var curTarget=self.proxyConnectors()[i].targetRepoId();
        $.log("curSrcRepo:"+curSrcRepo+",curTarget:"+curTarget);
        var sourceRepo = $.grep(sourcesRepos,
                                function(srcRepo,idx){
                                  $.log("grep:"+srcRepo.source);
                                  $.log("sourcesRepos.length:"+sourcesRepos.length);
                                  for (j=0;j<sourcesRepos.length;j++){
                                    if (srcRepo.source==curSrcRepo){
                                      return true;
                                    }
                                  }
                                  return false;
                                }
        );
        $.log("isArray:"+$.isArray(sourceRepo)+",length:"+sourceRepo.length);
        if (sourceRepo.length>0){
          $.log("sourceRepo!=null:"+sourceRepo[0]);
          sourceRepo[0].targetRepos.push(curTarget);
        } else {
          $.log("sourceRepo==null:"+curSrcRepo);
          sourcesRepos.push({source:curSrcRepo,targetRepos:[curTarget]});
        }
      }

      $.log("sourcesRepo.length:"+sourcesRepos.length);
      for(i=0;i<sourcesRepos.length;i++){
        $.log("sourcesRepos[i]:"+sourcesRepos[i].source+"="+sourcesRepos[i].targetRepos.join(":"));
      }
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