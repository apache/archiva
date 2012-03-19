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

  //-----------------------------------------
  // browse part
  //-----------------------------------------

  BrowseViewModel=function(browseResultEntries,parentBrowseViewModel,groupId){
    var self=this;
    this.browseResultEntries=browseResultEntries;
    this.parentBrowseViewModel=parentBrowseViewModel;
    this.groupId=groupId;
    displayGroupId=function(groupId){
      displayGroupDetail(groupId,self);
    }
    displayParentGroupId=function(){
      $.log("called displayParentGroupId groupId:"+self.parentBrowseViewModel.groupId);
      // if null parent is root level
      if (self.parentBrowseViewModel.groupId){
        displayGroupDetail(self.parentBrowseViewModel.groupId,self.parentBrowseViewModel);
      } else {
        browseRoot();
      }
    }

    displayProjectEntry=function(id){
      // value org.apache.maven/maven-archiver
      // split this org.apache.maven and maven-archiver
      var values = id.split(".");
      var groupId="";
      for (var i = 0;i<values.length-1;i++){
        groupId+=values[i];
        if (i<values.length-2)groupId+=".";
      }
      var artifactId=values[values.length-1];
      displayArtifactDetail(groupId,artifactId,self);

    }

    breadCrumbEntries=function(){
      // root level ?
      if (!self.parentBrowseViewModel) return [];
      return calculateBreadCrumbEntries(self.groupId);
    }

    displayEntry=function(value){
      if (self.groupId){
        return value.substr(self.groupId.length+1,value.length-self.groupId.length);
      }
      return value;
    }
  }

  calculateBreadCrumbEntries=function(groupId){
    var splitted = groupId.split(".");
    var breadCrumbEntries=[];
    var curGroupId="";
    for (var i=0;i<splitted.length;i++){
      curGroupId+=splitted[i];
      breadCrumbEntries.push(new BreadCrumbEntry(curGroupId,splitted[i]));
      $.log("calculateBreadCrumbEntries:"+curGroupId+":"+splitted[i]);
      curGroupId+="."
    }
    return breadCrumbEntries;
  }

  displayGroupDetail=function(groupId,parentBrowseViewModel,restUrl){
    var mainContent = $("#main-content");
    mainContent.find("#browse_artifact_detail").hide();
    var browseResult=mainContent.find("#browse_result");
    browseResult.show();
    mainContent.find("#browse_artifact" ).hide();
    var browseBreadCrumb=mainContent.find("#browse_breadcrumb");
    mainContent.find("#main_browse_result_content").hide( "slide", {}, 300,
        function(){
          browseResult.html(mediumSpinnerImg());
          browseBreadCrumb.html(smallSpinnerImg());
          mainContent.find("#main_browse_result_content" ).show();
          var url = "";
          if (!restUrl) {
            url="restServices/archivaServices/browseService/browseGroupId/"+encodeURIComponent(groupId);
            var selectedRepo=getSelectedBrowsingRepository();
            if (selectedRepo){
              url+="?repositoryId="+selectedRepo;
            }
          }else {
            url=restUrl;
          }

          $.ajax(url, {
            type: "GET",
            dataType: 'json',
            success: function(data) {
              var browseResultEntries = mapbrowseResultEntries(data);
              var browseViewModel = new BrowseViewModel(browseResultEntries,parentBrowseViewModel,groupId);
              ko.applyBindings(browseViewModel,browseBreadCrumb.get(0));
              ko.applyBindings(browseViewModel,browseResult.get(0));
              enableAutocompleBrowse(groupId);
            }
         });
        }
    );
  }

  ArtifactDetailViewModel=function(groupId,artifactId){
    var self=this;
    this.versions=[];
    this.projectVersionMetadata=null;
    this.groupId=groupId;
    this.artifactId=artifactId;
    breadCrumbEntries=function(){
      var entries = calculateBreadCrumbEntries(self.groupId);
      entries.push(new BreadCrumbEntry("foo",self.artifactId));
      return entries;
    }
    displayArtifactInfo=function(){
      if ($("#main-content #artifact-info:visible" ).length>0) {
        $("#main-content #artifact-info" ).hide();
      } else {
        $("#main-content #artifact-info" ).show();
      }
    }

    displayArtifactVersionDetail=function(version){
      var artifactVersionDetailViewModel=new ArtifactVersionDetailViewModel(self.groupId,self.artifactId,version,getSelectedBrowsingRepository());
      artifactVersionDetailViewModel.display();
    }

  }

  ArtifactVersionDetailViewModel=function(groupId,artifactId,version){
    var mainContent = $("#main-content");
    var self=this;
    this.groupId=groupId;
    this.artifactId=artifactId;
    this.version=version;
    this.projectVersionMetadata=null;

    displayGroupId=function(groupId){
      displayGroupDetail(groupId,null);
    }

    breadCrumbEntries=function(){
      var entries = calculateBreadCrumbEntries(self.groupId);
      var artifactBreadCrumbEntry = new BreadCrumbEntry(self.groupId,self.artifactId);
      artifactBreadCrumbEntry.artifactId=self.artifactId;
      artifactBreadCrumbEntry.artifact=true;
      entries.push(artifactBreadCrumbEntry);
      entries.push(new BreadCrumbEntry("foo",self.version));
      return entries;
    }
    this.display=function(){
      mainContent.find("#browse_breadcrumb").hide("slide", {}, 300,function(){
        mainContent.find("#browse_artifact").hide("slide", {}, 300,function(){
          mainContent.find("#browse_artifact_detail").show();
          mainContent.find("#browse_artifact_detail").html(mediumSpinnerImg());
          mainContent.find("#browse_breadcrumb" ).show();
          mainContent.find("#browse_breadcrumb" ).html(mediumSpinnerImg());
          var metadataUrl="restServices/archivaServices/browseService/projectVersionMetadata/"+encodeURIComponent(groupId)+"/"+encodeURIComponent(artifactId);
          var selectedRepo=getSelectedBrowsingRepository();
          if (selectedRepo){
            metadataUrl+="?repositoryId="+encodeURIComponent(selectedRepo);
          }

          $.ajax(metadataUrl, {
            type: "GET",
            dataType: 'json',
            success: function(data) {
              self.projectVersionMetadata=mapProjectVersionMetadata(data);
              ko.applyBindings(self,mainContent.find("#browse_artifact_detail" ).get(0));
              ko.applyBindings(self,mainContent.find("#browse_breadcrumb" ).get(0));
              mainContent.find("#browse-autocomplete" ).hide();
              mainContent.find("#browse-autocomplete-divider" ).hide();
              mainContent.find("#artifact-details-tabs").on('show', function (e) {
                if ($(e.target).attr("href")=="#artifact-details-dependency-tree-content") {
                  $.log("#artifact-details-dependency-tree-content");
                }
                if ($(e.target).attr("href")=="#artifact-details-used-by-content") {
                  $.log("#artifact-details-used-by-content");
                }

              });
            }
          });

        });
      });
    }
  }

  displayArtifactDetail=function(groupId,artifactId,parentBrowseViewModel,restUrl){
    var artifactDetailViewModel=new ArtifactDetailViewModel(groupId,artifactId);
    var mainContent = $("#main-content");
    mainContent.find("#browse_artifact_detail" ).hide();
    mainContent.find("#browse_result").hide();
    mainContent.find("#main_browse_result_content").hide("slide", {}, 300,function(){
      mainContent.find("#browse_breadcrumb").html(smallSpinnerImg());
      mainContent.find("#browse_artifact").show();
      mainContent.find("#browse_artifact").html(mediumSpinnerImg());
      mainContent.find("#main_browse_result_content").show();
      var metadataUrl="restServices/archivaServices/browseService/projectVersionMetadata/"+encodeURIComponent(groupId)+"/"+encodeURIComponent(artifactId);
      var versionsListUrl="restServices/archivaServices/browseService/versionsList/"+encodeURIComponent(groupId)+"/"+encodeURIComponent(artifactId);
      var selectedRepo=getSelectedBrowsingRepository();
      if (selectedRepo){
        metadataUrl+="?repositoryId="+encodeURIComponent(selectedRepo);
        versionsListUrl+="?repositoryId="+encodeURIComponent(selectedRepo);
      }
      $.ajax(metadataUrl, {
        type: "GET",
        dataType: 'json',
        success: function(data) {
          artifactDetailViewModel.projectVersionMetadata=mapProjectVersionMetadata(data);
          $.ajax(versionsListUrl, {
            type: "GET",
            dataType: 'json',
            success: function(data) {
              artifactDetailViewModel.versions=mapVersionsList(data);
              ko.applyBindings(artifactDetailViewModel,mainContent.find("#browse_artifact").get(0));
              ko.applyBindings(artifactDetailViewModel,mainContent.find("#browse_breadcrumb").get(0));

             }
          });
        }
      });
    });
  }

  browseRoot=function(){
    displayGroupDetail(null,null,"restServices/archivaServices/browseService/rootGroups");
  }

  /**
   * call from menu entry to display root level
   */
  displayBrowse=function(freshView){
    screenChange()
    var mainContent = $("#main-content");
    if(freshView){
      mainContent.html($("#browse-tmpl" ).tmpl());
    }
    mainContent.find("#browse_result").html(mediumSpinnerImg());

    $.ajax("restServices/archivaServices/browseService/userRepositories", {
        type: "GET",
        dataType: 'json',
        success: function(data) {
          mainContent.find("#selected_repository" ).html($("#selected_repository_tmpl" ).tmpl({repositories:data}));
          var url="restServices/archivaServices/browseService/rootGroups"
          $.ajax(url, {
              type: "GET",
              dataType: 'json',
              success: function(data) {
                var browseResultEntries = mapbrowseResultEntries(data);
                $.log("size:"+browseResultEntries.length);
                var browseViewModel = new BrowseViewModel(browseResultEntries,null,null);
                ko.applyBindings(browseViewModel,mainContent.find("#browse_breadcrumb").get(0));
                ko.applyBindings(browseViewModel,mainContent.find("#browse_result").get(0));
                enableAutocompleBrowse();
              }
          });
        }
    });

  }

  changeBrowseRepository=function(){
    var selectedRepository=getSelectedBrowsingRepository();
    displayGroupDetail(null,null,"restServices/archivaServices/browseService/rootGroups?repositoryId="+encodeURIComponent(selectedRepository));
  }

  getSelectedBrowsingRepository=function(){
    var selectedOption=$("#main-content #select_browse_repository option:selected" );
    if (selectedOption.length>0){
      var repoId=selectedOption.val();
      return repoId;
    }
    return null;
  }

  enableAutocompleBrowse=function(groupId){
    // browse-autocomplete
    var url="restServices/archivaServices/browseService/rootGroups";
    if (groupId){
      url="restServices/archivaServices/browseService/browseGroupId/"+encodeURIComponent(groupId);
    }
    var selectedRepo=getSelectedBrowsingRepository();
    if (selectedRepo){
      url+="?repositoryId="+selectedRepo;
    }
    $( "#main-content #browse-autocomplete" ).autocomplete({
      minLength: 2,
			source: function(request, response){
        var query = "";
        if (request.term.indexOf('.')<0&&!groupId){
          // try with rootGroups then filtered
          $.get(url,
             function(data) {
               var browseResultEntries = mapbrowseResultEntries(data);

               var filetered = [];
               for(var i=0;i<browseResultEntries.length;i++){
                 if (browseResultEntries[i].name.startsWith(request.term)){
                   if (groupId){
                     $.log("groupId:"+groupId+",browseResultEntry.name:"+browseResultEntries[i].name);
                     if (browseResultEntries[i].name.startsWith(groupId)) {
                       filetered.push(browseResultEntries[i]);
                     }

                   } else {
                     filetered.push(browseResultEntries[i]);
                   }
                 }
               }
               response(filetered);

             }
          );
          return;
        }
        var dotEnd=request.term.endsWith(".");
        // org.apache. requets with org.apache
        // org.apa request with org before last dot and filter response with startsWith
          if (request.term.indexOf(".")>=0){
            if (dotEnd){
              query= groupId?groupId+'.'+request.term.substring(0, request.term.length-1):request.term.substring(0, request.term.length-1);
            } else {
              // substring before last
              query=groupId?groupId+'.'+request.term.substringBeforeLast("."):request.term.substringBeforeLast(".");
            }
          } else {
            query=groupId?groupId:request.term;
          }
        var browseUrl="restServices/archivaServices/browseService/browseGroupId/"+encodeURIComponent(query);
        var selectedRepo=getSelectedBrowsingRepository();
        if (selectedRepo){
          browseUrl+="?repositoryId="+selectedRepo;
        }
        $.get(browseUrl,
           function(data) {
             var browseResultEntries = mapbrowseResultEntries(data);
             if (dotEnd){
              response(browseResultEntries);
             } else {
               var filetered = [];
               for(var i=0;i<browseResultEntries.length;i++){
                 if (groupId){
                   if (browseResultEntries[i].name.startsWith(groupId+'.'+request.term)){
                     filetered.push(browseResultEntries[i]);
                   }
                 } else {
                   if (browseResultEntries[i].name.startsWith(request.term)){
                     filetered.push(browseResultEntries[i]);
                   }
                 }
               }
               response(filetered);
             }
           }
        );
      },
      select: function( event, ui ) {
        $.log("ui.item.label:"+ui.item.name);
        if (ui.item.project){
          // value org.apache.maven/maven-archiver
          // split this org.apache.maven and maven-archiver
          var id=ui.item.name;
          var values = id.split(".");
          var groupId="";
          for (var i = 0;i<values.length-1;i++){
            groupId+=values[i];
            if (i<values.length-2)groupId+=".";
          }
          var artifactId=values[values.length-1];
          displayArtifactDetail(groupId,artifactId,self);
        } else {
          displayBrowseGroupIdFromAutoComplete(ui.item.name);
        }
        return false;
      }
		}).data( "autocomplete" )._renderItem = function( ul, item ) {
					return $( "<li></li>" )
						.data( "item.autocomplete", item )
						.append( groupId ? "<a>" +  item.name.substring(groupId.length+1, item.name.length) + "</a>": "<a>" + item.name + "</a>" )
						.appendTo( ul );
				};
  }

  /**
   *
   * @param groupId
   */
  displayBrowseGroupIdFromAutoComplete=function(groupId){
    clearUserMessages();
    var mainContent = $("#main-content");
    mainContent.find("#browse_result").html(mediumSpinnerImg());
    var parentBrowseViewModel=new BrowseViewModel(null,null,null);
    displayGroupDetail(groupId,parentBrowseViewModel,null);
  }

  /**
   * called if browser url contains queryParam browse=groupId
   * @param groupId
   */
  displayBrowseGroupId=function(groupId){
    clearUserMessages();
    $.log("displayBrowseGroupId:"+groupId);
    var mainContent = $("#main-content");
    mainContent.html($("#browse-tmpl" ).tmpl());
    mainContent.find("#browse_result").html(mediumSpinnerImg());
    var parentBrowseViewModel=new BrowseViewModel(null,null,null);
    displayGroupDetail(groupId,parentBrowseViewModel,null);
  }

  displayBrowseArtifactDetail=function(groupId, artifactId){
    displayBrowseGroupId(groupId);
    displayArtifactDetail(groupId,artifactId,null,null);
  }

  mapbrowseResultEntries=function(data){
    $.log("mapbrowseResultEntries");
    if (data.browseResultEntries) {
      return $.isArray(data.browseResultEntries) ?
         $.map(data.browseResultEntries,function(item){
           return new BrowseResultEntry(item.name, item.project);
         } ).sort(): [data.browseResultEntries];
    }
    return [];
  }

  BrowseResultEntry=function(name,project){
    this.name=name;
    this.project=project;
  }

  BreadCrumbEntry=function(groupId,displayValue){
    this.groupId=groupId;
    this.displayValue=displayValue;
    this.artifactId=null;
    this.artifact=false;
  }
  mapVersionsList=function(data){
    if (data){
      if (data.versions){
        return $.isArray(data.versions)? $.map(data.versions,function(item){return item})
            :[data.versions];
      }

    }
    return [];
  }
  mapProjectVersionMetadata=function(data){
    if (data){
      var projectVersionMetadata =
          new ProjectVersionMetadata(data.id,data.url,
                                    data.name,data.description,
                                    null,null,null,null,null,null,null,data.incomplete);

      if (data.organization){
        projectVersionMetadata.organization=new Organization(data.organization.name,data.organization.url);
      }
      if (data.issueManagement){
        projectVersionMetadata.issueManagement=
            new IssueManagement(data.issueManagement.system,data.issueManagement.url);
      }
      if (data.scm){
        projectVersionMetadata.scm=
            new Scm(data.scm.connection,data.scm.developerConnection,data.scm.url);
      }
      if (data.ciManagement){
        projectVersionMetadata.ciManagement=new CiManagement(data.ciManagement.system,data.ciManagement.url);
      }
      if (data.licenses){
        var licenses =
        $.isArray(data.licenses) ? $.map(data.licenses,function(item){
              return new License(item.name,item.url);
          }):[data.licenses];
        projectVersionMetadata.licenses=licenses;
      }
      if (data.mailingLists){
        var mailingLists =
        $.isArray(data.mailingLists) ? $.map(data.mailingLists,function(item){
              return new MailingList(item.mainArchiveUrl,item.otherArchives,item.name,item.postAddress,
                                     item.subscribeAddress,item.unsubscribeAddress);
          }):[data.mailingLists];
        projectVersionMetadata.mailingLists=mailingLists;
      }
      if (data.dependencies){
        var dependencies =
        $.isArray(data.dependencies) ? $.map(data.dependencies,function(item){
              return new Dependency(item.classifier,item.optional,item.scope,item.systemPath,item.type,
                                    item.artifactId,item.groupId,item.version);
          }):[data.dependencies];
        projectVersionMetadata.dependencies=dependencies;
      }
      // maven facet currently only for packaging
      if(data.facetList){
        if( $.isArray(data.facetList)){
          for (var i=0;i<data.facetList.length;i++){
            if(data.facetList[i].facetId=='org.apache.archiva.metadata.repository.storage.maven2.project'){
              projectVersionMetadata.mavenFacet=new MavenFacet(data.facetList[i].packaging);
            }
          }
        } else {
          if(data.facetList.facetId=='org.apache.archiva.metadata.repository.storage.maven2.project'){
            projectVersionMetadata.mavenFacet=new MavenFacet(data.facetList.packaging);
          }
        }
      }
      return projectVersionMetadata;
    }
    return null;
  }

  MavenFacet=function(packaging){
    this.packaging=packaging;
  }

  ProjectVersionMetadata=function(id,url,name,description,organization,issueManagement,scm,ciManagement,licenses,
                                  mailingLists,dependencies,incomplete){
    // private String id;
    this.id=id;

    // private String url;
    this.url=url

    //private String name;
    this.name=name;

    //private String description;
    this.description=description;

    //private Organization organization;
    this.organization=organization;

    //private IssueManagement issueManagement;
    this.issueManagement=issueManagement;

    //private Scm scm;
    this.scm=scm;

    //private CiManagement ciManagement;
    this.ciManagement=ciManagement;

    //private List<License> licenses = new ArrayList<License>();
    this.licenses=licenses;

    //private List<MailingList> mailingLists = new ArrayList<MailingList>();
    this.mailingLists=mailingLists;

    //private List<Dependency> dependencies = new ArrayList<Dependency>();
    this.dependencies=dependencies;

    //private boolean incomplete;
    this.incomplete=incomplete;

    this.mavenFacet=null;

  }

  Organization=function(name,url){
    //private String name;
    this.name=name;

    //private String url;
    this.url=url;
  }

  IssueManagement=function(system,url) {
    //private String system;
    this.system=system;

    //private String url;
    this.url=url;
  }

  Scm=function(connection,developerConnection,url) {
    //private String connection;
    this.connection=connection;

    //private String developerConnection;
    this.developerConnection=developerConnection;

    //private String url;
    this.url=url;
  }

  CiManagement=function(system,url) {
    //private String system;
    this.system=system;

    //private String url;
    this.url=url;
  }

  License=function(name,url){
    this.name=name;
    this.url=url;
  }

  MailingList=function(mainArchiveUrl,otherArchives,name,postAddress,subscribeAddress,unsubscribeAddress){
    //private String mainArchiveUrl;
    this.mainArchiveUrl=mainArchiveUrl;

    //private List<String> otherArchives;
    this.otherArchives=otherArchives;

    //private String name;
    this.name=name;

    //private String postAddress;
    this.postAddress=postAddress;

    //private String subscribeAddress;
    this.subscribeAddress=subscribeAddress;

    //private String unsubscribeAddress;
    this.unsubscribeAddress=unsubscribeAddress;
  }

  Dependency=function(classifier,optional,scope,systemPath,type,artifactId,groupId,version){
    //private String classifier;
    this.classifier=classifier;

    //private boolean optional;
    this.optional=optional;

    //private String scope;
    this.scope=scope;

    //private String systemPath;
    this.systemPath=systemPath;

    //private String type;
    this.type=type;

    //private String artifactId;
    this.artifactId=artifactId;

    //private String groupId;
    this.groupId=groupId;

    //private String version;
    this.version=version;

  }

  //-----------------------------------------
  // search part
  //-----------------------------------------
  Artifact=function(context,url,groupId,artifactId,repositoryId,version,prefix,goals,bundleVersion,bundleSymbolicName,
                    bundleExportPackage,bundleExportService,bundleDescription,bundleName,bundleLicense,bundleDocUrl,
                    bundleImportPackage,bundleRequireBundle,classifier,packaging,fileExtension){
    //private String context;
    this.context=context;

    //private String url;
    this.url=url;

    //private String groupId;
    this.groupId=groupId;

    //private String artifactId;
    this.artifactId=artifactId;

    //private String repositoryId;
    this.repositoryId=repositoryId;

    //private String version;
    this.version=version;

    //Plugin goal prefix (only if packaging is "maven-plugin")
    //private String prefix;
    this.prefix=prefix;

    //Plugin goals (only if packaging is "maven-plugin")
    //private List<String> goals;
    this.goals=goals;

    //private String bundleVersion;
    this.bundleVersion=bundleVersion;

    // contains osgi metadata Bundle-SymbolicName if available
    //private String bundleSymbolicName;
    this.bundleSymbolicName=bundleSymbolicName;

    //contains osgi metadata Export-Package if available
    //private String bundleExportPackage;
    this.bundleExportPackage=bundleExportPackage;

    //contains osgi metadata Export-Service if available
    //private String bundleExportService;
    this.bundleExportService=bundleExportService;

    ///contains osgi metadata Bundle-Description if available
    //private String bundleDescription;
    this.bundleDescription=bundleDescription;

    // contains osgi metadata Bundle-Name if available
    //private String bundleName;
    this.bundleName=bundleName;

    //contains osgi metadata Bundle-License if available
    //private String bundleLicense;
    this.bundleLicense=bundleLicense;

    ///contains osgi metadata Bundle-DocURL if available
    //private String bundleDocUrl;
    this.bundleDocUrl=bundleDocUrl;

    // contains osgi metadata Import-Package if available
    //private String bundleImportPackage;
    this.bundleImportPackage=bundleImportPackage;

    ///contains osgi metadata Require-Bundle if available
    //private String bundleRequireBundle;
    this.bundleRequireBundle=bundleRequireBundle;

    //private String classifier;
    this.classifier=classifier;

    //private String packaging;
    this.packaging=packaging;

    //file extension of the artifact
    //private String fileExtension;
    this.fileExtension=fileExtension;
  }

  mapArtifacts=function(data){
    if (data){
      return $.isArray(data )? $.map(data,function(item){return mapArtifact(item)}) : [data];
    }
    return [];
  }

  mapArtifact=function(data){
    return new Artifact(data.context,data.url,data.groupId,data.artifactId,data.repositoryId,data.version,data.prefix,
                        data.goals,data.bundleVersion,data.bundleSymbolicName,
                        data.bundleExportPackage,data.bundleExportService,data.bundleDescription,data.bundleName,
                        data.bundleLicense,data.bundleDocUrl,
                        data.bundleImportPackage,data.bundleRequireBundle,data.classifier,data.packaging,data.fileExtension);
  }

  SearchRequest=function(){

    this.queryTerms=ko.observable();

    //private String groupId;
    this.groupId=ko.observable();

    //private String artifactId;
    this.artifactId=ko.observable();

    //private String version;
    this.version=ko.observable();

    //private String packaging;
    this.packaging=ko.observable();

    //private String className;
    this.className=ko.observable();

    //private List<String> repositories = new ArrayList<String>();
    this.repositories=ko.observableArray([]);

    //private String bundleVersion;
    this.bundleVersion=ko.observable();

    //private String bundleSymbolicName;
    this.bundleSymbolicName=ko.observable();

    //private String bundleExportPackage;
    this.bundleExportPackage=ko.observable();

    //private String bundleExportService;
    this.bundleExportService=ko.observable();

    this.bundleImportPackage=ko.observable();

    this.bundleRequireBundle=ko.observable();

    //private String classifier;
    this.classifier=ko.observable();

    //private boolean includePomArtifacts = false;
    this.includePomArtifacts=ko.observable(false);

    this.classifier=ko.observable();
  }

  applyAutocompleteOnHeader=function(property,resultViewModel){
    $( "#main-content #search-filter-auto-"+property ).autocomplete({
      minLength: 0,
			source: function(request, response){
        var founds=[];
        $(resultViewModel.artifacts()).each(function(idx,artifact){
          if(artifact[property] && artifact[property].startsWith(request.term)){
            founds.push(artifact[property]);
          }
        });
        response(unifyArray(founds,true));
      },
      select: function( event, ui ) {
        $.log("property:"+property+','+ui.item.value);
        var artifacts=[];
        $(resultViewModel.artifacts()).each(function(idx,artifact){
          if(artifact[property] && artifact[property].startsWith(ui.item.value)){
            artifacts.push(artifact);
          }
        });
        $.log("property:"+property+','+ui.item.value+",size:"+artifacts.length);
        resultViewModel.artifacts(artifacts);
        return false;
      }
    });
  }

  ResultViewModel=function(artifacts){
    var self=this;
    this.originalArtifacts=artifacts;
    this.artifacts=ko.observableArray(artifacts);
    this.gridViewModel = new ko.simpleGrid.viewModel({
      data: self.artifacts,
      columns: [
        {
          headerText: $.i18n.prop('search.artifact.results.groupId'),
          rowText: "groupId",
          id: "groupId"
        },
        {
          headerText: $.i18n.prop('search.artifact.results.artifactId'),
          rowText: "artifactId",
          id: "artifactId"
        },
        {
          headerText: $.i18n.prop('search.artifact.results.version'),
          rowText: "version",
          id: "version"
        },
        {
          headerText: $.i18n.prop('search.artifact.results.classifier'),
          rowText: "classifier",
          id: "classifier"
        }
      ],
      pageSize: 10,
      gridUpdateCallBack: function(){
        applyAutocompleteOnHeader('groupId',self);
        applyAutocompleteOnHeader('artifactId',self);
        applyAutocompleteOnHeader('version',self);
        applyAutocompleteOnHeader('classifier',self);
      }
    });
  }


  SearchViewModel=function(){
    var self=this;
    this.searchRequest=ko.observable(new SearchRequest());
    this.observableRepoIds=ko.observableArray([]);
    this.selectedRepoIds=[];
    this.resultViewModel=new ResultViewModel([]);
    basicSearch=function(){
      var queryTerm=this.searchRequest().queryTerms();
      if ($.trim(queryTerm).length<1){
        var errorList=[{
          message: $.i18n.prop("search.artifact.search.form.terms.empty"),
    		  element: $("#main-content #search-basic-form #search-terms" ).get(0)
        }];
        customShowError("#main-content #search-basic-form", null, null, errorList);
        return;
      } else {
        // cleanup previours error message
        customShowError("#main-content #search-basic-form", null, null, []);
      }
      self.search("restServices/archivaServices/searchService/quickSearchWithRepositories");
    }

    /**
     * use from autocomplete search
     */
    this.externalAdvancedSearch=function(){
      this.search("restServices/archivaServices/searchService/searchArtifacts");
    }
    advancedSearch=function(){
      self.search("restServices/archivaServices/searchService/searchArtifacts");
    }
    removeFilter=function(){
      self.resultViewModel.artifacts(self.resultViewModel.originalArtifacts);
    }
    this.search=function(url){

      var mainContent=$("#main-content");

      var searchResultsGrid=mainContent.find("#search-results #search-results-grid" );
      mainContent.find("#btn-basic-search" ).button("loading");
      mainContent.find("#btn-advanced-search" ).button("loading");
      $("#user-messages").html(mediumSpinnerImg());


      self.selectedRepoIds=[];
      mainContent.find("#search-basic-repositories" )
          .find(".chzn-choices li span").each(function(i,span){
                      self.selectedRepoIds.push($(span).html());
                      }
                    );

      this.searchRequest().repositories=this.selectedRepoIds;
      $.ajax(url,
        {
          type: "POST",
          data: ko.toJSON(this.searchRequest),
          contentType: 'application/json',
          dataType: 'json',
          success: function(data) {
            clearUserMessages();
            var artifacts=mapArtifacts(data);
            if (artifacts.length<1){
              displayWarningMessage( $.i18n.prop("search.artifact.noresults"));
              return;
            } else {
              self.resultViewModel.originalArtifacts=artifacts;
              self.resultViewModel.artifacts(artifacts);
              if (!searchResultsGrid.attr("data-bind")){
                searchResultsGrid.attr("data-bind",
                                 "simpleGrid: gridViewModel,simpleGridTemplate:'search-results-view-grid-tmpl',pageLinksId:'search-results-view-grid-pagination'");
                ko.applyBindings(self.resultViewModel,searchResultsGrid.get(0));
                ko.applyBindings(self,mainContent.find("#remove-filter-id" ).get(0));
                mainContent.find("#search-result-number-div").attr("data-bind",
                  "template:{name:'search-result-number-div-tmpl'}");
                ko.applyBindings(self,mainContent.find("#search-result-number-div" ).get(0));
              }

              activateSearchResultsTab();
            }
          },
          error: function(data) {
            var res = $.parseJSON(data.responseText);
            displayRestError(res);
          },
          complete:function() {
            mainContent.find("##btn-basic-search" ).button("reset");
            mainContent.find("#btn-advanced-search" ).button("reset");
            removeMediumSpinnerImg("#user-messages");
          }
        }
      );
    }

  }

  activateSearchResultsTab=function(){
    var mainContent=$("#main-content");
    mainContent.find("#search-form-collapse").removeClass("active");
    mainContent.find("#search-results").addClass("active");

    mainContent.find("#search-form-collapse-li").removeClass("active");
    mainContent.find("#search-results-li" ).addClass("active");

  }

  displaySearch=function(successCallbackFn){
    clearUserMessages();
    var mainContent=$("#main-content");
    mainContent.html(mediumSpinnerImg());
    $.ajax("restServices/archivaServices/searchService/observableRepoIds", {
        type: "GET",
        dataType: 'json',
        success: function(data) {
          mainContent.html($("#search-artifacts-div-tmpl" ).tmpl());
          var searchViewModel=new SearchViewModel();
          var repos=mapStringList(data);
          $.log("repos:"+repos);
          searchViewModel.observableRepoIds(repos);
          ko.applyBindings(searchViewModel,mainContent.find("#search-artifacts-div").get(0));
          mainContent.find("#search-basic-repostories-select" ).chosen();
          if (successCallbackFn && $.isFunction(successCallbackFn)) successCallbackFn();
        }
    });

  }



});