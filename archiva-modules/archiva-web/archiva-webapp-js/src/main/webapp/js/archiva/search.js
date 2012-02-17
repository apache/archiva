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

  BrowseViewModel=function(browseResultEntries,parentGroupdId){
    this.browseResultEntries=browseResultEntries;
    this.parentGroupdId=parentGroupdId;
    displayGroupId=function(groupId){
      displayGroupDetail(groupId,"..");
    }

  }



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
          var browseViewModel = new BrowseViewModel(browseResultEntries);

          ko.applyBindings(browseViewModel,mainContent.get(0));
        }
    });
  }

  displayGroupDetail=function(groupId,parentGroupdId){
    var mainContent = $("#main-content");
    var browseResult=mainContent.find("#browse_result");
    browseResult.hide( "slide", {}, 500,
      function(){
        browseResult.html(mediumSpinnerImg());
        browseResult.show();
        var url = "restServices/archivaServices/browseService/browseGroupId/"+encodeURIComponent(groupId);
        $.ajax(url, {
          type: "GET",
          dataType: 'json',
          success: function(data) {
            var browseResultEntries = mapbrowseResultEntries(data);
            var browseViewModel = new BrowseViewModel(browseResultEntries,parentGroupdId);

            ko.applyBindings(browseViewModel,mainContent.get(0));
          }
       });
      }
    );

  }

  displaySearch=function(){
    $("#main-content" ).html("coming soon :-)");
  }

  mapbrowseResultEntries=function(data){
    if (data.browseResult && data.browseResult.browseResultEntries) {
      return $.isArray(data.browseResult.browseResultEntries) ?
         $.map(data.browseResult.browseResultEntries,function(item){
           return new BrowseResultEntry(item.name, item.project);
         }): [data.browseResult.browseResultEntries];
    }
    return [];
  }

  BrowseResultEntry=function(name,project){
    this.name=name;
    this.project=project;
  }
});