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
      $.log("displayProjectEntry:"+id);
      var url = "restServices/archivaServices/browseService/browseGroupId?g=org.apache.maven&a=maven-archiver";

      $.ajax(url, {
        type: "GET",
        dataType: 'json',
        success: function(data) {

        }
     });
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

  browseRoot=function(){
    displayGroupDetail(null,null,"restServices/archivaServices/browseService/rootGroups");
  }

  /**
   * call from menu entry to display root level
   */
  displayBrowse=function(){
    clearUserMessages();
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

  //-----------------------------------------
  // search part
  //-----------------------------------------

  displaySearch=function(){
    $("#main-content" ).html("coming soon :-)");
  }

});