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
define("archiva/admin/repository/maven2/proxy-connectors-rules",["jquery","jquery.ui","i18n","jquery.tmpl","bootstrap","knockout"
  ,"knockout.simpleGrid","knockout.sortable","archiva/admin/repository/maven2/proxy-connectors"],
  function(jquery,jqueryUi,i18n,jqueryTmpl,bootstrap,ko) {

  ProxyConnectorRulesViewModel=function(proxyConnectorRules,proxyConnectors){
    var self=this;
    self.proxyConnectorRules=ko.observableArray(proxyConnectorRules?proxyConnectorRules:[]);
    self.proxyConnectors=ko.observableArray(proxyConnectors);
    self.proxyConnectors.id="select";

    // FIXME get that from a REST service
    // FIXME i18n
    this.ruleTypes=[new RuleType("BLACK_LIST","Black list","images/red-22-22.png"),new RuleType("WHITE_LIST","White list","images/green-22-22.png")];

    this.findRuleType=function(proxyConnectorRule){
      var ruleType;
      $.each(self.ruleTypes, function(index, value) {
        if(value.type==proxyConnectorRule.proxyConnectorRuleType()){
          ruleType=value;
        }
      });
      return ruleType;
    }

    this.findProxyConnector=function(sourceRepoId,targetRepoId){
      for(var i=0;i<self.proxyConnectors().length;i++){
        var proxyConnector=self.proxyConnectors()[i];
        if(proxyConnector.sourceRepoId()==sourceRepoId && proxyConnector.targetRepoId()==targetRepoId){
          return proxyConnector;
        }
      }
    }

    this.displayGrid=function(){
      var mainContent = $("#main-content");

      $.each(self.proxyConnectorRules(), function(index, value) {
        value.ruleType=self.findRuleType(value);
      });

      this.gridViewModel = new ko.simpleGrid.viewModel({
        data: self.proxyConnectorRules,
        pageSize: 5,
        gridUpdateCallBack: function(){
          //$("#main-content" ).find("#proxy-connectors-rules-view-tabsTable" ).find("[title]").tooltip();
        }
      });

      ko.applyBindings(self,mainContent.find("#proxy-connector-rules-view").get(0));

      removeSmallSpinnerImg(mainContent);

      mainContent.find("#proxy-connectors-rules-view-tabs").on('show', function (e) {
        $.log("on show:"+$(e.target).attr("href"));
        if ($(e.target).attr("href")=="#proxy-connector-rules-edit") {
          var proxyConnectorRuleViewModel = new ProxyConnectorRuleViewModel(new ProxyConnectorRule(),self,false);
          ko.applyBindings(proxyConnectorRuleViewModel,mainContent.find("#proxy-connector-rules-edit" ).get(0));
          activateProxyConnectorRulesEditTab();
        }
      });
    }
    addProxyConnectorRule=function(proxyConnectorRule){
      $("#proxy-connector-rule-add-btn" ).button("loading");
      $.log("addProxyConnectorRule");
      self.saveProxyConnectorRule(proxyConnectorRule,"restServices/archivaServices/proxyConnectorRuleService/proxyConnectorRule",true,
      function(){
        $("#proxy-connector-rule-add-btn" ).button("reset");
      });
    }

    this.saveProxyConnectorRule=function(proxyConnectorRule,url,add,completeFnCallback){
      $.log("saveProxyConnectorRule:"+url);
      var userMessages=$("#user-messages");
      userMessages.html(mediumSpinnerImg());
      $.ajax(url,
        {
          type: "POST",
          contentType: 'application/json',
          data: ko.toJSON(proxyConnectorRule),
          dataType: 'json',
          success: function(data) {
            $.log("save proxyConnectorRule pattern:"+proxyConnectorRule.pattern());
            var message=$.i18n.prop(add?'proxy-connector-rule.added':'proxy-connector-rule.updated',proxyConnectorRule.pattern());
            displaySuccessMessage(message);
            proxyConnectorRule.modified(false);
            if(add){
              // add rule type for image
              proxyConnectorRule.ruleType=self.findRuleType(proxyConnectorRule);
              self.proxyConnectorRules.push(proxyConnectorRule);
            }
            activateProxyConnectorRulesGridTab();
          },
          error: function(data) {
            var res = $.parseJSON(data.responseText);
            displayRestError(res);
          },
          complete:function(data){
            removeMediumSpinnerImg(userMessages);
            if(completeFnCallback){
              completeFnCallback();
            }
          }
        }
      );
    }

    updateProxyConnectorRule=function(proxyConnectorRule){
      $.log("updateProxyConnectorRule");
      $("#main-content" ).find("#proxy-connectors-rules-edit-div").find("#proxy-connector-rule-update-btn").button("loading");
      self.saveProxyConnectorRule(proxyConnectorRule,"restServices/archivaServices/proxyConnectorRuleService/updateProxyConnectorRule",
                                  false,
                                  function(){
                                    $("#proxy-connector-rule-update-btn" ).button("reset");
                                  }
      );
    }

    this.deleteProxyConnectorRule=function(proxyConnectorRule){
      $("#main-content" ).find("proxy-connectors-rules-view-tabsTable").find(".btn").button("loading");
      var userMessages=$("#user-messages");
      userMessages.html(mediumSpinnerImg());
      $.ajax("restServices/archivaServices/proxyConnectorRuleService/deleteProxyConnectorRule",
       {
         type:"POST",
         contentType: 'application/json',
         data: ko.toJSON(proxyConnectorRule),
         dataType: 'json',
         success:function(data){
           var message=$.i18n.prop('proxy-connector-rule.deleted',proxyConnectorRule.pattern());
           self.proxyConnectorRules.remove(proxyConnectorRule);
           displaySuccessMessage(message);
         },
         error: function(data) {
           var res = $.parseJSON(data.responseText);
           displayRestError(res);
         },
         complete:function(data){
           removeMediumSpinnerImg(userMessages);
           $("#main-content" ).find("proxy-connectors-rules-view-tabsTable").find(".btn").button("reset");
         }
       }
      );
    }

    removeProxyConnectorRule=function(proxyConnectorRule){

      openDialogConfirm(
          function(){self.deleteProxyConnectorRule(proxyConnectorRule);window.modalConfirmDialog.modal('hide')},
          $.i18n.prop('ok'), $.i18n.prop('cancel'),
          $.i18n.prop('proxy-connector-rule.delete.confirm',proxyConnectorRule.pattern()),"");

    }

    editProxyConnectorRule=function(proxyConnectorRule){
      var proxyConnectorRuleViewModel=new ProxyConnectorRuleViewModel(proxyConnectorRule,self,true);
      ko.applyBindings(proxyConnectorRuleViewModel,$("#main-content").find("#proxy-connector-rules-edit" ).get(0));
      activateProxyConnectorRulesEditTab();
      proxyConnectorRuleViewModel.activateRemoveChosen(self);
      proxyConnectorRuleViewModel.activateRemoveAvailable(self);
    }

    remove=function(){
      $.log("remove");
    }

  }

  ProxyConnectorRuleViewModel=function(proxyConnectorRule,proxyConnectorRulesViewModel,update){
    var self=this;
    this.proxyConnectorRule=proxyConnectorRule;
    this.proxyConnectorRulesViewModel=proxyConnectorRulesViewModel;
    this.availableProxyConnectors=ko.observableArray([]);
    this.availableProxyConnectors.id="availableProxyConnectors";
    this.update=update;

    $.each(this.proxyConnectorRulesViewModel.proxyConnectors(), function(idx, value) {
      //$.log(idx + ': ' + value.sourceRepoId() +":"+value.targetRepoId());
      var available=true;
      // is it in proxyConnectorRule.proxyConnectors
      $.each(self.proxyConnectorRule.proxyConnectors(),function(index,proxyConnector){
        if(value.sourceRepoId()==proxyConnector.sourceRepoId() && value.targetRepoId()==proxyConnector.targetRepoId()){
          available=false;
        }
      });
      if(available==true){
        self.availableProxyConnectors.push(value);
      }
    });

    proxyConnectorMoved=function(arg){
      $.log("repositoryMoved:"+arg.sourceIndex+" to " + arg.targetIndex);
      self.proxyConnectorRule.modified(true);
      self.activateRemoveChosen(self.proxyConnectorRulesViewModel);
      self.activateRemoveAvailable(self.proxyConnectorRulesViewModel);
    }

    saveProxyConnectorRule=function(){
      self.proxyConnectorRulesViewModel.saveProxyConnectorRule(self.proxyConnectorRule)
    }

    this.removeChosen=function(proxyConnectorRulesViewModel,sourceRepoId,targetRepoId){
      $.log("removeChosen:"+sourceRepoId+":"+targetRepoId);

      $.log("size before:"+self.proxyConnectorRule.proxyConnectors().length);
      var proxyConnectorToRemove=null;
      for(var i=0;i<self.proxyConnectorRule.proxyConnectors().length;i++){
        if(self.proxyConnectorRule.proxyConnectors()[i].sourceRepoId()==sourceRepoId &&
            self.proxyConnectorRule.proxyConnectors()[i].targetRepoId()==targetRepoId){
          proxyConnectorToRemove=self.proxyConnectorRule.proxyConnectors()[i];
        }
      }
      self.proxyConnectorRule.proxyConnectors.remove(proxyConnectorToRemove);
      self.availableProxyConnectors.push(proxyConnectorToRemove);
      $.log("size after:"+self.proxyConnectorRule.proxyConnectors().length);
      var mainContent=$("#main-content");
      mainContent.find("#proxy-connectors-rules-available-proxy-connectors" ).find("[data-source-repoId="+sourceRepoId+"][data-target-repoId="+targetRepoId+"]" ).on("click", function(){
        self.removeAvailable(proxyConnectorRulesViewModel,$(this).attr("data-source-repoId"),$(this).attr("data-target-repoId"));
      });
      mainContent.find("#proxy-connectors-rules-edit-order-div" ).find("[data-source-repoId="+sourceRepoId+"][data-target-repoId="+targetRepoId+"]" ).off("click");
    }

    this.activateRemoveChosen=function(proxyConnectorRulesViewModel){
      $("#main-content" ).find("#proxy-connectors-rules-edit-order-div" ).find(".icon-minus-sign" ).on("click", function(){
        self.removeChosen(proxyConnectorRulesViewModel,$(this).attr("data-source-repoId"),$(this).attr("data-target-repoId"));
      });
    }

    this.removeAvailable=function(proxyConnectorRulesViewModel,sourceRepoId,targetRepoId){
      $.log("removeAvailable:"+sourceRepoId+":"+targetRepoId);

      $.log("size before:"+self.availableProxyConnectors().length);
      var proxyConnectorToAdd=null;
      for(var i=0;i<self.availableProxyConnectors().length;i++){
        if(self.availableProxyConnectors()[i].sourceRepoId()==sourceRepoId &&
            self.availableProxyConnectors()[i].targetRepoId()==targetRepoId){
          $.log("found");
          proxyConnectorToAdd=self.availableProxyConnectors()[i];
        }
      }
      self.proxyConnectorRule.proxyConnectors.push(proxyConnectorToAdd);
      self.availableProxyConnectors.remove(proxyConnectorToAdd);
      $.log("size after:"+self.availableProxyConnectors().length);
      var mainContent=$("#main-content");
      mainContent.find("#proxy-connectors-rules-edit-order-div" ).find("[data-source-repoId="+sourceRepoId+"][data-target-repoId="+targetRepoId+"]" ).on("click", function(){
        self.removeChosen(proxyConnectorRulesViewModel,$(this).attr("data-source-repoId"),$(this).attr("data-target-repoId"));
      });
      mainContent.find("#proxy-connectors-rules-available-proxy-connectors" ).find("[data-source-repoId="+sourceRepoId+"][data-target-repoId="+targetRepoId+"]" ).off("click");
    }

    this.activateRemoveAvailable=function(proxyConnectorRulesViewModel){
      $("#main-content" ).find("#proxy-connectors-rules-available-proxy-connectors" ).find(".icon-plus-sign" ).on("click", function(){
        self.removeAvailable(proxyConnectorRulesViewModel,$(this).attr("data-source-repoId"),$(this).attr("data-target-repoId"));
      });
    }

  }


  displayProxyConnectorsRules=function(){
    $.log("displayProxyConnectorsRules");
    screenChange();
    var mainContent = $("#main-content");
    mainContent.html($("#proxyConnectorsRulesMain").tmpl());
    var userMessages=$("#user-messages");
    userMessages.html(mediumSpinnerImg());
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
          },
          complete: function(data){
            removeMediumSpinnerImg(userMessages);
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

    this.ruleType=null;

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

    this.ruleType=null;
  }

  mapProxyConnectorRule=function(data){
    if (data==null){
      return null;
    }
    return new ProxyConnectorRule(data.pattern, data.proxyConnectorRuleType, mapProxyConnectors(data.proxyConnectors));
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
    mainContent.find("#proxy-connectors-rules-view-tabs li").removeClass("active");

    mainContent.find("#proxy-connector-rules-view").addClass("active");
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

  RuleType=function(type,label,image){
    this.type=type;
    this.label=label;
    this.image=image;
  }

});
