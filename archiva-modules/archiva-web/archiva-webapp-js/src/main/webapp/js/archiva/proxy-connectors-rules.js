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
define("archiva.proxy-connectors-rules",["jquery","i18n","jquery.tmpl","bootstrap","jquery.validate","knockout"
  ,"knockout.simpleGrid","knockout.sortable","archiva.proxy-connectors"], function() {



  ProxyConnectorRulesViewModel=function(proxyConnectorRules,proxyConnectors){
    var self=this;
    this.proxyConnectorRules=ko.observableArray(proxyConnectorRules?proxyConnectorRules:[]);
    this.proxyConnectors=proxyConnectors;

    this.displayGrid=function(){
      var mainContent = $("#main-content");

      this.gridViewModel = new ko.simpleGrid.viewModel({
        data: self.proxyConnectorRules,
        pageSize: 5,
        gridUpdateCallBack: function(){
          $("#main-content" ).find("#proxy-connectors-rules-view-tabsTable" ).find("[title]").tooltip();
        }
      });

      ko.applyBindings(self,mainContent.find("#proxy-connectors-rules-view-tabs-view").get(0));

      removeSmallSpinnerImg();


      mainContent.find("#proxy-connectors-rules-view-tabs").on('show', function (e) {
        $.log("on show:"+$(e.target).attr("href"));
        if ($(e.target).attr("href")=="#proxy-connector-rules-edit") {
          var proxyConnectorRuleViewModel = new ProxyConnectorRuleViewModel(new ProxyConnectorRule(),self,false);
          ko.applyBindings(proxyConnectorRuleViewModel,mainContent.find("#proxy-connector-rules-edit" ).get(0));
          activateProxyConnectorRulesEditTab();
        }


      });
    }
  }

  ProxyConnectorRuleViewModel=function(proxyConnectorRule,proxyConnectorRulesViewModel,update){
    var self=this;
    this.proxyConnectorRule=proxyConnectorRule;
    this.proxyConnectorRulesViewModel=proxyConnectorRulesViewModel;
    this.availableProxyConnectors=ko.observableArray(proxyConnectorRulesViewModel.proxyConnectors);
    this.update=update;

    proxyConnectorMoved=function(arg){

    }

  }


  displayProxyConnectorsRules=function(){
    $.log("displayProxyConnectorsRules");
    screenChange();
    var mainContent = $("#main-content");
    mainContent.html($("#proxyConnectorsRulesMain").tmpl());
    mainContent.append(smallSpinnerImg());
    loadAllProxyConnectors(function(data){
      var proxyConnectors = mapProxyConnectors(data);

        $.ajax("restServices/archivaServices/proxyConnectorRuleService/proxyConnectorRules", {
          type: "GET",
          dataType: 'json',
          success: function (data){
            var proxyConnectorRules=mapProxyConnectorRules(data);
            var proxyConnectorRulesViewModel = new ProxyConnectorRulesViewModel(proxyConnectorRules,proxyConnectors);
            proxyConnectorRulesViewModel.displayGrid();
            activateProxyConnectorRulesGridTab();
          }

        });

    });
  }

  ProxyConnectorRule=function(pattern,proxyConnectorRuleType,proxyConnectors){
    //private String pattern;
    var self=this;

    this.modified=ko.observable(false);

    //private String sourceRepoId;
    this.pattern=ko.observable(pattern);
    this.pattern.subscribe(function(newValue){
      self.modified(true);
    });


    //private ProxyConnectorRuleType proxyConnectorRuleType;
    this.proxyConnectorRuleType=ko.observable(proxyConnectorRuleType);
    this.proxyConnectorRuleType.subscribe(function(newValue){
      self.modified(true);
    });

    //private List<ProxyConnector> proxyConnectors;
    this.proxyConnectors=ko.observableArray(proxyConnectors?proxyConnectors:[]);
    this.proxyConnectors.subscribe(function(newValue){
      self.modified(true);
    });
  }

  mapProxyConnectorRule=function(data){
    if (data==null){
      return null;
    }
    return new ProxyConnector(data.pattern, data.proxyConnectorRuleType, mapProxyConnectors(data.proxyConnectors));
  }

  mapProxyConnectorRules=function(data){
    var mappedProxyConnectorRules = $.map(data, function(item) {
      return mapProxyConnectorRule(item);
    });
    return mappedProxyConnectorRules;
  }


  activateProxyConnectorRulesGridTab=function(){
    var mainContent = $("#main-content");
    mainContent.find("#proxy-connectors-rules-view-tabs-content div[class*='tab-pane']").removeClass("active");
    mainContent.find("#proxy-connectors-rules-view-tabs > li").removeClass("active");

    mainContent.find("#repository-groups-view").addClass("active");
    mainContent.find("#proxy-connectors-rules-view-tabs-li-grid").addClass("active");
    mainContent.find("#proxy-connectors-rules-view-tabs-a-edit").html($.i18n.prop("add"));

  }

  activateProxyConnectorRulesEditTab=function(){
    var mainContent = $("#main-content");

    mainContent.find("#proxy-connectors-rules-view-tabs-content div[class*='tab-pane']").removeClass("active");
    mainContent.find("#proxy-connectors-rules-view-tabs > li").removeClass("active");

    mainContent.find("#proxy-connector-rules-edit").addClass("active");
    mainContent.find("#proxy-connectors-rules-view-tabs-edit").addClass("active");
  }


});
