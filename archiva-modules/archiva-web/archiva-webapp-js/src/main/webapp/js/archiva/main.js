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
define("archiva.main",["jquery","jquery.ui","sammy","jquery.tmpl",'i18n',"jquery.cookie","bootstrap","archiva.search",
         "jquery.validate","jquery.json","knockout","redback.templates","archiva.templates",
          "redback.roles","redback","archiva.general-admin","archiva.repositories",
          "archiva.network-proxies","archiva.proxy-connectors","archiva.repository-groups","archiva.artifacts-management",
          "archiva.proxy-connectors-rules","archiva.docs"],
function(jquery,ui,sammy,tmpl,i18n,jqueryCookie,bootstrap,archivaSearch,jqueryValidate,jqueryJson,ko) {

  /**
   * reccord a cookie for session with the logged user
   * @param user see user.js
   */
  reccordLoginCookie=function(user) {
    $.cookie('redback_login', ko.toJSON(user));
  };

  getUserFromLoginCookie=function(){
    return $.parseJSON($.cookie('redback_login'));
  };

  deleteLoginCookie=function(){
    $.cookie('redback_login', null);
  };

  logout=function(doScreenChange){
    deleteLoginCookie();
    $("#login-link").show();
    $("#register-link").show();
    $("#logout-link").hide();
    $("#change-password-link").hide();
    // cleanup karmas
    window.redbackModel.operatioNames=[];
    hideElementWithKarma();
    $("#main-content").html("");
    $.ajax({
      url: 'restServices/redbackServices/loginService/logout',
      complete: function(){
        // go to welcome on logout
        window.sammyArchivaApplication.setLocation("#search");
      }

    });
  };



  decorateMenuWithKarma=function(user) {
    var username = user.username;
    $.log("decorateMenuWithKarma");
    // we can receive an observable user so take if it's a function or not
    if ($.isFunction(username)){
      username = user.username();
    }
    var url = 'restServices/redbackServices/userService/getCurrentUserOperations';
    $.ajax({
      url: url,
      success: function(data){
        var mappedOperations = $.map(data, function(item) {
            return mapOperation(item);
        });
        window.redbackModel.operatioNames = $.map(mappedOperations, function(item){
          return item.name();
        });

        $("#topbar-menu-container").find("[redback-permissions]").each(function(element){
          checkElementKarma(this);
        });
        $("#sidebar-content").find("[redback-permissions]").each(function(element){
          checkElementKarma(this);
        });
        checkUrlParams();
      }
    });
  };

  checkElementKarma=function(element){
    var bindingValue = $(element).attr("redback-permissions");
    $(element).hide();
    var neededKarmas = $(eval(bindingValue)).toArray();
    var karmaOk = false;
    $(neededKarmas).each(function(value){
      if ($.inArray(neededKarmas[value],window.redbackModel.operatioNames)>=0) {
        karmaOk = true;
      }
    });
    if (karmaOk == false) {
      $(element).hide();
    } else {
      $(element).show();
    }
  };

  hideElementWithKarma=function(){
    $("#topbar-menu-container [redback-permissions]").each(function(element){
      $(this).hide();
    });

    $("#sidebar-content").find("[redback-permissions]").each(function(element){
      $(this).hide();
    });
    $.log("hideElementWithKarma");
  };

  //------------------------------------//
  // Change UI with appearance settings //
  //------------------------------------//
  updateAppearanceToolBar=function() {
    $.ajax("restServices/archivaServices/archivaAdministrationService/registrationDisabled", {
      type: "GET",
      dataType: 'json',
      success: function(data) {
        //var disableRegistration=data.disableRegistration;
        var topbarMenu=$("#topbar-menu");
        if( data){
          $.log("disableRegistration");
          topbarMenu.find("#register-link" ).hide();
        }
        $.ajax("restServices/archivaServices/archivaAdministrationService/getOrganisationInformation", {
            type: "GET",
            dataType: 'json',
            success: function(data) {
              var organisationLogo=topbarMenu.find("#organisation-logo");
              if(data.url){
                var url = data.url.startsWith("http://") || data.url.startsWith("https://") ? data.url : "http://"+data.url;
                var link="<a href='"+url+"' class='brand'>";
                if (data.logoLocation) {
                    link+="<img src='"+data.logoLocation+"' style='max-height: 30px'/>";
                } else if (data.name) {
                    link+=data.name;
                } else {
                    link+="Archiva";
                }
                link+="</a>";
                organisationLogo.html(link);
              }
              if (!data.url && data.name){
                organisationLogo.html("<a href='/' class='brand'>"+data.name+"</a>");
              }
              if (!data.url && !data.name){
                organisationLogo.html("<a href='/' class='brand'>Archiva</a>");
              }
            },
            error: function() {
              organisationLogo.html("<a href='/' class='brand'>Archiva</a>");
            }
        });
    }});
  };


  MainMenuViewModel=function() {
      
      var self = this;
      this.artifactMenuItems = ko.observableArray([
              {  text : $.i18n.prop('menu.artifacts') , id: null},
              {  text : $.i18n.prop('menu.artifacts.search') , id: "menu-find-search-a", href: "#search" , func: function(){displaySearch(this)}},
              {  text : $.i18n.prop('menu.artifacts.browse') , id: "menu-find-browse-a", href: "#browse" , func: function(){displayBrowse(true)}},
              {  text : $.i18n.prop('menu.artifacts.upload') , id: "menu-find-upload-a", href: "#upload" , redback: "{permissions: ['archiva-upload-repository']}", func: function(){displayUploadArtifact(true)}}
      ]);
      this.administrationMenuItems = ko.observableArray([
              {  text : $.i18n.prop('menu.administration') , id: null},
              {  text : $.i18n.prop('menu.repository.groups')        , id: "menu-repository-groups-list-a"     , href: "#repositorygroup"  , redback: "{permissions: ['archiva-manage-configuration']}", func: function(){displayRepositoryGroups()}},
              {  text : $.i18n.prop('menu.repositories')             , id: "menu-repositories-list-a"           , href: "#repositorylist"   , redback: "{permissions: ['archiva-manage-configuration']}", func: function(){displayRepositoriesGrid()}},
              {  text : $.i18n.prop('menu.proxy-connectors')         , id: "menu-proxy-connectors-list-a"       , href: "#proxyconnectors"  , redback: "{permissions: ['archiva-manage-configuration']}", func: function(){displayProxyConnectors()}},
              {  text : $.i18n.prop('menu.proxy-connectors-rules')   , id: "menu.proxy-connectors-rules-list-a" , href: "#proxyconnectorsrules" , redback: "{permissions: ['archiva-manage-configuration']}", func: function(){displayProxyConnectorsRules()}},
              {  text : $.i18n.prop('menu.network-proxies')          , id: "menu-network-proxies-list-a"        , href: "#networkproxies"   , redback: "{permissions: ['archiva-manage-configuration']}", func: function(){displayNetworkProxies()}},
              {  text : $.i18n.prop('menu.legacy-artifact-support')  , id: "menu-legacy-support-list-a"         , href: "#legacy"           , redback: "{permissions: ['archiva-manage-configuration']}", func: function(){displayLegacyArtifactPathSupport()}},
              {  text : $.i18n.prop('menu.repository-scanning')      , id: "menu-repository-scanning-list-a"    , href: "#scanningList"     , redback: "{permissions: ['archiva-manage-configuration']}", func: function(){displayRepositoryScanning()}},
              {  text : $.i18n.prop('menu.network-configuration')    , id: "menu-network-configuration-list-a"  , href: "#network"          , redback: "{permissions: ['archiva-manage-configuration']}", func: function(){displayNetworkConfiguration()}},
              {  text : $.i18n.prop('menu.system-status')            , id: "menu-system-status-list-a"          , href: "#status"           , redback: "{permissions: ['archiva-manage-configuration']}", func: function(){displaySystemStatus()}},
              {  text : $.i18n.prop('menu.appearance-configuration') , id: "menu-appearance-list-a"             , href: "#appearance"       , redback: "{permissions: ['archiva-manage-configuration']}", func: function(){displayAppearanceConfiguration()}},
              {  text : $.i18n.prop('menu.ui-configuration')         , id: "menu-ui-configuration-list-a"       , href: "#uiconfig"         , redback: "{permissions: ['archiva-manage-configuration']}", func: function(){displayUiConfiguration()}},
              {  text : $.i18n.prop('menu.reports')                  , id: "menu-report-list-a"                 , href: "#reports"         , redback: "{permissions: ['archiva-manage-configuration']}", func: function(){displayReportsPage()}}
      ]);
      
      this.usersMenuItems = ko.observableArray([
              {  text : $.i18n.prop('menu.users') , id: null},
              {  text : $.i18n.prop('menu.users.manage')    , id: "menu-users-list-a", href: "#users" , redback: "{permissions: ['archiva-manage-users']}", func: function(){displayUsersGrid()}},
              {  text : $.i18n.prop('menu.users.roles')     , id: "menu-roles-list-a", href: "#roles" , redback: "{permissions: ['archiva-manage-users']}", func: function(){displayRolesGrid()}}
      ]);

      this.docsMenuItems = ko.observableArray([
              {  text : $.i18n.prop('menu.docs') , id: null},
              {  text : $.i18n.prop('menu.docs.rest')    , id: "menu-docs-rest-list-a", href: "#docs-rest", target: "", func: function(){displayRestDocs()}},
              {  text : $.i18n.prop('menu.docs.users')   , id: "menu-docs-users-list-a", href: "http://archiva.apache.org/docs/"+window.archivaRuntimeInfo.version, target: "_blank", func: function(){displayUsersDocs()}}
      ]);

      this.activeMenuId = ko.observable();
          
      window.sammyArchivaApplication = Sammy(function () {

        this.get('#quicksearch~:artifactId',function(){
          $("#main-content" ).html(mediumSpinnerImg());
          var artifactId= this.params.artifactId;
          // user can be in a non search view so init the search view first
          var searchViewModel = new SearchViewModel();
          var searchRequest = new SearchRequest();
          searchRequest.artifactId(artifactId);
          searchViewModel.searchRequest(searchRequest);
          displaySearch(function(){
            searchViewModel.externalAdvancedSearch();
          },searchViewModel);
        });

        this.get('#basicsearch/:queryterms',function(){
          var queryterms= this.params.queryterms;
          $.log("queryterms:"+queryterms);
          var searchViewModel = new SearchViewModel();
          var searchRequest = new SearchRequest();
          searchRequest.queryTerms(queryterms);
          searchViewModel.searchRequest(searchRequest);
          displaySearch(function(){
            searchViewModel.externalBasicSearch();
          },searchViewModel);

        });
        this.get('#basicsearch~:repositoryIds/:queryterms',function(){
          var queryterms= this.params.queryterms;
          var repositoryIds = this.params.repositoryIds;
          var repos = repositoryIds.split("~");
          $.log("queryterms:"+queryterms+',repositoryIds:'+repositoryIds+",repos:"+repos.length);
          var searchViewModel = new SearchViewModel();
          var searchRequest = new SearchRequest();
          searchRequest.queryTerms(queryterms);
          searchRequest.repositories=repos;
          searchViewModel.searchRequest(searchRequest);
          displaySearch(function(){
            searchViewModel.externalBasicSearch();
          },searchViewModel);
        });

        this.get('#basicsearch~:repositoryIds/:queryterms',function(){
          var queryterms= this.params.queryterms;
          var repositoryIds = this.params.repositoryIds;
          var repos = repositoryIds.split("~");
          $.log("queryterms:"+queryterms+',repositoryIds:'+repositoryIds+",repos:"+repos.length);
          var searchViewModel = new SearchViewModel();
          var searchRequest = new SearchRequest();
          searchRequest.queryTerms(queryterms);
          searchRequest.repositories=repos;
          searchViewModel.searchRequest(searchRequest);
          displaySearch(function(){
            searchViewModel.externalBasicSearch();
          },searchViewModel);
        });

        this.get('#basicsearch/:queryterms',function(){
          var queryterms= this.params.queryterms;
          $.log("queryterms:"+queryterms);
          var searchViewModel = new SearchViewModel();
          var searchRequest = new SearchRequest();
          searchRequest.queryTerms(queryterms);
          searchViewModel.searchRequest(searchRequest);
          displaySearch(function(){
            searchViewModel.externalBasicSearch();
          },searchViewModel);
        });

        var advancedSearchRoute=function(params){
          var repositoryIds = params.repositoryIds;
          var repos = repositoryIds ? repositoryIds.split("~"):[];
          var queryTerms = params.queryterms;
          var terms=queryTerms?queryTerms.split('~'):[];
          $.log("queryTerms:"+queryTerms+",terms.length:"+terms.length);
          var groupId= terms.length>0?terms[0]:"";
          var artifactId= terms.length>1?terms[1]:"";
          var version= terms.length>2?terms[2]:"";
          var classifier= terms.length>3?terms[3]:"";
          var packaging= terms.length>4?terms[4]:"";
          var className= terms.length>5?terms[5]:"";
          $.log("groupId:artifactId:version:classifier:packaging:className="+groupId+':'+artifactId+':'+version+':'+classifier+':'+packaging+':'+className);
          var searchViewModel = new SearchViewModel();
          var searchRequest = new SearchRequest();
          searchRequest.groupId(groupId);
          searchRequest.artifactId(artifactId);
          searchRequest.version(version);
          searchRequest.classifier(classifier);
          searchRequest.packaging(packaging);
          searchRequest.className(className);
          //searchRequest.repositories=repos;
          //searchRequest.selectedRepoIds=repos;
          searchViewModel.searchRequest(searchRequest);
          displaySearch(function(){
            searchViewModel.search("restServices/archivaServices/searchService/searchArtifacts",repos);
          },searchViewModel);
        };

        this.get("#advancedsearch/:queryterms",function(){
          advancedSearchRoute(this.params);
        });

        this.get("#advancedsearch~:repositoryIds/:queryterms",function(){
          advancedSearchRoute(this.params);
        });


        this.get('#open-admin-create-box',function(){
          $.log("#open-admin-create-box");
          adminCreateBox();
        });

        // #artifact-(optionnal repositoryId)
        // format groupId:artifactId org.apache.maven.plugins:maven-jar-plugin
        // or  groupId:artifactId:version org.apache.maven.plugins:maven-jar-plugin:2.3.1
        this.get('#artifact/:groupId/:artifactId',function(context){
          var groupId= this.params.groupId;
          var artifactId= this.params.artifactId;
          $.log("get #artifact:"+groupId+":"+artifactId);
          goToBrowseArtifactDetail(groupId,artifactId);//,null,null);
        });
        this.get('#artifact~:repositoryId/:groupId/:artifactId',function(context){
          var groupId= this.params.groupId;
          var artifactId= this.params.artifactId;
          var repositoryId = this.params.repositoryId;
          $.log("get #artifact:"+groupId+":"+artifactId);
          goToBrowseArtifactDetail(groupId,artifactId,repositoryId);//,null,null);
        });

        var checkArtifactDetailContent=function(groupId,artifactId,version,repositoryId,tabToActivate,idContentToCheck,contentDisplayFn){
          // no need to recalculate all stuff just activate the tab
          var htmlId = idContentToCheck?idContentToCheck:"browse_artifact_detail";
          // olamy: cause some issues when browsing so desactivate this fix until more check
          // navigating from dependencies list or dependency or used by of an artifact to fix in search.js
          /*
          var htmlIdSelect = $("#main-content").find("#"+htmlId );
          if(htmlIdSelect.html()!=null){
            if( $.trim(htmlIdSelect.html().length)>0){
              $("#main-content #"+tabToActivate).tab('show');
              $.log("checkArtifactDetailContent " + htmlId + " html not empty no calculation, tabToActivate:"+tabToActivate);
              return;
            }
          }
          */

          var artifactAvailableUrl="restServices/archivaServices/browseService/artifactAvailable/"+encodeURIComponent(groupId)+"/"+encodeURIComponent(artifactId);
          artifactAvailableUrl+="/"+encodeURIComponent(version);
          var selectedRepo=getSelectedBrowsingRepository();
          if (selectedRepo){
            artifactAvailableUrl+="?repositoryId="+encodeURIComponent(selectedRepo);
          }
          $("#main-content").html( mediumSpinnerImg());
          $.ajax(artifactAvailableUrl, {
            type: "GET",
            dataType: 'json',
            success: function(data) {
              // TODO take of the result true or false
              //$.log("artifactAvailable:"+data);
              generalDisplayArtifactDetailsVersionView(groupId,artifactId,version,repositoryId,
                                                       function(artifactVersionDetailViewModel){
                                                         $("#main-content #"+tabToActivate).tab('show');
                                                         if(contentDisplayFn){
                                                           contentDisplayFn(groupId,artifactId,version,artifactVersionDetailViewModel);
                                                         }
                                                       }
              );

            }
          });


        };

        this.get('#artifact/:groupId/:artifactId/:version',function(context){

          var repositoryId = this.params.repositoryId;
          var groupId= this.params.groupId;
          var artifactId= this.params.artifactId;
          var version= this.params.version;
          checkArtifactDetailContent(groupId,artifactId,version,repositoryId,"artifact-details-info-content-a");
        });
        this.get('#artifact~:repositoryId/:groupId/:artifactId/:version',function(context){

          var repositoryId = this.params.repositoryId;
          var groupId= this.params.groupId;
          var artifactId= this.params.artifactId;
          var version= this.params.version;
          checkArtifactDetailContent(groupId,artifactId,version,repositoryId,"artifact-details-info-content-a");
        });

        this.get('#artifact-dependencies/:groupId/:artifactId/:version',function(context){

          var repositoryId = this.params.repositoryId;
          var groupId= this.params.groupId;
          var artifactId= this.params.artifactId;
          var version= this.params.version;

          checkArtifactDetailContent(groupId,artifactId,version,repositoryId,"artifact-details-dependencies-content-a");

        });

        this.get('#artifact-dependencies~:repositoryId/:groupId/:artifactId/:version',function(context){

          var repositoryId = this.params.repositoryId;
          var groupId= this.params.groupId;
          var artifactId= this.params.artifactId;
          var version= this.params.version;
          checkArtifactDetailContent(groupId,artifactId,version,repositoryId,"artifact-details-dependencies-content-a");
        });

        this.get('#artifact-details-files-content/:groupId/:artifactId/:version',function(context){

          var repositoryId = this.params.repositoryId;
          var groupId= this.params.groupId;
          var artifactId= this.params.artifactId;
          var version= this.params.version;

          checkArtifactDetailContent(groupId,artifactId,version,null,"artifact-details-files-content-a","artifact-details-files-content",
                                     function(groupId,artifactId,version,artifactVersionDetailViewModel){
                                       displayArtifactFilesContent(artifactVersionDetailViewModel);
                                     });

        });

        this.get('#artifact-details-files-content~:repositoryId/:groupId/:artifactId/:version',function(context){

          var repositoryId = this.params.repositoryId;
          var groupId= this.params.groupId;
          var artifactId= this.params.artifactId;
          var version= this.params.version;
          checkArtifactDetailContent(groupId,artifactId,version,repositoryId,"artifact-details-files-content-a","artifact-details-files-content",
                                     function(groupId,artifactId,version,artifactVersionDetailViewModel){
                                       displayArtifactFilesContent(artifactVersionDetailViewModel);
                                     });
        });


        this.get('#artifact-details-download-content/:groupId/:artifactId/:version',function(context){

          var repositoryId = this.params.repositoryId;
          var groupId= this.params.groupId;
          var artifactId= this.params.artifactId;
          var version= this.params.version;

          checkArtifactDetailContent(groupId,artifactId,version,null,"artifact-details-download-content-a","artifact-details-download-content",
                                     function(groupId,artifactId,version,artifactVersionDetailViewModel){
                                       displayArtifactDownloadContent(artifactVersionDetailViewModel);
                                     });

        });

        this.get('#artifact-details-download-content~:repositoryId/:groupId/:artifactId/:version',function(context){

          var repositoryId = this.params.repositoryId;
          var groupId= this.params.groupId;
          var artifactId= this.params.artifactId;
          var version= this.params.version;
          checkArtifactDetailContent(groupId,artifactId,version,repositoryId,"artifact-details-download-content-a","artifact-details-download-content",
                                     function(groupId,artifactId,version,artifactVersionDetailViewModel){
                                       displayArtifactDownloadContent(artifactVersionDetailViewModel);
                                     });
        });


        this.get('#artifact-dependency-tree/:groupId/:artifactId/:version',function(context){

          var repositoryId = this.params.repositoryId;
          var groupId= this.params.groupId;
          var artifactId= this.params.artifactId;
          var version= this.params.version;
          checkArtifactDetailContent(groupId,artifactId,version,repositoryId,"artifact-details-dependency-tree-content-a");
        });

        this.get('#artifact-dependency-tree~:repositoryId/:groupId/:artifactId/:version',function(context){

          var repositoryId = this.params.repositoryId;
          var groupId= this.params.groupId;
          var artifactId= this.params.artifactId;
          var version= this.params.version;
          checkArtifactDetailContent(groupId,artifactId,version,repositoryId,"artifact-details-dependency-tree-content-a");
        });

        this.get('#artifact-mailing-list/:groupId/:artifactId/:version',function(context){

          var repositoryId = this.params.repositoryId;
          var groupId= this.params.groupId;
          var artifactId= this.params.artifactId;
          var version= this.params.version;
          checkArtifactDetailContent(groupId,artifactId,version,repositoryId,"artifact-details-mailing-list-content-a");
        });

        this.get('#artifact-mailing-list~:repositoryId/:groupId/:artifactId/:version',function(context){

          var repositoryId = this.params.repositoryId;
          var groupId= this.params.groupId;
          var artifactId= this.params.artifactId;
          var version= this.params.version;
          checkArtifactDetailContent(groupId,artifactId,version,repositoryId,"artifact-details-mailing-list-content-a");
        });


        var calculateUsedBy=function(groupId,artifactId,version){
          var dependeesContentDiv=$("#main-content" ).find("#artifact-details-used-by-content" );
          var dependeesTable=dependeesContentDiv.find("#artifact-usedby-table");

          dependeesContentDiv.append(mediumSpinnerImg());
          var dependeesUrl="restServices/archivaServices/browseService/dependees/"+encodeURIComponent(groupId);
          dependeesUrl+="/"+encodeURIComponent(artifactId);
          dependeesUrl+="/"+encodeURIComponent(version);
          var selectedRepo=getSelectedBrowsingRepository();
          if (selectedRepo){
            dependeesUrl+="?repositoryId="+encodeURIComponent(selectedRepo);
          }
          $.ajax(dependeesUrl, {
            type: "GET",
            dataType: 'json',
            success: function(data) {
              var artifacts=mapArtifacts(data);
              var gridViewModel = new ko.simpleGrid.viewModel({
                data: artifacts,
                columns: [],
                pageSize: 7,
                gridUpdateCallBack: function(){
                  // no op
                }
              });
              $.log("artifacts:"+artifacts.length);
              dependeesTable.attr("data-bind",
                                       "simpleGrid: gridViewModel,simpleGridTemplate:'dependees_tmpl',pageLinksId:'usedbyPagination',data:'artifacts'");
              ko.applyBindings({artifacts:artifacts,gridViewModel:gridViewModel},dependeesContentDiv.get(0));
            },
            complete: function(){
              removeMediumSpinnerImg(dependeesContentDiv);
            }
          });

        };

        this.get('#artifact-used-by/:groupId/:artifactId/:version',function(context){

          var repositoryId = this.params.repositoryId;
          var groupId= this.params.groupId;
          var artifactId= this.params.artifactId;
          var version= this.params.version;
          checkArtifactDetailContent(groupId,artifactId,version,repositoryId,"artifact-details-used-by-content-a","artifact-details-used-by-content",calculateUsedBy);
        });

        this.get('#artifact-used-by~:repositoryId/:groupId/:artifactId/:version',function(context){

          var repositoryId = this.params.repositoryId;
          var groupId= this.params.groupId;
          var artifactId= this.params.artifactId;
          var version= this.params.version;
          checkArtifactDetailContent(groupId,artifactId,version,repositoryId,"artifact-details-used-by-content-a","artifact-details-used-by-content",calculateUsedBy);
        });

        var calculateMetadatas=function(groupId,artifactId,version,artifactVersionDetailViewModel){

          var metadatasContentDiv=$("#main-content" ).find("#artifact-details-metadatas-content" );
          var metadatasUrl="restServices/archivaServices/browseService/metadatas/"+encodeURIComponent(groupId);
          metadatasUrl+="/"+encodeURIComponent(artifactId);
          metadatasUrl+="/"+encodeURIComponent(version);
          var selectedRepo=getSelectedBrowsingRepository();
          if (selectedRepo){
            metadatasUrl+="?repositoryId="+encodeURIComponent(selectedRepo);
          }

          //fixe self.entries not in the scope

          $.ajax(metadatasUrl, {
            type: "GET",
            dataType: 'json',
            success: function(data) {
              var entries= $.map(data,function(e,i){
                return new MetadataEntry( e.key, e.value,false);
              });
              artifactVersionDetailViewModel.entries(entries);
            }
          });
        };

        this.get('#artifact-metadatas/:groupId/:artifactId/:version',function(context){

          var repositoryId = this.params.repositoryId;
          var groupId= this.params.groupId;
          var artifactId= this.params.artifactId;
          var version= this.params.version;
          checkArtifactDetailContent(groupId,artifactId,version,repositoryId,"artifact-details-metadatas-content-a",null,calculateMetadatas);
        });

        this.get('#artifact-metadatas~:repositoryId/:groupId/:artifactId/:version',function(context){

          var repositoryId = this.params.repositoryId;
          var groupId= this.params.groupId;
          var artifactId= this.params.artifactId;
          var version= this.params.version;
          checkArtifactDetailContent(groupId,artifactId,version,repositoryId,"artifact-details-metadatas-content-a",null,calculateMetadatas);
        });


        this.get('#browse/:groupId',function(context){
          var groupId = this.params.groupId;
          if (groupId){
            displayBrowseGroupId(groupId);
          } else {
            displayBrowse(true);
          }
        });
        this.get('#browse~:repositoryId/:groupId',function(context){
          var groupId = this.params.groupId;
          var repositoryId = this.params.repositoryId;
          $.log("repositoryId:"+repositoryId);
          if (groupId){
            displayBrowseGroupId(groupId,repositoryId);
          } else {
            displayBrowse(true,repositoryId);
          }
        });
        this.get('#browse~:repositoryId',function(context){
          var repositoryId = this.params.repositoryId;
          $.log("repositoryId:"+repositoryId);
          displayBrowse(true,repositoryId);
        });

        this.get('#welcome', function () {
          $.log("#welcome hash");
          checkCreateAdminLink(function(){window.sammyArchivaApplication.setLocation("#search")});

        });

        this.get('#:folder', function () {
          var folder = this.params.folder;
          self.activeMenuId(folder);
          var baseItems = self.artifactMenuItems()?self.artifactMenuItems():[];
          ko.utils.arrayFirst(baseItems.concat(self.usersMenuItems(), self.administrationMenuItems(),self.docsMenuItems()), function(p) {
            if ( p.href == "#"+self.activeMenuId()) {
              screenChange();
              p.func();
            }
          });
        });

        this.get("#rest-docs-archiva-rest-api/:target",function(){
          var target=this.params.target;
          $.log("archiva-rest-docs, target:"+target);
          goToArchivaRestDoc(target);
        });

        this.get("#rest-docs-archiva-ui/:target",function(){
          var target=this.params.target;
          $.log("archiva-rest-docs-ui, target:"+target);
          goToArchivaRestUiDoc(target);
        });

        this.get("#rest-docs-redback-rest-api/:target",function(){
          var target=this.params.target;
          $.log("redback-rest-docs, target:"+target);
          goToRedbackRestDoc(target);
        });

        this.get("#managedrepositoryedit/:repositoryId",function(){
          var repositoryId=this.params.repositoryId;
          $.log("edit managed repository:"+repositoryId);
          displayRepositoriesGrid(function(managedRepositoriesViewModel){managedRepositoriesViewModel.editManagedRepositoryWithId(repositoryId)});
        });

      });
  };

  userLoggedCallbackFn=function(user){
    $.log("userLoggedCallbackFn:"+ (user?user.username:null));
    var loginLink=$("#login-link");
    var registerLink=$("#register-link");
    var changePasswordLink=$("#change-password-link");
    if (!user) {
      loginLink.show();
      registerLink.show();
      changePasswordLink.hide();
      checkUrlParams();
    } else {
      changePasswordLink.show();
      $("#logout-link").show();
      registerLink.hide();
      loginLink.hide();
      decorateMenuWithKarma(user);
    }
  };

  checkSecurityLinks=function(){
    userLogged(userLoggedCallbackFn);
  };

  checkCreateAdminLink=function(callbackFn){
    $.ajax("restServices/redbackServices/userService/isAdminUserExists", {
      type: "GET",
      dataType: 'json',
      success: function(data) {
        var adminExists = data;
        window.archivaModel.adminExists=adminExists;
        var createAdminLink=$("#create-admin-link");
        if (adminExists == false) {
          createAdminLink.show();
          $("#login-link").hide();
          $("#register-link").hide();
        } else {
          createAdminLink.hide();
        }
        if(callbackFn){
          callbackFn()
        }
        $.log("adminExists:"+adminExists);
      }
    });
  };

  // handle url with registration link
  checkUrlParams=function(){
    var validateMeId = $.urlParam('validateMe');
    if (validateMeId) {
      validateKey(validateMeId);
      return;
    }
    var resetPassword= $.urlParam('resetPassword');
    if (resetPassword){
      resetPasswordForm(resetPassword);
      return;
    }

    var matches = window.location.toString().match(/^[^#]*(#.+)$/);
    var hash = matches ? matches[1] : '';
    $.log("location:"+window.sammyArchivaApplication.getLocation()+",hash:"+hash);
    // by default display welcome screen
    if(!hash){
      window.sammyArchivaApplication.setLocation("#welcome");
    }

  };

  hasKarma=function(karmaName){
    return $.inArray(karmaName,window.redbackModel.operatioNames)>=0;
  };

  startArchivaApplication=function(){

    $.log("startArchivaApplication");
    loadRedbackTemplate();
    loadArchivaTemplate();
    $('#topbar-menu-container').html($("#topbar_menu_tmpl" ).tmpl());
    $('#sidebar-content').html($("#main_menu_tmpl").tmpl());

    ko.bindingHandlers.redbackP = {
      init: function(element, valueAccessor) {
          $(element).attr("redback-permissions",valueAccessor);
          }
    };

    ko.applyBindings(new MainMenuViewModel());

    hideElementWithKarma();
    checkSecurityLinks();
    checkCreateAdminLink();
    $('#footer-content').html($('#footer-tmpl').tmpl(window.archivaRuntimeInfo));

    updateAppearanceToolBar();

    window.sammyArchivaApplication.run();

  };

  drawQuickSearchAutocomplete=function(){

    $( "#quick-search-autocomplete" ).autocomplete({
      minLength: 3,
      delay: 600,
			source: function(request, response){
        $.get("restServices/archivaServices/searchService/quickSearch?queryString="+encodeURIComponent(request.term),
           function(data) {
             var res = mapArtifacts(data);
             var uniqId = [];
             var uniqArtifactIds=[];
             for (var i= 0;i<res.length;i++){
               if ( $.inArray(res[i].artifactId,uniqId)<0){
                 uniqId.push(res[i].artifactId);
                 uniqArtifactIds.push(res[i]);
               }
             }
             response(uniqArtifactIds);
           }
        );
      },
      select: function( event, ui ) {
        $.log("select artifactId:"+ui.item.artifactId);
        window.sammyArchivaApplication.setLocation("#quicksearch~"+ui.item.artifactId);
      }
		}).data( "autocomplete" )._renderItem = function( ul, item ) {
							return $( "<li></li>" )
								.data( "item.autocomplete", item )
								.append( "<a>" + item.artifactId + "</a>" )
								.appendTo( ul );
						};

  }


});

