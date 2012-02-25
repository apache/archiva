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
      var splitted = self.groupId.split(".");
      var breadCrumbEntries=[];
      var curGroupId="";
      for (var i=0;i<splitted.length;i++){
        curGroupId+=splitted[i];
        breadCrumbEntries.push(new BreadCrumbEntry(curGroupId,splitted[i]));
        curGroupId+="."
      }
      return breadCrumbEntries;
    }

    displayEntry=function(value){
      if (self.groupId){
        return value.substr(self.groupId.length+1,value.length-self.groupId.length);
      }
      return value;
    }
  }



  displayGroupDetail=function(groupId,parentBrowseViewModel,restUrl){
    var mainContent = $("#main-content");
    var browseResult=mainContent.find("#browse_result");
    var browseBreadCrumb=mainContent.find("#browse_breadcrumb");
    mainContent.find("#main_browse_result_content").hide( "slide", {}, 300,
        function(){
          browseResult.html(mediumSpinnerImg());
          browseBreadCrumb.html(smallSpinnerImg());
          mainContent.find("#main_browse_result_content" ).show();
          var url = restUrl ? restUrl : "restServices/archivaServices/browseService/browseGroupId/"+encodeURIComponent(groupId);
          $.ajax(url, {
            type: "GET",
            dataType: 'json',
            success: function(data) {
              var browseResultEntries = mapbrowseResultEntries(data);
              var browseViewModel = new BrowseViewModel(browseResultEntries,parentBrowseViewModel,groupId);
              ko.applyBindings(browseViewModel,mainContent.get(0));
            }
         });
        }
    );
  }

  ArtifactDetailViewModel=function(){
    this.versions=[];
    this.projectVersionMetadata=null;

  }

  displayArtifactDetail=function(groupId,artifactId,parentBrowseViewModel,restUrl){
    $.log("displayArtifactDetail:"+groupId+":"+artifactId);
    var artifactDetailViewModel=new ArtifactDetailViewModel();
    $.ajax("restServices/archivaServices/browseService/projectVersionMetadata/"+groupId+"/"+artifactId, {
      type: "GET",
      dataType: 'json',
      success: function(data) {
        artifactDetailViewModel.projectVersionMetadata=mapProjectVersionMetadata(data);
        $.ajax("restServices/archivaServices/browseService/versionsList/"+groupId+"/"+artifactId, {
          type: "GET",
          dataType: 'json',
          success: function(data) {
            artifactDetailViewModel.versions=mapVersionsList(data);
          }
        });

      }
    });
  }

  browseRoot=function(){
    displayGroupDetail(null,null,"restServices/archivaServices/browseService/rootGroups");
  }

  /**
   * call from menu entry to display root level
   */
  displayBrowse=function(){
    screenChange()
    var mainContent = $("#main-content");
    mainContent.html($("#browse-tmpl" ).tmpl());
    mainContent.find("#browse_result").html(mediumSpinnerImg());
    $.ajax("restServices/archivaServices/browseService/rootGroups", {
        type: "GET",
        dataType: 'json',
        success: function(data) {
          var browseResultEntries = mapbrowseResultEntries(data);
          $.log("size:"+browseResultEntries.length);
          var browseViewModel = new BrowseViewModel(browseResultEntries,null,null);
          ko.applyBindings(browseViewModel,mainContent.get(0));
        }
    });
  }

  /**
   * called if browser url contains queryParam browse=groupId
   * @param groupId
   */
  displayBrowseGroupId=function(groupId){
    clearUserMessages();
    var mainContent = $("#main-content");
    mainContent.html($("#browse-tmpl" ).tmpl());
    mainContent.find("#browse_result").html(mediumSpinnerImg());
    var parentBrowseViewModel=new BrowseViewModel(null,null,null);
    displayGroupDetail(groupId,parentBrowseViewModel,null)
  }


  mapbrowseResultEntries=function(data){
    if (data.browseResult && data.browseResult.browseResultEntries) {
      return $.isArray(data.browseResult.browseResultEntries) ?
         $.map(data.browseResult.browseResultEntries,function(item){
           return new BrowseResultEntry(item.name, item.project);
         } ).sort(): [data.browseResult.browseResultEntries];
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
  }
  mapVersionsList=function(data){
    if (data.versionsList){
      if (data.versionsList.versions){
        return $.isArray(data.versionsList.versions)? $.map(data.versionsList.versions,function(item){return item})
            :[data.versionsList.versions];
      }

    }
    return [];
  }
  mapProjectVersionMetadata=function(data){
    if (data.projectVersionMetadata){
      var projectVersionMetadata = new ProjectVersionMetadata(data.id,data.url,data.name,data.description,null,null,null,null,null,
                                        null,null,data.incomplete);
      if (data.organization){
        projectVersionMetadata.organization=new Organization(data.organization.name,data.organization.url);
      }
      if (data.issueManagement){
        projectVersionMetadata.issueManagement=new IssueManagement(data.issueManagement.system,data.issueManagement.url);
      }
      if (data.scm){
        projectVersionMetadata.scm=new Scm(data.scm.connection,data.scm.developerConnection,data.scm.url);
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
      return projectVersionMetadata;
    }
    return null;
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

  displaySearch=function(){
    $("#main-content" ).html("coming soon :-)");
  }

});