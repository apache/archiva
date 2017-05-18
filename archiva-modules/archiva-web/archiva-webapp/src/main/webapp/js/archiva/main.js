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
         "jquery.validate","jquery.json","knockout","typeahead","hogan","redback.templates","archiva.templates",
          "redback.roles","redback","archiva.artifacts-management","archiva.docs"],
function(jquery,ui,sammy,tmpl,i18n,jqueryCookie,bootstrap,archivaSearch,jqueryValidate,jqueryJson,ko,typeahead) {

  /**
   * record a cookie for session with the logged user
   * @param user see user.js
   */
  reccordLoginCookie=function(user) {

    var path = window.cookieInformation.path;
    path = path ? path : "/";

    var domain = window.cookieInformation.domain;
    var secure = window.cookieInformation.secure;

    var expires= Number(window.cookieInformation.timeout);

    var kUser = new User(user.username, user.password, user.confirmPassword,user.fullName,user.email,user.permanent,user.validated,
                         user.timestampAccountCreation,user.timestampLastLogin,user.timestampLastPasswordChange,user.locked,
                         user.passwordChangeRequired,null,user.readOnly,user.userManagerId, user.validationToken);

    kUser.rememberme(user.rememberme());
    var userJson=ko.toJSON(kUser);

    var options = null;
    if (secure == 'true'){
      options = {
        expires: expires,
        path: path,
        domain: domain,
        secure: secure
      }
    }else {
      options = {
        expires: expires,
        path: path,
        domain: domain
      }
    }

    $.cookie('archiva_login',userJson,options);
  };

  getUserFromLoginCookie=function(){
    var cookieContent=$.cookie('archiva_login');
    $.log("archiva_getUserFromLoginCookie cookie content:"+cookieContent);
    if (!cookieContent) {
      return null;
    }
    var user = $.parseJSON(cookieContent);
    if(!user){
      return null;
    }
    var kUser = new User(user.username, user.password, user.confirmPassword,user.fullName,user.email,user.permanent,user.validated,
                    user.timestampAccountCreation,user.timestampLastLogin,user.timestampLastPasswordChange,user.locked,
                    user.passwordChangeRequired,null,user.readOnly,user.userManagerId, user.validationToken);

    $.log("user.rememberme:"+user.rememberme);

    kUser.rememberme(user.rememberme);
    return kUser;
  };



  logout=function(){
    var user = window.user;//getUserFromLoginCookie();
    if(user){
      user.logged=false;
      reccordLoginCookie(user);
    }
    $("#login-link").show();
    $("#register-link").show();
    $("#logout-link").hide();
    $("#change-password-link").hide();
    // cleanup karmas
    window.redbackModel.operatioNames=[];
    hideElementWithKarma();
    $("#main-content").empty();
    $("#user-messages" ).empty();
    $("#login-welcome" ).hide();
    $.ajax({
      url: 'restServices/redbackServices/loginService/logout'
    } ).always(
        function(){
          window.sammyArchivaApplication.setLocation("#welcome");
          displayWelcome();
        }
    );
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
        var organisationLogo=topbarMenu.find("#organisation-logo");
        if( data){
          $.log("disableRegistration");
          topbarMenu.find("#register-link" ).hide();

        }
        $.ajax("restServices/archivaServices/archivaAdministrationService/getOrganisationInformation", {
            type: "GET",
            dataType: 'json',
            success: function(data) {

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
        {text: $.i18n.prop('menu.administration'), id: null ,order : 1}     ]);
        
        var pluginsURL = "restServices/archivaServices/pluginsService/getAdminPlugins";
        $.ajax(pluginsURL, {
            type: "GET",
            dataType: 'text',
            
            success: function(data) {
               $.each(data.split("|"), function(key, value) {
                    require([value], function() {
                        showMenu(self.administrationMenuItems);
                        // sort menu according to order field
                        // 
                        self.administrationMenuItems.sort(function(left, right) {
                            return left.order == right.order ? 0 : (left.order < right.order ? -1 : 1)
                        })
                    });

                });
                
            }

        });
        
       
        this.usersMenuItems = ko.observableArray([
        {  text : $.i18n.prop('menu.users') , id: null},
        {  text : $.i18n.prop('menu.users.manage')          , id: "menu-users-list-a"                  , href: "#users"         , redback: "{permissions: ['archiva-manage-users']}", func: function(){displayUsersGrid();}},
        {  text : $.i18n.prop('menu.users.roles')           , id: "menu-roles-list-a"                  , href: "#roles"         , redback: "{permissions: ['archiva-manage-users']}", func: function(){displayRolesGrid();}},
        {  text : $.i18n.prop('menu.users-runtime-configuration') , id: "menu-redback-runtime-configuration-list-a"  , href: "#redbackruntimeconfig" , redback: "{permissions: ['archiva-manage-configuration']}", func: function(){displayRedbackRuntimeConfiguration();}}
      ]);

      this.docsMenuItems = ko.observableArray([
        {  text : $.i18n.prop('menu.docs') , id: null},
        {  text : $.i18n.prop('menu.docs.rest')    , id: "menu-docs-rest-list-a" , href: "#docs-rest", target: "", func: function(){displayRestDocs()}},
        {  text : $.i18n.prop('menu.docs.users')   , id: "menu-docs-users-list-a", href: "http://archiva.apache.org/docs/"+window.archivaRuntimeInfo.version, target: "_blank", func: function(){displayUsersDocs()}}
      ]);

      this.activeMenuId = ko.observable();
          
      window.sammyArchivaApplication = Sammy(function () {

        this.get('#quicksearch~:artifactId',function(){
          self.activeMenuId("search");
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
          self.activeMenuId("search");
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
          self.activeMenuId("search");
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
          self.activeMenuId("search");
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
          self.activeMenuId("search");
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
          self.activeMenuId("search");
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
          var pageSize= terms.length>6?terms[6]:"";
          $.log("groupId:artifactId:version:classifier:packaging:className="+groupId+':'+artifactId+':'+version+':'+classifier+':'+packaging+':'+className);
          var searchViewModel = new SearchViewModel();
          var searchRequest = new SearchRequest();
          searchRequest.groupId(groupId);
          searchRequest.artifactId(artifactId);
          searchRequest.version(version);
          searchRequest.classifier(classifier);
          searchRequest.packaging(packaging);
          searchRequest.className(className);
          searchRequest.pageSize(pageSize);
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
          self.activeMenuId("browse");
          goToBrowseArtifactDetail(groupId,artifactId);//,null,null);
        });
        this.get('#artifact~:repositoryId/:groupId/:artifactId',function(context){
          var groupId= this.params.groupId;
          var artifactId= this.params.artifactId;
          var repositoryId = this.params.repositoryId;
          self.activeMenuId("browse");
          $.log("get #artifact:"+groupId+":"+artifactId);
          goToBrowseArtifactDetail(groupId,artifactId,repositoryId);//,null,null);
        });


        var checkArtifactDetailContent=function(groupId,artifactId,version,repositoryId,tabToActivate,idContentToCheck,contentDisplayFn,classifier){
          self.activeMenuId("browse");
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
          if(classifier){
            artifactAvailableUrl+="/"+encodeURIComponent(classifier);
          }

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
                                                         $("#main-content" ).find("#browse_artifact" ).hide();
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

        this.get('#artifact/:groupId/:artifactId/:version/:classifier',function(context){
          var groupId= this.params.groupId;
          var artifactId= this.params.artifactId;
          var version= this.params.version;
          var classifier= this.params.classifier;
          checkArtifactDetailContent(groupId,artifactId,version,null,"artifact-details-info-content-a",null,null,classifier);
        });

        this.get('#artifact~:repositoryId/:groupId/:artifactId/:version/:classifier',function(context){

          var repositoryId = this.params.repositoryId;
          var groupId= this.params.groupId;
          var artifactId= this.params.artifactId;
          var version= this.params.version;
          var classifier= this.params.classifier;
          checkArtifactDetailContent(groupId,artifactId,version,repositoryId,"artifact-details-info-content-a",null,null,classifier);
        });

        this.get('#artifact-dependencies/:groupId/:artifactId/:version',function(context){

          var repositoryId = this.params.repositoryId;
          var groupId= this.params.groupId;
          var artifactId= this.params.artifactId;
          var version= this.params.version;

          checkArtifactDetailContent(groupId,artifactId,version,repositoryId,"artifact-details-dependencies-content-a");

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

        this.get('#artifact-dependency-graph/:groupId/:artifactId/:version', function(context) {

          var repositoryId = this.params.repositoryId;
          var groupId= this.params.groupId;
          var artifactId= this.params.artifactId;
          var version= this.params.version;
          checkArtifactDetailContent(groupId,artifactId,version,repositoryId,"artifact-details-dependency-graph-content-a");
        });

        this.get('#artifact-dependency-graph~:repository/::groupId/:artifactId/:version', function(context) {

          var repositoryId = this.params.repositoryId;
          var groupId= this.params.groupId;
          var artifactId= this.params.artifactId;
          var version= this.params.version;
          checkArtifactDetailContent(groupId,artifactId,version,repositoryId,"artifact-details-dependency-graph-content-a");
        })

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
            displayBrowse(true,null);
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
          displayWelcome();

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

        this.get("#repositorygroupedit/:groupId",function(){
          var groupId=this.params.groupId;
          $.log("edit repository group:"+groupId);
          displayRepositoryGroups(function(repositoryGroupsViewModel){repositoryGroupsViewModel.editRepositoryGroupWithId(groupId)});

        });

      });
  };

  displayWelcome=function(){
    checkCreateAdminLink(function(){
      $("#main-content" ).html($("#welcome" ).tmpl({runtimeInfo: window.archivaRuntimeInfo}));
      drawQuickSearchAutocomplete("#quick-search-autocomplete-welcome");
      updateAppearanceToolBar();
    });
  };

  userLoggedCallbackFn=function(user){
    $.log("userLoggedCallbackFn:"+ (user?user.username():null));
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
    $.log("checkSecurityLinks");
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
        if(callbackFn) {
          callbackFn(adminExists)
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

  addValidationTokenHeader=function(user) {
    if(user) {
      if (user.validationToken) {
        $.log("Adding validation token "+user.validationToken);
        $.ajaxSetup({
                      beforeSend: function (xhr) {
                        xhr.setRequestHeader('X-XSRF-TOKEN', user.validationToken);
                      }
                    });
      } else {
        $.log("No validation token in user object "+user.username+", "+user.validationToken);
      }
    }
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


    var user = getUserFromLoginCookie();

    $.log("found user:"+(user==null?"null":user.username()+":"+user.password()+":"+user.rememberme()));

    // if user has details as username and passowrd and rememberme is on just try to log it
    if (user!=null&&user.username()!=null&&user.password()!=null&&user.rememberme()==true){
      window.redbackModel.rememberme=user.rememberme();
      window.redbackModel.password=user.password();
      loginCall(user.username(),user.password(),user.rememberme()
          ,successLoginCallbackFn,errorLoginCallbackFn,completeLoginCallbackFn);
    } else {
      // Token for origin validation
      addValidationTokenHeader(user);
    }

  };


  /**
   * callback success function on rest login call.
   * modal close and hide/show some links (login,logout,register...)
   * @param result
   */
   successLoginCallbackFn=function(result){

    var logged = false;
    if (result == null) {
      logged = false;
    } else {
      if (result.username) {
        logged = true;
      }
    }
    if (logged == true) {
      var user = mapUser(result);
      addValidationTokenHeader(user);

      if (user.passwordChangeRequired()==true){
        changePasswordBox(true,false,user);
        return;
      }
      // not really needed as an exception is returned but "ceintures et bretelles" as we say in French :-)
      if (user.locked()==true){
        $.log("user locked");
        displayErrorMessage($.i18n.prop("account.locked"));
        return;
      }


      $.log("window.redbackModel.rememberme:"+window.redbackModel.rememberme);
      user.rememberme(window.redbackModel.rememberme);
      if(user.rememberme()){
        user.password(window.redbackModel.password);
      }
      $.log("user.rememberme:"+(user.rememberme()));
      reccordLoginCookie(user);
      window.user=user;
      $("#login-link").hide();
      $("#logout-link").show();
      $("#register-link").hide();
      $("#change-password-link").show();
      if (window.modalLoginWindow){
        window.modalLoginWindow.modal('hide');
      }
      clearForm("#user-login-form");
      decorateMenuWithKarma(user);

      // Token for origin validation
      $("#login-welcome" ).show();
      $("#welcome-label" ).html( $.i18n.prop("user.login.welcome",user.username()));
      return;
    }
    var modalLoginErrMsg=$("#modal-login-err-message");
    modalLoginErrMsg.html($.i18n.prop("incorrect.username.password"));
    modalLoginErrMsg.show();
  };

  /**
   * callback error function on rest login call. display error message
   * @param result
   */
  errorLoginCallbackFn= function(result) {
    var obj = jQuery.parseJSON(result.responseText);
    displayRedbackError(obj,"modal-login-err-message");
    $("#modal-login-err-message").show();
  };

  /**
   * callback complate function on rest login call. remove spinner from modal login box
   * @param result
   */
  completeLoginCallbackFn=function(){
    $("#modal-login-ok").button("reset");
    $("#small-spinner").remove();
    // force current screen reload to consider user karma
    window.sammyArchivaApplication.refresh();
  };

  drawQuickSearchAutocomplete=function(selector){
    var box = $( selector ? selector : "#quick-search-autocomplete" );
    box.typeahead(
        {
          name: 'quick-search-result',
          remote: 'restServices/archivaServices/searchService/quickSearch?queryString=%QUERY',
          valueKey: 'artifactId',
          maxParallelRequests:0,
          limit: 50
        }
    );

    box.bind('typeahead:selected', function(obj, datum, name) {
      window.sammyArchivaApplication.setLocation("#quicksearch~" + datum.artifactId);
    });
  };


});

