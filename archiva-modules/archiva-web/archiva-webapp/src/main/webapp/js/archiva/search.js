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
define("archiva.search",["jquery","jquery.ui","i18n","jquery.tmpl","select2","knockout","knockout.simpleGrid","jqueryFileTree"
  ,"prettify", "d3", "typeahead","hogan"]
, function(jquery,jqueryui,i18n,jqueryTmpl,select2,ko,koSimpleGrid,jqueryFileTree,prettify,d3,typeahead,hogan) {

  //-----------------------------------------
  // browse part
  //-----------------------------------------

  BrowseViewModel=function(browseResultEntries,parentBrowseViewModel,groupId,repositoryId,feedsUrl){
    $.log("BrowseViewModel:"+repositoryId);
    var self=this;
    this.browseResultEntries=browseResultEntries;
    this.parentBrowseViewModel=parentBrowseViewModel;
    this.groupId=groupId;
    this.repositoryId=repositoryId;
    this.feedsUrl=feedsUrl;
    displayGroupId=function(groupId){
      $.log("BrowseViewModel#displayGroupId"+groupId+",self.repositoryId:"+self.repositoryId);
      if(self.repositoryId){
        window.sammyArchivaApplication.setLocation("#browse~"+self.repositoryId+"/"+groupId);
      } else {
        window.sammyArchivaApplication.setLocation("#browse/"+groupId);
      }
    }
    displayParentGroupId=function(){
      $.log("called displayParentGroupId groupId:"+self.parentBrowseViewModel.groupId);

      // if null parent is root level
      if (self.parentBrowseViewModel.groupId && self.parentBrowseViewModel.groupId.indexOf(".")>=1){
        // remove last part of the groupId
        window.sammyArchivaApplication.setLocation("#browse/"+groupId.substringBeforeLast('.'));
        //displayGroupDetail(self.parentBrowseViewModel.groupId,self.parentBrowseViewModel);
      } else {
        browseRoot();
      }
    }

    breadCrumbEntries=function(){
      // root level ?
      if (!self.parentBrowseViewModel){
        return [];
      }
      return calculateBreadCrumbEntries(self.groupId);
    }

    displayProjectEntry=function(id){

      // value org.apache.maven/maven-archiver
      // artifactId can contains .
      // value org.apache.aries/org.apache.aries.util
      // split this org.apache.maven and maven-archiver
      var artifactId = id.substring((self.groupId+'.').length,id.length);//.split(".");
      var selectedRepo=getSelectedBrowsingRepository();

      var location ="#artifact";
      if (selectedRepo){
        location+="~"+selectedRepo;
      }
      location+="/"+self.groupId+"/"+artifactId;

      window.sammyArchivaApplication.setLocation(location);

    }

    displayEntry=function(value){
      if (self.groupId){
        return value.substr(self.groupId.length+1,value.length-self.groupId.length);
      }
      return value;
    }

    deleteKarma=function(){
      return hasKarma('archiva-delete-artifact');
    }

    deleteProject=function(groupId,projectId){
      $.log("deleteProject:"+groupId+"/"+projectId);

      var repoId=getSelectedBrowsingRepository();
      if(!repoId){
        var escapedGroupId=escapeDot(groupId );
        var selected = $("#main-content" ).find("#delete-"+escapedGroupId );
        selected.attr("data-content",$.i18n.prop('projectId.delete.missing.repoId'))
        selected.popover({
          html:true,
          template: '<div class="popover"><div class="arrow"></div><div class="popover-inner"><div class="popover-content"><p></p></div></div></div>',
          placement:'top',
          trigger:'manual'});
        selected.popover('show');
        selected.mouseover(function(){
          selected.popover("destroy");
        });
        return;
      }


      var previousHash=getUrlHash();
      $.log("previousHash:"+previousHash);
      openDialogConfirm(function(){
        $("#dialog-confirm-modal-ok").button('loading');
        $.ajax({
          url:"restServices/archivaServices/repositoriesService/project/"+repoId+"/"+groupId+"/"+projectId,
          type:"DELETE",
          dataType:"json",
          success:function(data){
            window.sammyArchivaApplication.setLocation(previousHash);
            refreshContent();
            displaySuccessMessage( $.i18n.prop("projectId.deleted", groupId,projectId));
          },
          error:function(data){
            displayRestError(data,"user-messages");
          },
          complete:function(){
            $("#dialog-confirm-modal-ok").button('reset');
            closeDialogConfirm();
          }
        });
      }, $.i18n.prop('ok'),
          $.i18n.prop('cancel'),
          $.i18n.prop('projectId.delete.confirm.title'),
          $.i18n.prop('projectId.delete.confirm.save',groupId,projectId));
      }
    }

    deleteGroupId=function(groupId){

      var repoId=getSelectedBrowsingRepository();
      if(!repoId){
        var escapedGroupId=escapeDot(groupId );
        var selected = $("#main-content" ).find("#delete-"+escapedGroupId );
        selected.attr("data-content",$.i18n.prop('groupId.delete.missing.repoId'))
        selected.popover({
          html:true,
          template: '<div class="popover"><div class="arrow"></div><div class="popover-inner"><div class="popover-content"><p></p></div></div></div>',
          placement:'top',
          trigger:'manual'});
        selected.popover('show');
        selected.mouseover(function(){
          selected.popover("destroy");
        });
        return;
      }
      var previousHash=getUrlHash();
      $.log("previousHash:"+previousHash);
      openDialogConfirm(function(){
        $("#dialog-confirm-modal-ok").button('loading');
        $.ajax({
          url:"restServices/archivaServices/repositoriesService/deleteGroupId?groupId="+groupId+"&repositoryId="+repoId,
          type:"GET",
          dataType:"json",
          success:function(data){
            window.sammyArchivaApplication.setLocation(previousHash);
            refreshContent();
            displaySuccessMessage( $.i18n.prop("groupdId.deleted", groupId));
          },
          error:function(data){
            displayRestError(data,"user-messages");
          },
          complete:function(){
            $("#dialog-confirm-modal-ok").button('reset');
            closeDialogConfirm();
          }
        });
      }, $.i18n.prop('ok'),
          $.i18n.prop('cancel'),
          $.i18n.prop('groupId.delete.confirm.title'),
          $.i18n.prop('groupId.delete.confirm.save',groupId));

  }

  calculateBreadCrumbEntries=function(groupId){
    if (!groupId){
      return [];
    }
    var splitted = groupId.split(".");
    var breadCrumbEntries=[];
    var curGroupId="";
    for (var i=0;i<splitted.length;i++){
      curGroupId+=splitted[i];
      breadCrumbEntries.push(new BreadCrumbEntry(curGroupId,splitted[i]));
      curGroupId+="."
    }
    return breadCrumbEntries;
  }

  displayGroupDetail=function(groupId,parentBrowseViewModel,restUrl,repositoryId,feedsUrl){
    $.log("displayGroupDetail");
    var mainContent = $("#main-content");
    mainContent.find("#browse_artifact_detail").hide();
    var browseResult=mainContent.find("#browse_result");
    browseResult.show();
    mainContent.find("#browse_artifact" ).hide();
    var browseBreadCrumb=mainContent.find("#browse_breadcrumb");
    $.log("before slide");
    //mainContent.find("#main_browse_result_content").hide( "slide", {}, 300,
    mainContent.find("#main_browse_result_content").animate( {},300,"slide",
        function(){
          browseResult.html(mediumSpinnerImg());
          browseBreadCrumb.html(smallSpinnerImg());
          mainContent.find("#main_browse_result_content").show();
          var url = "";
          if (!restUrl) {
            url="restServices/archivaServices/browseService/browseGroupId/"+encodeURIComponent(groupId);
            var selectedRepo=getSelectedBrowsingRepository();
            if (selectedRepo){
              url+="?repositoryId="+selectedRepo;
            }
          } else {
            url=restUrl;
          }

          $.ajax(url, {
            type: "GET",
            dataType: 'json',
            success: function(data) {
              var browseResultEntries = mapBrowseResultEntries(data);
              var browseViewModel = new BrowseViewModel(browseResultEntries,parentBrowseViewModel,groupId,repositoryId,feedsUrl);
              ko.applyBindings(browseViewModel,browseBreadCrumb.get(0));
              ko.applyBindings(browseViewModel,browseResult.get(0));
              enableAutocompleBrowse(groupId,browseResultEntries);
            }
         });
        }
    );
  }

  ArtifactDetailViewModel=function(groupId,artifactId){
    var self=this;
    this.versions=ko.observableArray([]);
    this.projectVersionMetadata=null;
    this.groupId=groupId;
    this.artifactId=artifactId;
    breadCrumbEntries=function(){
      var entries = calculateBreadCrumbEntries(self.groupId);
      entries.push(new BreadCrumbEntry("foo",self.artifactId));
      return entries;
    }
    displayArtifactInfo=function(){
      if ($("#main-content").find("#artifact-info:visible" ).length>0) {
        $("#main-content").find("#artifact-info" ).hide();
      } else {
        $("#main-content").find("#artifact-info" ).show();
      }
    }

    deleteVersion=function(version){
      var repoId=getSelectedBrowsingRepository();
      if(!repoId){
        var escapedVersion=escapeDot(version);
        var selected = $("#main-content" ).find("#delete-"+escapedVersion );
        selected.attr("data-content",$.i18n.prop('version.delete.missing.repoId'))
        selected.popover({
          html:true,
          template: '<div class="popover"><div class="arrow"></div><div class="popover-inner"><div class="popover-content"><p></p></div></div></div>',
          placement:'top',
          trigger:'manual'});
        selected.popover('show');
        selected.mouseover(function(){
          selected.popover("destroy");
        });
        return;
      }

      clearUserMessages();
        var artifact = new Artifact(repoId,null,self.groupId,self.artifactId,repoId,version);
        openDialogConfirm(function(){
          var url = "restServices/archivaServices/repositoriesService/projectVersion/"+repoId;
          url+="/"+encodeURIComponent(self.groupId)+"/"+encodeURIComponent(self.artifactId);
          url+="/"+encodeURIComponent(version);
          $("#dialog-confirm-modal-ok").button('loading');
          $.ajax({
            url:url,
            type:"DELETE",
            success:function(data){
              self.versions.remove(version);
              refreshContent();
              displaySuccessMessage( $.i18n.prop('artifact.deleted'));
            },
            error:function(data){
              displayRestError( data,"user-messages");
            },
            complete:function(){
              $("#dialog-confirm-modal-ok").button('reset');
              closeDialogConfirm();
            }
          });
        }, $.i18n.prop('ok'),
            $.i18n.prop('cancel'),
            $.i18n.prop('artifact.delete.confirm.title'),
            $.i18n.prop('artifact.delete.confirm.save'));
    }

    displayArtifactVersionDetail=function(version){
      var selectedRepo=getSelectedBrowsingRepository();
      var location ="#artifact";
      if (selectedRepo){
        location+="~"+selectedRepo;
      }
      location+="/"+self.groupId+"/"+self.artifactId+"/"+version;
      window.sammyArchivaApplication.setLocation(location);
    }

    displayGroupId=function(groupId){
      var selectedRepo=getSelectedBrowsingRepository();
      var location ="#browse";
      if (selectedRepo){
        location+="~"+selectedRepo;
      }
      location+="/"+groupId;
      window.sammyArchivaApplication.setLocation(location);
    }

  }

  displayArtifactVersionDetailViewModel=function(groupId,artifactId,version){
    $.log("displayArtifactVersionDetailViewModel:"+groupId+":"+artifactId+":"+version);
    var artifactVersionDetailViewModel = new ArtifactVersionDetailViewModel (groupId,artifactId,version)
    artifactVersionDetailViewModel.display();
  }


  ArtifactVersionDetailViewModel=function(groupId,artifactId,version,repositoryId){
    var mainContent = $("#main-content");
    var self=this;
    this.groupId=groupId;
    this.artifactId=artifactId;
    this.version=version;
    this.projectVersionMetadata=null;
    this.entries=ko.observableArray([]);
    this.repositoryId=repositoryId;

    displayGroupId=function(groupId){
      var location ="#browse";
      if (self.repositoryId){
        location+="~"+self.repositoryId;
      }
      location+="/"+groupId;
      window.sammyArchivaApplication.setLocation(location);
    }

    displayParent=function(){
      var selectedRepo=getSelectedBrowsingRepository();
      var location ="#artifact";
      if (selectedRepo){
        location+="~"+selectedRepo;
      }
      location+="/"+self.projectVersionMetadata.mavenFacet.parent.groupId+"/"+self.projectVersionMetadata.mavenFacet.parent.artifactId;
      location+="/"+self.projectVersionMetadata.mavenFacet.parent.version;

      window.sammyArchivaApplication.setLocation(location);

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

    this.display=function(afterCallbackFn){
      $.log("display");
      mainContent.find("#browse_breadcrumb").animate({},300,"slide",function(){
        mainContent.find("#browse_artifact").animate({},300,"slide",function(){

          mainContent.find("#browse_artifact_detail").show();
          mainContent.find("#browse_artifact_detail").html(mediumSpinnerImg());
          mainContent.find("#browse_breadcrumb" ).show();
          mainContent.find("#browse_breadcrumb" ).html(mediumSpinnerImg());
          var metadataUrl="restServices/archivaServices/browseService/projectVersionMetadata/"+encodeURIComponent(groupId)+"/"+encodeURIComponent(artifactId);
          metadataUrl+="/"+encodeURIComponent(version);
          var selectedRepo=getSelectedBrowsingRepository();
          if (selectedRepo){
            metadataUrl+="?repositoryId="+encodeURIComponent(selectedRepo);
          }

          $.ajax(metadataUrl, {
            type: "GET",
            dataType: 'json',
            success: function(data) {
              self.projectVersionMetadata=mapProjectVersionMetadata(data);

              //pagination for dependencies
              self.projectVersionMetadata.dependencies=ko.observableArray(self.projectVersionMetadata.dependencies?self.projectVersionMetadata.dependencies:[]);
              self.gridViewModel = new ko.simpleGrid.viewModel({
                data: self.projectVersionMetadata.dependencies(),
                columns: [],
                pageSize: 7,
                gridUpdateCallBack: function(){
                  // nope
                }
              });

              ko.applyBindings(self,mainContent.find("#browse_artifact_detail" ).get(0));
              ko.applyBindings(self,mainContent.find("#browse_breadcrumb" ).get(0));
              mainContent.find("#browse-autocomplete" ).hide();
              mainContent.find("#browse-autocomplete-divider" ).hide();

              //calculate tree content
              var treeContentDiv=mainContent.find("#artifact-details-dependency-tree-content" );
              if( $.trim(treeContentDiv.html()).length<1){
                treeContentDiv.html(mediumSpinnerImg());
                var treeDependencyUrl="restServices/archivaServices/browseService/treeEntries/"+encodeURIComponent(groupId);
                treeDependencyUrl+="/"+encodeURIComponent(artifactId);
                treeDependencyUrl+="/"+encodeURIComponent(version);
                var selectedRepo=getSelectedBrowsingRepository();
                if (selectedRepo){
                  treeDependencyUrl+="?repositoryId="+encodeURIComponent(selectedRepo);
                }
                $.ajax(treeDependencyUrl, {
                  type: "GET",
                  dataType: 'json',
                  success: function(data) {
                    var treeEntries = mapTreeEntries(data);
                    treeContentDiv.html($("#dependency_tree_tmpl" ).tmpl({treeEntries: treeEntries}));
                  }
                });
              }


              mainContent.find("#artifact-details-tabs").on('show', function (e) {
                $.log("e.target:"+e.target);
                if ($(e.target).attr("data-target")=="#artifact-details-info-content") {
                  var location ="#artifact";
                  if (self.repositoryId){
                    location+="~"+self.repositoryId;
                  }
                  location+="/"+self.groupId+"/"+self.artifactId+"/"+self.version;

                  window.sammyArchivaApplication.setLocation(location);
                  return;
                }


                if ($(e.target).attr("data-target")=="#artifact-details-dependencies-content") {
                  var location ="#artifact-dependencies";
                  if (self.repositoryId){
                    location+="~"+self.repositoryId;
                  }
                  location+="/"+self.groupId+"/"+self.artifactId+"/"+self.version;

                  window.sammyArchivaApplication.setLocation(location);
                  return;
                }

                if ($(e.target).attr("data-target")=="#artifact-details-dependency-tree-content") {
                  var location ="#artifact-dependency-tree";
                  if (self.repositoryId){
                    location+="~"+self.repositoryId;
                  }
                  location+="/"+self.groupId+"/"+self.artifactId+"/"+self.version;

                  window.sammyArchivaApplication.setLocation(location);
                  return;
                }

                if ($(e.target).attr("data-target")=="#artifact-details-dependency-graph-content") {
                  var location ="#artifact-dependency-graph";
                  if (self.repositoryId) {
                    location+="~"+self.repositoryId;
                  }
                  location+="/"+self.groupId+"/"+self.artifactId+"/"+self.version;
                  displayGraph(treeDependencyUrl);
                  window.sammyArchivaApplication.setLocation(location);
                  return;
                }

                if ($(e.target).attr("data-target")=="#artifact-details-used-by-content") {
                  var location ="#artifact-used-by";
                  if (self.repositoryId){
                    location+="~"+self.repositoryId;
                  }
                  location+="/"+self.groupId+"/"+self.artifactId+"/"+self.version;

                  window.sammyArchivaApplication.setLocation(location);
                  return;
                }

                if ($(e.target).attr("href")=="#artifact-details-metadatas-content") {
                  var location ="#artifact-metadatas";
                  if (self.repositoryId){
                    location+="~"+self.repositoryId;
                  }
                  location+="/"+self.groupId+"/"+self.artifactId+"/"+self.version;

                  window.sammyArchivaApplication.setLocation(location);
                  return;
                }

                if ($(e.target).attr("href")=="#artifact-details-download-content") {
                  var location ="#artifact-details-download-content";
                  if (self.repositoryId){
                    location+="~"+self.repositoryId;
                  }
                  location+="/"+self.groupId+"/"+self.artifactId+"/"+self.version;

                  window.sammyArchivaApplication.setLocation(location);
                  return;
                }
                if ($(e.target).attr("href")=="#artifact-details-files-content") {
                  var location ="#artifact-details-files-content";
                  if (self.repositoryId){
                    location+="~"+self.repositoryId;
                  }
                  location+="/"+self.groupId+"/"+self.artifactId+"/"+self.version;

                  window.sammyArchivaApplication.setLocation(location);
                  return;
                }
                if ($(e.target).attr("href")=="#artifact-details-mailing-list-content") {
                  var location ="#artifact-mailing-list";
                  if (self.repositoryId){
                    location+="~"+self.repositoryId;
                  }
                  location+="/"+self.groupId+"/"+self.artifactId+"/"+self.version;

                  window.sammyArchivaApplication.setLocation(location);
                  return;
                }
              });
              if(afterCallbackFn){
                afterCallbackFn(self);
              }
            }

          });

        });
      });
    }

    displayGraphData=function(data) {

      var w = 960,
          h = 500,
          r =6,
          node,
          link,
          root;

      var onmousedown = false;

      var svg = d3.select("#artifact-details-dependency-graph-content")
          .append("svg:svg")
            .attr("width", w)
            .attr("height", h)
            .attr("pointer-events", "all")
          .append('svg:g')
            .call(d3.behavior.zoom().on("zoom", redraw))
          .append('svg:g');

      svg.append('svg:rect')
          .attr('width', w)
          .attr('height', h)
          .attr('fill', 'rgba(255, 255, 255, 0)')

      var force = d3.layout.force()
          .on("tick", tick)
          .linkDistance(100)
          .charge(-300)
          .size([w, h]);

      var nodes = flatten(data[0]);
      var links = d3.layout.tree().links(nodes);

      force.nodes(nodes)
          .links(links)
          .start();

      svg.append("svg:defs").selectAll("marker")
          .data(["suit"])
          .enter().append("svg:marker")
            .attr("id", String)
            .attr("viewBox", "0 -5 10 10")
            .attr("refX", 15)
            .attr("refY", -1.5)
            .attr("markerWidth", 6)
            .attr("markerHeight", 6)
            .attr("orient", "auto")
          .append("svg:path")
            .attr("d", "M0,-5L10,0L0,5");

      var path = svg.append("svg:g").selectAll("path")
          .data(force.links())
          .enter().append("svg:path")
          .attr("class", function (d) {
              return "link suit";
            })
          .attr("marker-end", function (d) {
              return "url(#suit)";
            });

      var circle = svg.append("svg:g").selectAll("circle")
          .data(force.nodes())
          .enter().append("svg:circle")
          .attr("r", r)
          .on("click", click)
          .on("mouseenter",onmouseover)
          .on("mouseleave", function (d, i) {
            setTimeout(function() {
              $("#plot_overlay").remove();
            }, 201)})
          .call(force.drag);

      force.drag()
          .on("dragstart", function() {
              $("#plot_overlay").remove();
              onmousedown = true;
            })
          .on("dragend", function() {
            $("#plot_overlay").remove();
            onmousedown = false;
          });

      var text = svg.append("svg:g")
          .selectAll("g")
          .data(force.nodes())
          .enter().append("svg:g");

      text.append("svg:text")
          .attr("x", 8)
          .attr("y", ".31em")
          .attr("class", "shadow")
          .text(function (d) {
              return d.artifact.artifactId;
            });

      text.append("svg:text")
          .attr("x", 8)
          .attr("y", ".31em")
          .text(function (d) {
              return d.artifact.artifactId;
            });

      function tick() {
        path.attr("d", function (d) {
          var dx = d.target.x - d.source.x,
              dy = d.target.y - d.source.y,
              dr = 0;
          return "M" + d.source.x + "," + d.source.y + "A" + dr + "," + dr + " 0 0,1 " + d.target.x + "," + d.target.y;
        });

        circle.attr("transform", function (d) {
          return "translate(" + d.x + "," + d.y + ")";
        });

        text.attr("transform", function (d) {
          return "translate(" + d.x + "," + d.y + ")";
        });
      }

      function redraw() {
        trans=d3.event.translate;
        scale=d3.event.scale;
        svg.attr("transform",
                 "translate(" + trans + ")"
                     + " scale(" + scale + ")");
      }

      function click(d) {
        var location ="#artifact";
        var selectedRepo=getSelectedBrowsingRepository();
        if (selectedRepo){
          location+="~"+selectedRepo;
        }
        location+="/"+d.artifact.groupId+"/"+d.artifact.artifactId+"/"+d.artifact.version;

        window.sammyArchivaApplication.setLocation(location);
      }

      function onmouseover(d) {
        if (!onmousedown) {
          var x;
          var y;
          if (d3.event.pageX != undefined && d3.event.pageY != undefined) {
            x = d3.event.pageX;
            y = d3.event.pageY;
          } else {
            x = d3.event.clientX + document.body.scrollLeft +
                document.documentElement.scrollLeft;
            y = d3.event.clientY + document.body.scrollTop +
                document.documentElement.scrollTop;
          }
          x += r;
          y += r;
          setTimeout(function() {
            var bubble_code = "<div id='plot_overlay' class='popover fade in left' style='position:absolute; top:"
                + y + "px; left:" + x + "px; z-index: 1; display: block;'>" +
                "<h3 class='popover-title'>Details</h3>" +
                "<div class='popover-content'><ul><li>ArtifactId: " +d.artifact.artifactId + "</li>"
                + "<li>GroupId: " +d.artifact.groupId + "</li>"
                + "<li>Version: " +d.artifact.version + "</li></ul></div>" +
                "</div>";
            $("body").append(bubble_code);
          }, 200);
        }
      }

      function flatten(root) {
        var nodes = [], i = 0;

        function recurse(node) {
          if (node.childs) {
            node.childs.forEach(recurse);
            node.children = node.childs;
          }
          if (!node.id) node.id = ++i;

          nodes.push(node);
        }

        recurse(root);
        return nodes;
      }
    }

    displayGraph=function(treeDependencyUrl) {

      $.ajax(treeDependencyUrl, {
        type: "GET",
        dataType: 'json',
        success: function(data) {
          displayGraphData(data);
        }
      });
    }

    displayGroup=function(groupId){
      var selectedRepo=getSelectedBrowsingRepository();
      var location ="#browse";
      if (selectedRepo){
        location+="~"+selectedRepo;
      }
      location+="/"+groupId;

      window.sammyArchivaApplication.setLocation(location);
    }

    displayArtifactDetailView=function(groupId, artifactId){
      var selectedRepo=getSelectedBrowsingRepository();
      var location ="#artifact";
      if (selectedRepo){
        location+="~"+selectedRepo;
      }
      location+="/"+groupId+"/"+artifactId;

      window.sammyArchivaApplication.setLocation(location);

    }

    displayArtifactVersionDetailViewModel=function(groupId,artifactId,version){
      $.log("ArtifactVersionDetailViewModel#displayArtifactVersionDetailViewModel:"+groupId+":"+artifactId+":"+version);
      var selectedRepo=getSelectedBrowsingRepository();
      var location ="#artifact";
      if (selectedRepo){
        location+="~"+selectedRepo;
      }
      location+="/"+groupId+"/"+artifactId+"/"+version;

      self.groupId=groupId;
      self.artifactId=artifactId;
      self.version=version;
      $("#main-content" ).find("#browse_artifact_detail" ).empty();

      window.sammyArchivaApplication.setLocation(location);
    }


    addProperty=function(){
      self.entries.push(new MetadataEntry("","",true));
    }

    deleteProperty=function(entry){
      var metadatasUrl="restServices/archivaServices/browseService/metadata/"+encodeURIComponent(groupId);
      metadatasUrl+="/"+encodeURIComponent(artifactId);
      metadatasUrl+="/"+encodeURIComponent(version);
      metadatasUrl+="/"+encodeURIComponent(entry.key());
      var selectedRepo=getSelectedBrowsingRepository();

      if(!selectedRepo){
        clearUserMessages();
        displayErrorMessage($.i18n.prop('repository.selected.missing'));
        return;
      }

      metadatasUrl+="?repositoryId="+encodeURIComponent(selectedRepo);

      $.ajax(metadatasUrl, {
        type: "DELETE",
        dataType: 'json',
        success: function(data) {
          clearUserMessages();
          displaySuccessMessage( $.i18n.prop("artifact.metadata.deleted"));
          self.entries.remove(entry);
        }
      });

    }

    hasSavePropertyKarma=function(){
      return hasKarma("archiva-add-metadata");
    }

    hasDeletePropertyKarma=function(){
      return hasKarma("archiva-delete-metadata");
    }

    saveProperty=function(entry){
      if($.trim(entry.key() ).length<1){
        clearUserMessages();
        displayErrorMessage( $.i18n.prop("artifact.metadata.key.mandatory"));
        return;
      }
      if($.trim(entry.value() ).length<1){
        clearUserMessages();
        displayErrorMessage( $.i18n.prop("artifact.metadata.value.mandatory"));
        return;
      }

      var selectedRepo=getSelectedBrowsingRepository();

      if(!selectedRepo){
        clearUserMessages();
        displayErrorMessage($.i18n.prop('repository.selected.missing'));
        return;
      }

      var metadatasUrl="restServices/archivaServices/browseService/metadata/"+encodeURIComponent(groupId);
      metadatasUrl+="/"+encodeURIComponent(artifactId);
      metadatasUrl+="/"+encodeURIComponent(version);
      metadatasUrl+="/"+encodeURIComponent(entry.key());
      metadatasUrl+="/"+encodeURIComponent(entry.value());

      if (selectedRepo){
        metadatasUrl+="?repositoryId="+encodeURIComponent(selectedRepo);
      }
      $.ajax(metadatasUrl, {
        type: "PUT",
        dataType: 'json',
        success: function(data) {
          clearUserMessages();
          displaySuccessMessage( $.i18n.prop("artifact.metadata.added"));
          entry.editable(false);
          entry.modified(false);
        }
      });
    }


    this.gridMetadataViewModel = new ko.simpleGrid.viewModel({
      data: self.entries,
      pageSize: 10
    });

  }

  ArtifactDetailsDownloadViewModel=function(artifacts, artifactVersionDetailViewModel){
    this.artifacts=ko.observableArray(artifacts);
    this.artifactVersionDetailViewModel=artifactVersionDetailViewModel;
    var self=this;
    deleteArtifact=function(artifact){

      clearUserMessages();

      openDialogConfirm(function(){
        $("#dialog-confirm-modal-ok").button('loading');
        $.ajax({
          url:"restServices/archivaServices/repositoriesService/deleteArtifact",
          type:"POST",
          dataType:"json",
          contentType: 'application/json',
          data: ko.toJSON(artifact),
          success:function(data){
            self.artifacts.remove(artifact);
            displaySuccessMessage( $.i18n.prop('artifact.deleted'));
            $("#main-content").find("#artifact-details-download-content" ).html(smallSpinnerImg());
            // reload datas from server
            var artifactDownloadInfosUrl = "restServices/archivaServices/browseService/artifactDownloadInfos/"+encodeURIComponent(self.artifactVersionDetailViewModel.groupId);
            artifactDownloadInfosUrl+="/"+encodeURIComponent(self.artifactVersionDetailViewModel.artifactId)+"/"+encodeURIComponent(self.artifactVersionDetailViewModel.version);
            artifactDownloadInfosUrl+="?repositoryId="+encodeURIComponent(getSelectedBrowsingRepository());

            $.get(artifactDownloadInfosUrl,function(data){
              self.artifacts(mapArtifacts(data));
            });

          },
          error:function(data){
            displayRestError(data,"user-messages");
          },
          complete:function(){
            $("#dialog-confirm-modal-ok").button('reset');
            closeDialogConfirm();
          }
        });
      }, $.i18n.prop('ok'),
          $.i18n.prop('cancel'),
          $.i18n.prop('artifact.delete.confirm.title'),
          $.i18n.prop('artifact.delete.confirm.save'));

    }

    this.deleteKarma = hasKarma('archiva-delete-artifact');

  }

  displayArtifactDownloadContent=function(artifactVersionDetailViewModel){
    var mainContent=$("#main-content");
    mainContent.find("#artifact-details-download-content" ).html(smallSpinnerImg());
    var artifactDownloadInfosUrl = "restServices/archivaServices/browseService/artifactDownloadInfos/"+encodeURIComponent(artifactVersionDetailViewModel.groupId);
    artifactDownloadInfosUrl+="/"+encodeURIComponent(artifactVersionDetailViewModel.artifactId)+"/"+encodeURIComponent(artifactVersionDetailViewModel.version);
    artifactDownloadInfosUrl+="?repositoryId="+encodeURIComponent(getSelectedBrowsingRepository());
    $.get(artifactDownloadInfosUrl,function(data){
      var artifactDetailsDownloadViewModel = new ArtifactDetailsDownloadViewModel(mapArtifacts(data),artifactVersionDetailViewModel);
      mainContent.find("#artifact-details-download-content" ).attr("data-bind",'template:{name:"artifact-details-download-content_tmpl"}');
      ko.applyBindings(artifactDetailsDownloadViewModel,mainContent.find("#artifact-details-download-content" ).get(0));


      mainContent.find("#artifact-download-list-files tr td img" ).on("click",function(){
        mainContent.find("#artifact_content_tree").empty();
        var contentText = mainContent.find("#artifact-content-text" );
        contentText.empty();
        var idValue = $(this ).attr("id");
        var splitted = idValue.split(":");

        var classifier=splitted[0];
        var version=splitted[1];
        var type = splitted[2];

        $.log("click:" + idValue + " -> " + classifier + ":" + type + ":" + version);
        if (type=="pom"){
          $.log("show pom");
          var pomContentUrl = "restServices/archivaServices/browseService/artifactContentText/"+encodeURIComponent(artifactVersionDetailViewModel.groupId);
          pomContentUrl+="/"+encodeURIComponent(artifactVersionDetailViewModel.artifactId)+"/"+encodeURIComponent(version);
          pomContentUrl+="?repositoryId="+encodeURIComponent(getSelectedBrowsingRepository());
          pomContentUrl+="&t=pom";
          contentText.html(mediumSpinnerImg());
          $.ajax({
                   url: pomContentUrl,
                   success: function(data) {
                     var text = data.content.replace(/</g,'&lt;');
                     text=text.replace(/>/g,"&gt;");
                     contentText.html(text);
                     prettyPrint();
                     // olamy do not move to anchor to not loose nav history
                     //goToAnchor("artifact-content-text-header");
                     //window.location.href=window.location+"#artifact-content-text-header";
                   }
                 });
          return;
        }
        var entriesUrl = "restServices/archivaServices/browseService/artifactContentEntries/"+encodeURIComponent(artifactVersionDetailViewModel.groupId);
        entriesUrl+="/"+encodeURIComponent(artifactVersionDetailViewModel.artifactId)+"/"+encodeURIComponent(version);
        entriesUrl+="?repositoryId="+encodeURIComponent(getSelectedBrowsingRepository());
        if(classifier){
          entriesUrl+="&c="+encodeURIComponent(classifier);
        }
        $("#main-content").find("#artifact_content_tree").fileTree({
           script: entriesUrl,
           root: ""
          },function(file) {
           $.log("file:"+file.substringBeforeLast("/")+',classifier:'+classifier);
           var fileContentUrl = "restServices/archivaServices/browseService/artifactContentText/"+encodeURIComponent(artifactVersionDetailViewModel.groupId);
           fileContentUrl+="/"+encodeURIComponent(artifactVersionDetailViewModel.artifactId)+"/"+encodeURIComponent(version);
           fileContentUrl+="?repositoryId="+encodeURIComponent(getSelectedBrowsingRepository());
           if(type){
             fileContentUrl+="&t="+encodeURIComponent(type);
           }
           if(classifier){
             fileContentUrl+="&c="+encodeURIComponent(classifier);
           }
           fileContentUrl+="&p="+encodeURIComponent(file.substringBeforeLast("/"));
           $.ajax({
                    url: fileContentUrl,
                    success: function(data) {
                      var text = data.content.replace(/</g,'&lt;');
                      text=text.replace(/>/g,"&gt;");
                      mainContent.find("#artifact-content-text" ).html(smallSpinnerImg());
                      mainContent.find("#artifact-content-text" ).html(text);
                      prettyPrint();
                      // olamy do not move to anchor to not loose nav history
                      //goToAnchor("artifact-content-text-header");
                      //window.location.href=window.location+"#artifact-content-text-header";
                    }
                  });
          }
        );
      });


    });
    return;
  }

  displayArtifactFilesContent=function(artifactVersionDetailViewModel){
    var mainContent = $("#main-content");
    mainContent.find("#artifact-details-files-content" ).html(smallSpinnerImg());
    var artifactDownloadInfosUrl = "restServices/archivaServices/browseService/artifactDownloadInfos/"+encodeURIComponent(artifactVersionDetailViewModel.groupId);
    artifactDownloadInfosUrl+="/"+encodeURIComponent(artifactVersionDetailViewModel.artifactId)+"/"+encodeURIComponent(artifactVersionDetailViewModel.version);
    artifactDownloadInfosUrl+="?repositoryId="+encodeURIComponent(getSelectedBrowsingRepository());

    $.get(artifactDownloadInfosUrl,function(data){
      $("#artifact-details-files-content" ).html($("#artifact-details-files-content_tmpl").tmpl({artifactDownloadInfos:data}));
      mainContent.find("#artifact-content-list-files li" ).on("click",function(){
        mainContent.find("#artifact_content_tree").empty();
        var contentText = mainContent.find("#artifact-content-text" );
        contentText.empty();
        var idValue = $(this ).attr("id");
        var splitted = idValue.split(":");

        var classifier=splitted[0];
        var version=splitted[1];
        var type = splitted[2];

        $.log("click:" + idValue + " -> " + classifier + ":" + type + ":" + version);
        if (type=="pom"){
          $.log("show pom");
          var pomContentUrl = "restServices/archivaServices/browseService/artifactContentText/"+encodeURIComponent(artifactVersionDetailViewModel.groupId);
          pomContentUrl+="/"+encodeURIComponent(artifactVersionDetailViewModel.artifactId)+"/"+encodeURIComponent(version);
          pomContentUrl+="?repositoryId="+encodeURIComponent(getSelectedBrowsingRepository());
          pomContentUrl+="&t=pom";
          contentText.html(mediumSpinnerImg());
          $.ajax({
            url: pomContentUrl,
            success: function(data) {
              var text = data.content.replace(/</g,'&lt;');
              text=text.replace(/>/g,"&gt;");
              contentText.html(text);
              prettyPrint();
              // olamy do not move to anchor to not loose nav history
              //goToAnchor("artifact-content-text-header");
              //window.location.href=window.location+"#artifact-content-text-header";
            }
          });
          return;
        }
        var entriesUrl = "restServices/archivaServices/browseService/artifactContentEntries/"+encodeURIComponent(artifactVersionDetailViewModel.groupId);
        entriesUrl+="/"+encodeURIComponent(artifactVersionDetailViewModel.artifactId)+"/"+encodeURIComponent(version);
        entriesUrl+="?repositoryId="+encodeURIComponent(getSelectedBrowsingRepository());
        if(classifier){
          entriesUrl+="&c="+encodeURIComponent(classifier);
        }
        $("#main-content").find("#artifact_content_tree").fileTree({
          script: entriesUrl,
          root: ""
    		  },function(file) {
            $.log("file:"+file.substringBeforeLast("/")+',classifier:'+classifier);
            var fileContentUrl = "restServices/archivaServices/browseService/artifactContentText/"+encodeURIComponent(artifactVersionDetailViewModel.groupId);
            fileContentUrl+="/"+encodeURIComponent(artifactVersionDetailViewModel.artifactId)+"/"+encodeURIComponent(version);
            fileContentUrl+="?repositoryId="+encodeURIComponent(getSelectedBrowsingRepository());
            if(type){
              fileContentUrl+="&t="+encodeURIComponent(type);
            }
            if(classifier){
              fileContentUrl+="&c="+encodeURIComponent(classifier);
            }
            fileContentUrl+="&p="+encodeURIComponent(file.substringBeforeLast("/"));
            $.ajax({
             url: fileContentUrl,
             success: function(data) {
               var text = data.content.replace(/</g,'&lt;');
               text=text.replace(/>/g,"&gt;");
               mainContent.find("#artifact-content-text" ).html(smallSpinnerImg());
               mainContent.find("#artifact-content-text" ).html(text);
               prettyPrint();
               // olamy do not move to anchor to not loose nav history
               //goToAnchor("artifact-content-text-header");
               //window.location.href=window.location+"#artifact-content-text-header";
             }
            });
    		  }
        );
      });

    });

  }

  ArtifactContentEntry=function(path,file,depth){
    this.path=path;
    this.file=file;
    this.depth=depth;
  }

  mapArtifactContentEntries=function(data){
    if(data==null){
      return [];
    }
    if ( $.isArray(data)){
      return $.map(data,function(e){
        return new ArtifactContentEntry(e.path,e.file,e.depth);
      })
    }
    return new ArtifactContentEntry(data.path,data.file,data.depth);
  }

  MetadataEntry=function(key,value,editable){
    var self=this;
    this.key=ko.observable(key);
    this.key.subscribe(function(newValue){self.modified(true)});
    this.value=ko.observable(value);
    this.value.subscribe(function(newValue){self.modified(true)});
    this.editable=ko.observable(editable);
    this.modified=ko.observable(false)
  }

  TreeEntry=function(artifact,childs){
    this.artifact=artifact;
    this.childs=childs;
  }

  mapTreeEntries=function(data){
    if (data==null){
      return [];
    }
    return $.map(data,function(e) {
      return new TreeEntry(mapArtifact(e.artifact),mapTreeEntries(e.childs));
    })
  }

  /**
   * display groupId note #main-content must contains browse-tmpl
   * @param groupId
   */
  generalDisplayGroup=function(groupId) {
    $.log("generalDisplayGroup");
    var selectedRepo=getSelectedBrowsingRepository();
    var location ="#browse";
    if (selectedRepo){
      location+="~"+selectedRepo;
    }
    location+="/"+groupId;

    window.sammyArchivaApplication.setLocation(location);
  }

  /**
   * display groupId/artifactId detail note #main-content must contains browse-tmpl
   * @param groupId
   * @param artifactId
   */
  generalDisplayArtifactDetailView=function(groupId, artifactId){
    var selectedRepo=getSelectedBrowsingRepository();
    var location ="#artifact";
    if (selectedRepo){
      location+="~"+selectedRepo;
    }
    location+="/"+groupId+"/"+artifactId;

    window.sammyArchivaApplication.setLocation(location);
  }

  /**
   * display groupId/artifactId/version detail  note #main-content must contains browse-tmpl
   * @param groupId
   * @param artifactId
   * @param version
   */
  generalDisplayArtifactVersionDetailViewModel=function(groupId,artifactId,version){
    var selectedRepo=getSelectedBrowsingRepository();
    var location ="#artifact";
    if (selectedRepo){
      location+="~"+selectedRepo;
    }
    location+="/"+groupId+"/"+artifactId+"/"+version;

    window.sammyArchivaApplication.setLocation(location);
  }

  goToBrowseArtifactDetail=function(groupId, artifactId,repositoryId){
    $.log("goToBrowseArtifactDetail:"+groupId+":"+artifactId);
    //displayBrowseGroupId(groupId,null,null);
    displayArtifactDetail(groupId,artifactId,null,null,repositoryId);
  }

  /**
   *
   */
  displayBrowseGroupId=function(groupId,repositoryId,artifactId){
    clearUserMessages();
    $.log("displayBrowseGroupId:"+groupId+":"+repositoryId);
    userRepositoriesCall(
        function(data){

          $.ajax({
              url: "restServices/archivaServices/archivaAdministrationService/applicationUrl",
              type: "GET",
              dataType: 'text',
              success: function(applicationUrl){

                var mainContent = $("#main-content");
                mainContent.empty();
                mainContent.html($("#browse-tmpl" ).tmpl());
                mainContent.find("#browse_result").html(mediumSpinnerImg());
                var parentBrowseViewModel=new BrowseViewModel(null,null,groupId,repositoryId);
                var url="restServices/archivaServices/browseService/browseGroupId/"+encodeURIComponent(groupId);
                var feedsUrl=applicationUrl?applicationUrl:window.location.toString().substringBeforeLast("/").substringBeforeLast("/");
                if (repositoryId){
                  url+="?repositoryId="+repositoryId;
                  // we are browsing a groupId so 2 substringBeforeLast

                  feedsUrl+="/feeds/"+repositoryId;
                  mainContent.find("#selected_repository" ).html($("#selected_repository_tmpl" )
                                                                     .tmpl({repositories:data,selected:repositoryId,feedsUrl:feedsUrl}));
                }else{
                  feedsUrl+="/feeds";
                  mainContent.find("#selected_repository" ).html($("#selected_repository_tmpl" )
                                                                     .tmpl({repositories:data,selected:"",feedsUrl:null}));
                }

                displayGroupDetail(groupId,parentBrowseViewModel,url,repositoryId,feedsUrl);

              }
          });

        }
    );

  }

  goToArtifactDetail=function(groupId,artifactId){
    var selectedRepo=getSelectedBrowsingRepository();
    var location ="#artifact";
    if (selectedRepo){
      location+="~"+selectedRepo;
    }
    location+="/"+groupId+"/"+artifactId;

    window.sammyArchivaApplication.setLocation(location);
  }

  /**
   *
   * @param groupId
   * @param artifactId
   * @param parentBrowseViewModel
   * @param restUrl
   * @param repositoryId
   */
  displayArtifactDetail=function(groupId,artifactId,parentBrowseViewModel,restUrl,repositoryId){
    $.log("displayArtifactDetail:"+groupId+":"+artifactId);
    var artifactDetailViewModel=new ArtifactDetailViewModel(groupId,artifactId);
    var mainContent = $("#main-content");

    mainContent.html($("#browse-tmpl" ).tmpl());

    userRepositoriesCall(
        function(data){


          $.ajax({
              url: "restServices/archivaServices/archivaAdministrationService/applicationUrl",
              type: "GET",
              dataType: 'text',
              success: function(applicationUrl){

                var feedsUrl=applicationUrl?applicationUrl:window.location.toString().substringBeforeLast("/").substringBeforeLast("/");
                feedsUrl+="/feeds/"+repositoryId;


                if(repositoryId){
                  mainContent.find("#selected_repository" ).html($("#selected_repository_tmpl" )
                              .tmpl({repositories:data,selected:repositoryId,feedsUrl:feedsUrl}));
                } else {
                  mainContent.find("#selected_repository" ).html($("#selected_repository_tmpl" )
                              .tmpl({repositories:data,selected:'',feedsUrl:null}));
                }

                mainContent.find("#browse_artifact_detail").hide();
                mainContent.find("#browse_result").hide();
                $.log("before slide");
                mainContent.find("#main_browse_result_content" ).animate({},500,"linear",function(){
                  $.log("yup");
                  mainContent.find("#browse_breadcrumb").html(smallSpinnerImg());
                  mainContent.find("#browse_artifact").show();
                  mainContent.find("#browse_artifact").html(mediumSpinnerImg());
                  mainContent.find("#main_browse_result_content").show();
                  var metadataUrl="restServices/archivaServices/browseService/projectVersionMetadata/"+encodeURIComponent(groupId)+"/"+encodeURIComponent(artifactId);
                  var versionsListUrl="restServices/archivaServices/browseService/versionsList/"+encodeURIComponent(groupId)+"/"+encodeURIComponent(artifactId);
                  $.log("before getSelectedBrowsingRepository");
                  var selectedRepo=getSelectedBrowsingRepository();
                  if (selectedRepo){
                    metadataUrl+="?repositoryId="+encodeURIComponent(selectedRepo);
                    versionsListUrl+="?repositoryId="+encodeURIComponent(selectedRepo);
                  }
                  $.ajax(metadataUrl, {
                    type: "GET",
                    dataType: 'json',
                    success: function(data) {
                      $.log("metadataUrl ok :"+metadataUrl);
                      artifactDetailViewModel.projectVersionMetadata=mapProjectVersionMetadata(data);
                      $.ajax(versionsListUrl, {
                        type: "GET",
                        dataType: 'json',
                        success: function(data) {
                          artifactDetailViewModel.versions=ko.observableArray(mapVersionsList(data));
                          ko.applyBindings(artifactDetailViewModel,mainContent.find("#browse_artifact").get(0));
                          ko.applyBindings(artifactDetailViewModel,mainContent.find("#browse_breadcrumb").get(0));

                         }
                      });
                    }
                  });
                }
              );
         }})
    });

  }

  browseRoot=function(){
    var selectedRepo=getSelectedBrowsingRepository();

    if(selectedRepo) {
      window.sammyArchivaApplication.setLocation("#browse~"+selectedRepo);
    } else {
      window.sammyArchivaApplication.setLocation("#browse");
    }
  }

  /**
   * call from menu entry to display root level
   * @param freshView redisplay everything
   * @param repositoryId if any repository selected
   */
  displayBrowse=function(freshView,repositoryId){
    screenChange();
    var mainContent = $("#main-content");
    if(freshView){
      mainContent.html($("#browse-tmpl" ).tmpl());
    }
    mainContent.find("#browse_artifact_detail").hide();
    mainContent.find("#browse_artifact" ).hide();
    mainContent.find("#browse_result").html(mediumSpinnerImg());


    userRepositoriesCall(
      function(data) {

        $.ajax({
            url: "restServices/archivaServices/archivaAdministrationService/applicationUrl",
            type: "GET",
            dataType: 'text',
            success: function(applicationUrl){

              var feedsUrl=applicationUrl?applicationUrl:window.location.toString().substringBeforeLast("/").substringBeforeLast("/");
              feedsUrl+="/feeds/"+repositoryId;


              mainContent.find("#selected_repository" ).html($("#selected_repository_tmpl" )
                  .tmpl({repositories:data,selected:repositoryId,feedsUrl:feedsUrl}));
              var url="restServices/archivaServices/browseService/rootGroups";
              if(repositoryId){
                url+="?repositoryId="+repositoryId;
              }
              $.ajax(url, {
                  type: "GET",
                  dataType: 'json',
                  success: function(data) {
                    var browseResultEntries = mapBrowseResultEntries(data);
                    var browseViewModel = new BrowseViewModel(browseResultEntries,null,null,repositoryId);
                    ko.applyBindings(browseViewModel,mainContent.find("#browse_breadcrumb").get(0));
                    ko.applyBindings(browseViewModel,mainContent.find("#browse_result").get(0));
                    enableAutocompleBrowse(null,browseResultEntries);
                  }
              });

          }}
        )

      }
    )

  }

  changeBrowseRepository=function(){
    var selectedRepository=getSelectedBrowsingRepository();
    // #browse~internal/org.apache.maven
    // or #artifact~snapshots/org.apache.maven.plugins/maven-compiler-plugin
    var currentHash=window.location.hash;
    //$.log("currentHash:"+currentHash);
    var newLocation = currentHash.substringBeforeFirst("/");
    //$.log("changeBrowseRepository newLocation:"+newLocation);
    // maybe the current hash contains a repositoryId so remove it
    if (newLocation.indexOf("~")>-1){
      newLocation=currentHash.substringBeforeFirst("~");
    }
    if (selectedRepository){
      newLocation+="~"+selectedRepository;
    }
    if (currentHash.indexOf("/")>-1) {
        // MRM-1767 
        // from all to internal
        // #browse -> #browse~internal
        // #browse/org.a.....  -> #browse~internal/org.a.... not #browse~internalorg.a
        newLocation += "/";
    }
    newLocation += currentHash.substringAfterFirst("/");
    // do we have extra path after repository ?

    $.log("changeBrowseRepository:"+newLocation);
    window.sammyArchivaApplication.setLocation(newLocation);
  }

  getSelectedBrowsingRepository=function(){
    var selectedOption=$("#main-content").find("#select_browse_repository").find("option:selected" );
    if (selectedOption.length>0){
      var repoId=selectedOption.val();
      return repoId;
    }
    return "";
  }

  enableAutocompleBrowse=function(groupId,entries){
    $.log("enableAutocompleBrowse with groupId:'"+groupId+"'");
    $("#select_browse_repository").select2({width: "resolve"});
    // browse-autocomplete
    var url="restServices/archivaServices/browseService/rootGroups";
    if (groupId){
      url="restServices/archivaServices/browseService/browseGroupId/"+encodeURIComponent(groupId);
    }
    var selectedRepo=getSelectedBrowsingRepository();
    if (selectedRepo){
      url+="?repositoryId="+encodeURIComponent(selectedRepo);
    }
    var theGroupId=groupId;
    var browseBox = $("#main-content").find("#browse-autocomplete" );

    browseBox.typeahead(
        {
          name: 'browse-result-'+$.now() ,////hack to avoid local storage caching
          local: entries? entries : [],
          remote: {
            url: url,
            cache: false,
            filter: function(parsedResponse){
              var request = browseBox.val();
              $.log("filter:"+request);

              if (request.indexOf('.')<0&&!groupId){
                // it's rootGroups so filtered
                var result=[];
                for(var i=0;i<parsedResponse.browseResultEntries.length;i++){
                  if (parsedResponse.browseResultEntries[i].name.startsWith(request)){
                    result.push(parsedResponse.browseResultEntries[i]);
                  }
                }
                return result;
              }
              $.log("with dot");
              var query = "";
              var dotEnd=request.endsWith(".");
              // org.apache. request with org.apache
              // org.apa request with org before last dot and filter response with startsWith
              if (request.indexOf(".")>=0){
                if (dotEnd){
                  query= groupId?groupId+'.'+request.substring(0, request.length-1):request.substring(0, request.length-1);
                } else {
                  // substring before last
                  query=groupId?groupId+'.'+request.substringBeforeLast("."):request.substringBeforeLast(".");
                }
              } else {
                query=groupId?groupId:request;
              }
              $.log("query:"+query);
              var browseUrl="restServices/archivaServices/browseService/browseGroupId/"+encodeURIComponent(query);
              var selectedRepo=getSelectedBrowsingRepository();
              if (selectedRepo){
                browseUrl+="?repositoryId="+encodeURIComponent(selectedRepo);
              }
              var result=[];
              $.ajax({
                  url: browseUrl,
                  async:   false,
                  success:  function(data) {
                    $.log("ajax get ok");
                    var browseResultEntries = mapBrowseResultEntries(data);
                    $.log("mapBrowseResultEntries done");
                    if (dotEnd){
                      result=browseResultEntries;
                    }
                    var filtered = [];
                    $.log("filtering with request '"+request+"'");
                    for(var i=0;i<browseResultEntries.length;i++){
                      if (groupId){
                        if (browseResultEntries[i].name.startsWith(groupId+'.'+request)){
                          var item = browseResultEntries[i];
                          item.name=item.name.substring(groupId.length+1, item.name.length);
                          filtered.push(item);
                        }
                      } else {
                        if (browseResultEntries[i].name.startsWith(request)){
                          filtered.push(browseResultEntries[i]);
                        }
                      }
                    }
                    result = filtered;
                  }
              });
              return result;
            }
          },
          valueKey: 'name',
          maxParallelRequests:0,
          limit: 50,
          template: '<p>{{name}}</p>',
          engine: Hogan
        }
    );

    browseBox.on('typeahead:selected', function(obj, datum) {
      $.log("typeahead:selected:"+datum.name+":"+datum.project+",groupId:"+theGroupId);
      //window.sammyArchivaApplication.setLocation("#quicksearch~" + datum.artifactId);

      if (datum.project){
        goToArtifactDetail(datum.groupId,datum.artifactId);
      } else {
        var selectedRepo=getSelectedBrowsingRepository();
        var location ="#browse";
        if (selectedRepo){
          location+="~"+selectedRepo;
        }
        if(theGroupId){
          location+="/"+theGroupId+"."+datum.name;
        }else{
          location+="/"+datum.name;
        }
        $.log("location:"+location);
        window.sammyArchivaApplication.setLocation(location);
      }

    });

    return;

    $( "#main-content").find("#browse-autocomplete" ).autocomplete({
      minLength: 2,
			source: function(request, response){
        var query = "";
        if (request.term.indexOf('.')<0&&!groupId){
          // try with rootGroups then filtered
          $.get(url,
             function(data) {
               var browseResultEntries = mapBrowseResultEntries(data);

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
        // org.apache. request with org.apache
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
          browseUrl+="?repositoryId="+encodeURIComponent(selectedRepo);
        }
        $.get(browseUrl,
           function(data) {
             var browseResultEntries = mapBrowseResultEntries(data);
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
          goToArtifactDetail(groupId,artifactId);
        } else {
          var selectedRepo=getSelectedBrowsingRepository();
          var location ="#browse";
          if (selectedRepo){
            location+="~"+selectedRepo;
          }
          location+="/"+ui.item.name;
          window.sammyArchivaApplication.setLocation(location);
        }
        return false;
      }
		})
    ._renderItem = function( ul, item ) {
          $.log("_renderItem");
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
    var parentBrowseViewModel=new BrowseViewModel(null,null,groupId);
    displayGroupDetail(groupId,parentBrowseViewModel,null);
  }

  displayBrowseArtifactDetail=function(groupId, artifactId){
    $.log("displayBrowseArtifactDetail");
    window.sammyArchivaApplication.setLocation("#artifact/"+groupId+"/"+artifactId);
  }

  mapBrowseResultEntries=function(data){
    $.log("mapBrowseResultEntries");
    if (data.browseResultEntries) {
      return $.isArray(data.browseResultEntries) ?
         $.map(data.browseResultEntries,function(item){
           return new BrowseResultEntry(item.name, item.project,item.groupId,item.artifactId);
         } ).sort(function(a, b){return a.name.localeCompare(b.name)}): [data.browseResultEntries];
    }
    return [];
  }

  BrowseResultEntry=function(name,project,groupId,artifactId){
    this.name=name;
    this.project=project;
    this.groupId=groupId;
    this.artifactId=artifactId;
  }

  BreadCrumbEntry=function(groupId,displayValue){
    this.groupId=groupId;
    this.displayValue=displayValue;
    this.artifactId=null;
    this.artifact=false;
    this.version=null;
    this.fileExtension=null;
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
        projectVersionMetadata.licenses=
                  $.isArray(data.licenses) ? $.map(data.licenses,function(item){
                      return new License(item.name,item.url);
                  }):[data.licenses];
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
              projectVersionMetadata.mavenFacet=new MavenFacet(data.facetList[i].packaging,data.facetList[i].parent);
            }
          }
        } else {
          if(data.facetList.facetId=='org.apache.archiva.metadata.repository.storage.maven2.project'){
            projectVersionMetadata.mavenFacet=new MavenFacet(data.facetList.packaging,data.facetList.parent);
          }
        }
      }
      return projectVersionMetadata;
    }
    return new ProjectVersionMetadata();
  }

  MavenFacet=function(packaging,parent){
    this.packaging=packaging;
    if(parent){
      this.parent={groupId:parent.groupId,artifactId:parent.artifactId,version:parent.version};
    }

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
    var self=this;
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

    this.crumbEntries=function(){
      return calculateCrumbEntries(self.groupId,self.artifactId,self.version);
    }

  }

  //-----------------------------------------
  // search part
  //-----------------------------------------
  Artifact=function(context,url,groupId,artifactId,repositoryId,version,prefix,goals,bundleVersion,bundleSymbolicName,
                    bundleExportPackage,bundleExportService,bundleDescription,bundleName,bundleLicense,bundleDocUrl,
                    bundleImportPackage,bundleRequireBundle,classifier,packaging,fileExtension,size){

    var self=this;

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
    this.bundleName=bundleName;

    //contains osgi metadata Bundle-License if available
    this.bundleLicense=bundleLicense;

    ///contains osgi metadata Bundle-DocURL if available
    this.bundleDocUrl=bundleDocUrl;

    // contains osgi metadata Import-Package if available
    this.bundleImportPackage=bundleImportPackage;

    ///contains osgi metadata Require-Bundle if available
    this.bundleRequireBundle=bundleRequireBundle;

    this.classifier=classifier;

    this.packaging=packaging;

    //file extension of the artifact
    this.fileExtension=fileExtension;

    this.size=size;

    this.crumbEntries=function(){
      return calculateCrumbEntries(self.groupId,self.artifactId,self.version,self.fileExtension);
    }

  }

  calculateCrumbEntries=function(groupId,artifactId,version,fileExtension){
    var splitted = groupId.split(".");
    var breadCrumbEntries=[];
    var curGroupId="";
    for (var i=0;i<splitted.length;i++){
      curGroupId+=splitted[i];
      breadCrumbEntries.push(new BreadCrumbEntry(curGroupId,splitted[i]));
      curGroupId+="."
    }
    var crumbEntryArtifact=new BreadCrumbEntry(groupId,artifactId);
    crumbEntryArtifact.artifactId=artifactId;
    crumbEntryArtifact.artifact=true;
    crumbEntryArtifact.fileExtension=fileExtension;
    breadCrumbEntries.push(crumbEntryArtifact);

    var crumbEntryVersion=new BreadCrumbEntry(groupId,version);
    crumbEntryVersion.artifactId=artifactId;
    crumbEntryVersion.artifact=false;
    crumbEntryVersion.version=version;
    crumbEntryVersion.fileExtension=fileExtension;
    breadCrumbEntries.push(crumbEntryVersion);

    return breadCrumbEntries;
  }

  mapArtifacts=function(data){
    if (data){
      return $.isArray(data)? $.map(data,function(item){return mapArtifact(item)}) : [data];
    }
    return [];
  }

  mapArtifact=function(data){
    if(data){
    return new Artifact(data.context,data.url,data.groupId,data.artifactId,data.repositoryId,data.version,data.prefix,
                        data.goals,data.bundleVersion,data.bundleSymbolicName,
                        data.bundleExportPackage,data.bundleExportService,data.bundleDescription,data.bundleName,
                        data.bundleLicense,data.bundleDocUrl,
                        data.bundleImportPackage,data.bundleRequireBundle,data.classifier,data.packaging,data.fileExtension,data.size);
    }
    return null;
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
    this.includePomArtifacts=ko.observable(true);

    this.classifier=ko.observable();

    // private int pageSize = 30;
    this.pageSize = ko.observable( 30 );
  }

  /**
   * search results view model: display a grid with autocomplete filtering on grid headers
   * @param artifacts
   */
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
        },
        {
          headerText: $.i18n.prop('search.artifact.results.fileExtension'),
          rowText: "fileExtension",
          id: "fileExtension"
        }
      ],
      pageSize: 10,
      gridUpdateCallBack: function(){
        $.log("gridUpdateCallBack search result");
        applyAutocompleteOnHeader('groupId',self);
        $.log("applyAutocompleteOnHeader groupId done ");
        applyAutocompleteOnHeader('artifactId',self);
        applyAutocompleteOnHeader('version',self);
        applyAutocompleteOnHeader('classifier',self);
        applyAutocompleteOnHeader('fileExtension',self);
      }
    });

    applyAutocompleteOnHeader=function(property,resultViewModel){
      $.log("applyAutocompleteOnHeader property:"+property);
      var values=[];
      $(resultViewModel.artifacts()).each(function(idx,artifact){
        var value=artifact[property];
        if(value!=null && $.inArray(value, values)<0){
          values.push(value);
        }
      });

      var box = $( "#main-content").find("#search-filter-auto-"+property );
      box.typeahead( { local: values } );

      box.bind('typeahead:selected', function(obj, datum, name) {
        var artifacts=[];
        $(resultViewModel.artifacts()).each(function(idx,artifact){
          if(artifact[property] && artifact[property].startsWith(datum.value)){
            artifacts.push(artifact);
          }
        });
        resultViewModel.artifacts(artifacts);
      });
    }

    groupIdView=function(artifact){
      displayBrowseGroupId(artifact.groupId);
    }
    artifactIdView=function(artifact){
      displayBrowseArtifactDetail(artifact.groupId,artifact.artifactId,null,null);
    }
    artifactDetailView=function(artifact){

      var selectedRepo=getSelectedBrowsingRepository();

      var location ="#artifact";
      if (selectedRepo){
        location+="~"+selectedRepo;
      }
      location+="/"+artifact.groupId+"/"+artifact.artifactId+"/"+artifact.version;

      if(artifact.classifier){
        location+="/"+artifact.classifier;
      }

      window.sammyArchivaApplication.setLocation(location);
    }
  }

  generalDisplayArtifactDetailsVersionView=function(groupId,artifactId,version,repositoryId,afterCallbackFn){
    var mainContent=$("#main-content");
    mainContent.html($("#browse-tmpl" ).tmpl());
    mainContent.find("#browse_result" ).hide();
    mainContent.find("#browse_artifact_detail").show();
    mainContent.find("#browse_artifact_detail").html(mediumSpinnerImg());
    mainContent.find("#browse_breadcrumb" ).show();
    mainContent.find("#browse_breadcrumb" ).html(mediumSpinnerImg());

    userRepositoriesCall(
      function(data) {
        $.ajax({
            url: "restServices/archivaServices/archivaAdministrationService/applicationUrl",
            type: "GET",
            dataType: 'text',
            success: function(applicationUrl){

              var feedsUrl=applicationUrl?applicationUrl:window.location.toString().substringBeforeLast("/").substringBeforeLast("/");
              feedsUrl+="/feeds/"+repositoryId;
              mainContent.find("#selected_repository" ).html($("#selected_repository_tmpl" )
                          .tmpl({repositories:data,selected:repositoryId,feedsUrl:feedsUrl}));
              var artifactVersionDetailViewModel=new ArtifactVersionDetailViewModel(groupId,artifactId,version,repositoryId);
              artifactVersionDetailViewModel.display(afterCallbackFn);
        }})
      }
    );

  }

  userRepositoriesCall=function(successCallbackFn){
    $.ajax("restServices/archivaServices/browseService/userRepositories", {
        type: "GET",
        dataType: 'json',
        success: function(data) {
          successCallbackFn(data);
        }
    });
  }

  /**
   * View model used for search response and filtering
   */
  SearchViewModel=function(){
    var self=this;
    var mainContent=$("#main-content");
    this.searchRequest=ko.observable(new SearchRequest());
    this.observableRepoIds=ko.observableArray([]);
    this.selectedRepoIds=[];
    this.resultViewModel=new ResultViewModel([]);
    basicSearch=function(){
      var queryTerm=this.searchRequest().queryTerms();
      $.log("basicSearch:"+queryTerm);
      if ($.trim(queryTerm).length<1){
        var errorList=[{
          message: $.i18n.prop("search.artifact.search.form.terms.empty"),
    		  element: $("#main-content").find("#search-basic-form").find("#search-terms" ).get(0)
        }];
        customShowError("#main-content #search-basic-form", null, null, errorList);
        return;
      } else {
        // cleanup previours error message
        customShowError("#main-content #search-basic-form", null, null, []);
      }
      var location="#basicsearch";

      self.selectedRepoIds=[];
      mainContent.find("#search-basic-repositories" )
          .find(".select2-search-choice").each(function(i,span){
                      $.log("find .select2-search-choice");
                      self.selectedRepoIds.push($(span).find("div").html());
                      }
                    );

      if (self.selectedRepoIds.length>0){
        $.log("selectedRepoIds:"+self.selectedRepoIds.length);
        $(self.selectedRepoIds).each(function(index, Element){
          location+="~"+self.selectedRepoIds[index];
        });
      }
      location+="/"+queryTerm;
      window.sammyArchivaApplication.setLocation(location);
    }

    this.externalBasicSearch=function(){
      var queryTerm=this.searchRequest().queryTerms();
      self.search("restServices/archivaServices/searchService/quickSearchWithRepositories",this.searchRequest().repositories);
    }

    /**
     * use from autocomplete search
     */
    this.externalAdvancedSearch=function(){
      this.search("restServices/archivaServices/searchService/searchArtifacts");
    }
    advancedSearch=function(){
      var location="#advancedsearch";

      self.selectedRepoIds=[];
      mainContent.find("#search-basic-repositories" )
          .find(".select2-search-choice").each(function(i,span){
                      self.selectedRepoIds.push($(span).find("div").html());
                      }
                    );

      if (self.selectedRepoIds.length>0){
        $.log("selectedRepoIds:"+self.selectedRepoIds.length);
        $(self.selectedRepoIds).each(function(index, Element){
          location+="~"+self.selectedRepoIds[index];
        });
      }
      location+="/";
      if(self.searchRequest().groupId()){
        location+=self.searchRequest().groupId();
      }
      if(self.searchRequest().artifactId()){
        location+='~'+self.searchRequest().artifactId();
      }else{
        location+='~';
      }
      if(self.searchRequest().version()){
        location+='~'+self.searchRequest().version();
      }else{
        location+='~';
      }
      if(self.searchRequest().classifier()){
        location+='~'+self.searchRequest().classifier();
      }else{
        location+='~';
      }
      if(self.searchRequest().packaging()){
        location+='~'+self.searchRequest().packaging();
      }else{
        location+='~';
      }
      if(self.searchRequest().className()){
        location+='~'+self.searchRequest().className();
      }else{
        location+='~';
      }
      if(self.searchRequest().pageSize()){
        location+='~'+self.searchRequest().pageSize();
      }else{
        location+='~';
      }

      $.log("location:"+location);
      window.sammyArchivaApplication.setLocation(location);
    }
    removeFilter=function(){
      $.log("removeFilter:"+self.resultViewModel.originalArtifacts.length);
      self.resultViewModel.artifacts(self.resultViewModel.originalArtifacts);
    }
    this.search=function(url,repositoriesIds){

      var searchResultsGrid=mainContent.find("#search-results" ).find("#search-results-grid" );
      mainContent.find("#btn-basic-search" ).button("loading");
      mainContent.find("#btn-advanced-search" ).button("loading");
      var userMessages=$("#user-messages");
      userMessages.html(mediumSpinnerImg());
      if (repositoriesIds){
        self.selectedRepoIds=repositoriesIds;
      } else {
        self.selectedRepoIds=[];
        mainContent.find("#search-basic-repositories" )
            .find(".select2-search-choice").each(function(i,span){
                        self.selectedRepoIds.push($(span ).find("div").html());
                        }
                      );
      }
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
            $.log("search#ajax call success:artifacts.length:"+artifacts.length);
            if (artifacts.length<1){
              displayWarningMessage( $.i18n.prop("search.artifact.noresults"));
              return;
            } else {
              self.resultViewModel.originalArtifacts=artifacts;
              $.log("search#ajax call success:self.resultViewModel.originalArtifacts:"+self.resultViewModel.originalArtifacts.length);
              self.resultViewModel.artifacts(artifacts);
              if (!searchResultsGrid.attr("data-bind")){
                $.log('!searchResultsGrid.attr("data-bind")');
                searchResultsGrid.attr("data-bind",
                                 "simpleGrid: gridViewModel,simpleGridTemplate:'search-results-view-grid-tmpl',pageLinksId:'search-results-view-grid-pagination'");
                ko.applyBindings(self.resultViewModel,searchResultsGrid.get(0));
                ko.applyBindings(self,mainContent.find("#remove-filter-id" ).get(0));
                mainContent.find("#search-result-number-div").attr("data-bind","template:{name:'search-result-number-div-tmpl'}");
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
            mainContent.find("#btn-basic-search" ).button("reset");
            mainContent.find("#btn-advanced-search" ).button("reset");
            removeMediumSpinnerImg(userMessages);
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

  /**
   * display a search result (collection of Artifacts) in a grid
   * see template with id #search-artifacts-div-tmpl
   * @param successCallbackFn can be a callback function called on success getting observable repositories.
   * @param searchViewModelCurrent model to reuse if not null whereas a new one is created.
   */
  displaySearch=function(successCallbackFn,searchViewModelCurrent){
    screenChange();
    var mainContent=$("#main-content");
    mainContent.html(mediumSpinnerImg());
    $.ajax("restServices/archivaServices/searchService/observableRepoIds", {
        type: "GET",
        dataType: 'json',
        success: function(data) {
          mainContent.html($("#search-artifacts-div-tmpl" ).tmpl());
          var searchViewModel;
          if (searchViewModelCurrent){
            $.log("searchViewModelCurrent not null");
            searchViewModel=searchViewModelCurrent
          }else {
            $.log("searchViewModelCurrent null");
            searchViewModel=new SearchViewModel();
          }
          var repos=mapStringList(data);
          $.log("repos:"+repos);
          searchViewModel.observableRepoIds(repos);
          ko.applyBindings(searchViewModel,mainContent.find("#search-artifacts-div").get(0));
          mainContent.find("#search-basic-repositories-select" ).select2();
          if (successCallbackFn && $.isFunction(successCallbackFn)) successCallbackFn();
        }
    });

  }



});
