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

  // FIXME this must be dynamic if we do a plugin mechanism with dynamic repositories types
  // FIXME i18n

  ManagedRepositoryType=function(type,label){
    this.type=type;
    this.label=label;
  }

  window.managedRepositoryTypes = [
            new ManagedRepositoryType("default","Maven 2.x Repository"),
            new ManagedRepositoryType("legacy", "Maven 1.x Repository")
            ];

  ManagedRepository=function(id,name,layout,indexDirectory,location,snapshots,releases,blockRedeployments,cronExpression,
                             scanned,daysOlder,retentionCount,deleteReleasedSnapshots,stageRepoNeeded){


    //private String id;
    this.id=ko.observable(id);

    //private String name;
    this.name=ko.observable(name);

    //private String layout = "default";
    this.layout=ko.observable(layout);

    //private String indexDirectory;
    this.indexDirectory=ko.observable(indexDirectory);

    //private String location;
    this.location=ko.observable(location);

    //private String cronExpression = "0 0 * * * ?";
    this.cronExpression=ko.observable(cronExpression);

    //private ManagedRepository stagingRepository;

    //private int daysOlder = 100;
    this.daysOlder=ko.observable(daysOlder);

    //private int retentionCount = 2;
    this.retentionCount=ko.observable(retentionCount);

    //private boolean scanned = false;
    this.scanned=ko.observable(scanned);

    //private boolean deleteReleasedSnapshots;
    this.deleteReleasedSnapshots=ko.observable(deleteReleasedSnapshots);

    //private boolean stageRepoNeeded;
    this.stageRepoNeeded=ko.observable(stageRepoNeeded);

    //private boolean snapshots = false;
    this.snapshots=ko.observable(snapshots);

    //private boolean releases = true;
    this.releases=ko.observable(releases);

    //private boolean blockRedeployments = false;
    this.blockRedeployments=ko.observable(blockRedeployments);

    var self=this;

    this.getTypeLabel=function(){
      for(i=0;i<window.managedRepositoryTypes.length;i++){
        if (window.managedRepositoryTypes[i].type==self.layout()){
          return window.managedRepositoryTypes[i].label;
        }
      }
      return "no label";
    }

  }

  ArchivaRepositoryStatistics=function(scanEndTime,scanStartTime,totalArtifactCount,totalArtifactFileSize,totalFileCount,
                                       totalGroupCount,totalProjectCount,newFileCount,duration,managedRepository){
    //private Date scanEndTime;
    this.scanEndTime = ko.observable(scanEndTime);

    //private Date scanStartTime;
    this.scanStartTime = ko.observable(scanStartTime);

    //private long totalArtifactCount;
    this.totalArtifactCount = ko.observable(totalArtifactCount);

    //private long totalArtifactFileSize;
    this.totalArtifactFileSize = ko.observable(totalArtifactFileSize);

    //private long totalFileCount;
    this.totalFileCount = ko.observable(totalFileCount);

    //private long totalGroupCount;
    this.totalGroupCount = ko.observable(totalGroupCount);

    //private long totalProjectCount;
    this.totalProjectCount = ko.observable(totalProjectCount);

    //private long newFileCount;
    this.newFileCount = ko.observable(newFileCount);

    this.duration = ko.observable(duration);

    this.managedRepository = managedRepository;
  }

  mapManagedRepositories=function(data){
    var mappedManagedRepositories = $.map(data.managedRepository, function(item) {
      return mapManagedRepository(item);
    });
    return mappedManagedRepositories;
  }
  mapManagedRepository=function(data){
    if (data==null){
      return null;
    }
    return new ManagedRepository(data.id,data.name,data.layout,data.indexDirectory,data.location,data.snapshots,data.releases,
                                 data.blockRedeployments,data.cronExpression,
                                 data.scanned,data.daysOlder,data.retentionCount,data.deleteReleasedSnapshots,data.stageRepoNeeded);
  }

  mapArchivaRepositoryStatistics=function(data){
    if (data==null){
      return null;
    }
    return new ArchivaRepositoryStatistics(data.scanEndTime,data.scanStartTime,data.totalArtifactCount,data.totalArtifactFileSize,
                                           data.totalFileCount,data.totalGroupCount,data.totalProjectCount,data.newFileCount,
                                           data.duration,data.managedRepository)
  }

  ManagedRepositoryViewModel=function(managedRepository, update, managedRepositoriesViewModel){
    this.managedRepository=managedRepository;
    this.managedRepositoriesViewModel = managedRepositoriesViewModel;
    this.update = update;

    var self = this;

    this.availableLayouts = window.managedRepositoryTypes;

    save=function(){
      $.log("repositories.js#save");
      var valid = $("#main-content #managed-repository-edit-form").valid();
      if (valid==false) {
          return;
      }
      $.log("save:"+this.managedRepository.name());
      clearUserMessages();
      if (this.update){
        $.ajax("restServices/archivaServices/managedRepositoriesService/updateManagedRepository",
          {
            type: "POST",
            data: "{\"managedRepository\": " + ko.toJSON(this.managedRepository)+"}",
            contentType: 'application/json',
            dataType: 'json',
            success: function(data) {
              displaySuccessMessage($.i18n.prop('managedrepository.updated'));
              activateManagedRepositoriesGridTab();
            },
            error: function(data) {
              var res = $.parseJSON(data.responseText);
              displayRestError(res);
            }
          }
        );
      } else {
        var url="restServices/archivaServices/managedRepositoriesService/fileLocationExists";
        url+="?fileLocation="+encodeURIComponent(self.managedRepository.location());
        $.ajax(url,
        {
          type: "GET",
          dataType: 'json',
          success: function(data) {
            if (data){
              openDialogConfirm(
                  function(){addManagedRepository(self.managedRepository),function(){window.modalConfirmDialog.modal('hide')}},
                  $.i18n.prop('ok'), $.i18n.prop('cancel'),
                  $.i18n.prop('managedrepository.add.title'),
                  $("#managed-repository-location-warning-tmpl").tmpl(self.managedRepository));
            }else{
              addManagedRepository(self.managedRepository);
            }
          }
        });
      }
    }

    addManagedRepository=function(managedRepository,completeCallbackFn){
      $.log("add managedRepo");
      var curManagedRepository=managedRepository;
      var callbackFn = completeCallbackFn;
      $.ajax("restServices/archivaServices/managedRepositoriesService/addManagedRepository",
        {
          type: "POST",
          contentType: 'application/json',
          data: "{\"managedRepository\": " + ko.toJSON(managedRepository)+"}",
          dataType: 'json',
          success: function(data) {
            curManagedRepository.location(data.managedRepository.location);
            self.managedRepositoriesViewModel.managedRepositories.push(curManagedRepository);
            displaySuccessMessage($.i18n.prop('managedrepository.added'));
            activateManagedRepositoriesGridTab();
          },
          error: function(data) {
            var res = $.parseJSON(data.responseText);
            displayRestError(res);
          },
          complete:function(data){
            if(callbackFn){
              callbackFn();
            }
          }
        }
      );
    }

    displayGrid=function(){
      activateManagedRepositoriesGridTab();
    }

  }

  activateManagedRepositoryFormValidation=function(){
    $("#main-content #managed-repository-edit-form").validate({
      rules: {
        daysOlder : {
          digits: true,
          min: 1
        },
        retentionCount : {
          digits: true,
          min: 1,
          max: 100
        },
        cronExpression: {
          required: true,
          remote: {
            url: "restServices/archivaServices/commonServices/validateCronExpression", // ?cronExpression="+$("#managed-repository-edit-form #cronExpression").val(),
            type: "get"
          }
        }
      },
      showErrors: function(validator, errorMap, errorList) {
        customShowError(validator,errorMap,errorMap);
      }
    });
  }

  ManagedRepositoriesViewModel=function(){
    this.managedRepositories=ko.observableArray([]);

    this.gridViewModel = null;
    var self = this;

    editManagedRepository=function(managedRepository){
      var viewModel = new ManagedRepositoryViewModel(managedRepository,true,self);
      ko.applyBindings(viewModel,$("#main-content #managed-repository-edit").get(0));
      activateManagedRepositoryEditTab();
      $("#managed-repository-edit-li a").html($.i18n.prop('edit'));
      activateManagedRepositoryFormValidation();
    }

    scanNow=function(managedRepository){
      clearUserMessages();
      openDialogConfirm(
          function(){
            $("#dialog-confirm-modal #modal-login-footer").append(smallSpinnerImg());
            var checked = $("#managed-repository-scan-now-all").get(0).checked;
            var url = "restServices/archivaServices/repositoriesService/scanRepositoryNow?";
            url += "repositoryId="+encodeURIComponent(managedRepository.id());
            url += "&fullScan="+(checked==true?"true":"false");
            $.ajax(url,
              {
                type: "GET",
                  beforeSend:function(){
                    displayInfoMessage($.i18n.prop("managedrepository.scan.started"));
                  },
                  success: function(data) {
                    displaySuccessMessage($.i18n.prop("managedrepository.scanned",managedRepository.name()));
                  },
                  error: function(data) {
                    var res = $.parseJSON(data.responseText);
                    displayRestError(res);
                  },
                  complete: function(){
                    removeSmallSpinnerImg();
                    closeDialogConfirm();
                  }
              }
            );
          },
          $.i18n.prop("ok"),
          $.i18n.prop("cancel"),
          $.i18n.prop("managedrepository.scan.now"),
          $("#managed-repository-scan-now-modal-tmpl").tmpl(managedRepository));

    }

    removeManagedRepository=function(managedRepository){
      clearUserMessages();
      openDialogConfirm(
          function(){
            $("#dialog-confirm-modal #modal-login-footer").append(smallSpinnerImg());
            var url = "restServices/archivaServices/managedRepositoriesService/deleteManagedRepository?";
            url += "repositoryId="+encodeURIComponent(managedRepository.id());

            var checked = $("#managedrepository-deletecontent").get(0).checked;

            url += "&deleteContent="+(checked==true?"true":"false");
            $.ajax(url,
              {
                type: "GET",
                  success: function(data) {
                    self.managedRepositories.remove(managedRepository);
                    displaySuccessMessage($.i18n.prop("managedrepository.deleted",managedRepository.name()));

                  },
                  error: function(data) {
                    var res = $.parseJSON(data.responseText);
                    displayRestError(res);
                  },
                  complete: function(){
                    removeSmallSpinnerImg();
                    closeDialogConfirm();
                  }
              }
            );

          },
          $.i18n.prop("ok"),
          $.i18n.prop("cancel"),
          $.i18n.prop("managedrepository.delete.confirm",managedRepository.name()),
          $("#managed-repository-delete-warning-tmpl").tmpl(managedRepository));
    }

    showStats=function(managedRepository){
      if ($(calculatePopoverId(managedRepository)).html()){
        // we ask stats all the time ? if no uncomment return
        //return;
        $("#managedrepository-stats-"+managedRepository.id()).append(smallSpinnerImg());
      }
      var curRepo=managedRepository;
      var url = "restServices/archivaServices/managedRepositoriesService/getManagedRepositoryStatistics/"+managedRepository.id();
      $.ajax(url,
        {
          type: "GET",
          dataType: 'json',
          success: function(data) {
            if (data.archivaRepositoryStatistics==null){
              return;
            }
            var archivaRepositoryStatistics=mapArchivaRepositoryStatistics(data.archivaRepositoryStatistics);
            archivaRepositoryStatistics.managedRepository=curRepo;

            $("#managedrepository-stats-"+curRepo.id()).append($("#managed-repository-stats-tmpl").tmpl(archivaRepositoryStatistics));
            $("#managedrepository-stats-img-"+curRepo.id()).attr("data-content",$(calculatePopoverId(curRepo)).html());
            $("#managedrepository-stats-img-"+curRepo.id()).popover(
                {
                  placement: "left",
                  html: true,
                  title: "popover-title"
                }
            );

            $("#managedrepository-stats-img-"+curRepo.id()).popover('show');
            removeSmallSpinnerImg();
          },
          error: function(data) {
            var res = $.parseJSON(data.responseText);
            displayRestError(res);
          },
          complete: function(){
           }
        }
      );
    }

    calculatePopoverId=function(managedRepository){
      return "#managedrepository-stats-"+managedRepository.id() + " #managedrepository-stats-"+managedRepository.id()+"-popover";
    }

    hideStats=function(managedRepository){

    }

    showPomSnippet=function(managedRepository){

      $("#managed-repositories-pom-snippet").html(mediumSpinnerImg());
      $('#managed-repositories-pom-snippet').show();
      var url = "restServices/archivaServices/managedRepositoriesService/getPomSnippet/"+managedRepository.id();
      $.ajax(url,
        {
          type: "GET",
          dataType: 'text',
          success: function(data) {
            $("#managed-repositories-pom-snippet").html($("#pom-snippet-tmpl").tmpl(data));
          },
          error: function(data) {
            var res = $.parseJSON(data.responseText);
            displayRestError(res);
          },
          complete: function(){
          }
        }
      );

    }



  }

  activateManagedRepositoriesGridTab=function(){
    $("#main-content #managed-repository-edit-li").removeClass("active");
    $("#main-content #managed-repository-edit").removeClass("active");

    $("#main-content #managed-repositories-view-li").addClass("active");
    $("#main-content #managed-repositories-view").addClass("active");
    $("#main-content #managed-repository-edit-li a").html($.i18n.prop("add"));
  }

  activateManagedRepositoryEditTab=function(){
    $("#main-content #managed-repositories-view-li").removeClass("active");
    $("#main-content #managed-repositories-view").removeClass("active");

    $("#main-content #managed-repository-edit-li").addClass("active");
    $("#main-content #managed-repository-edit").addClass("active");
  }


  //---------------------------
  // Remote repositories part
  //---------------------------



  RemoteRepository=function(id,name,layout,indexDirectory,url,userName,password,timeout,downloadRemoteIndex,remoteIndexUrl,
                            remoteDownloadNetworkProxyId,cronExpression,remoteDownloadTimeout,downloadRemoteIndexOnStartup){


    //private String id;
    this.id=ko.observable(id);

    //private String name;
    this.name=ko.observable(name);

    //private String layout = "default";
    this.layout=ko.observable(layout);

    //private String indexDirectory;
    this.indexDirectory=ko.observable(indexDirectory);

    //private String url;
    this.url=ko.observable(url);

    //private String userName;
    this.userName=ko.observable(userName);

    //private String password;
    this.password=ko.observable(password);

    //private int timeout = 60;
    this.timeout=ko.observable(timeout);

    //private boolean downloadRemoteIndex = false;
    this.downloadRemoteIndex=ko.observable(downloadRemoteIndex);

    //private String remoteIndexUrl = ".index";
    this.remoteIndexUrl=ko.observable(remoteIndexUrl);

    //private String remoteDownloadNetworkProxyId;
    this.remoteDownloadNetworkProxyId=ko.observable(remoteDownloadNetworkProxyId);

    //private String cronExpression = "0 0 08 ? * SUN";
    this.cronExpression=ko.observable(cronExpression);

    //private int remoteDownloadTimeout = 300;
    this.remoteDownloadTimeout=ko.observable(remoteDownloadTimeout);

    //private boolean downloadRemoteIndexOnStartup = false;
    this.downloadRemoteIndexOnStartup=ko.observable(downloadRemoteIndexOnStartup);

    var self=this;

    this.getTypeLabel=function(){
      for(i=0;i<window.managedRepositoryTypes.length;i++){
        if (window.managedRepositoryTypes[i].type==self.layout()){
          return window.managedRepositoryTypes[i].label;
        }
      }
      return "no label";
    }
  }

  mapRemoteRepository=function(data){
    if (data==null){
      return null;
    }
    return new RemoteRepository(data.id,data.name,data.layout,data.indexDirectory,data.url,data.userName,data.password,
                                data.timeout,data.downloadRemoteIndex,data.remoteIndexUrl,data.remoteDownloadNetworkProxyId,
                                data.cronExpression,data.remoteDownloadTimeout,data.downloadRemoteIndexOnStartup);
  }

  mapRemoteRepositories=function(data){
    var mappedRemoteRepositories = $.map(data.remoteRepository, function(item) {
      return mapRemoteRepository(item);
    });
    return mappedRemoteRepositories;
  }

  RemoteRepositoryViewModel=function(remoteRepository, update, remoteRepositoriesViewModel){
    this.remoteRepository=remoteRepository;
    this.remoteRepositoriesViewModel = remoteRepositoriesViewModel;
    this.update = update;

    var self = this;

    this.availableLayouts = window.managedRepositoryTypes;

    save=function(){
      var valid = $("#main-content #remote-repository-edit-form").valid();
      if (valid==false) {
        return;
      }
      clearUserMessages();
      if (update){
        $.ajax("restServices/archivaServices/remoteRepositoriesService/updateRemoteRepository",
          {
            type: "POST",
            data: "{\"remoteRepository\": " + ko.toJSON(this.remoteRepository)+"}",
            contentType: 'application/json',
            dataType: 'json',
            success: function(data) {
              displaySuccessMessage($.i18n.prop('remoterepository.updated'));
              activateRemoteRepositoriesGridTab();
            },
            error: function(data) {
              var res = $.parseJSON(data.responseText);
              displayRestError(res);
            }
          }
        );
      }else {
        $.ajax("restServices/archivaServices/remoteRepositoriesService/addRemoteRepository",
          {
            type: "POST",
            data: "{\"remoteRepository\": " + ko.toJSON(this.remoteRepository)+"}",
            contentType: 'application/json',
            dataType: 'json',
            success: function(data) {
              self.remoteRepositoriesViewModel.remoteRepositories.push(self.remoteRepository);
              displaySuccessMessage($.i18n.prop('remoterepository.added'));
              activateRemoteRepositoriesGridTab();
            },
            error: function(data) {
              var res = $.parseJSON(data.responseText);
              displayRestError(res);
            }
          }
        );
      }
    }

    displayGrid=function(){
      activateRemoteRepositoriesGridTab();
    }
  }

  RemoteRepositoriesViewModel=function(){
    this.remoteRepositories=ko.observableArray([]);

    this.gridViewModel = null;
    var self = this;

    editRemoteRepository=function(remoteRepository){
      $.log("editRemoteRepository");
      var viewModel = new RemoteRepositoryViewModel(remoteRepository,true,self);
      ko.applyBindings(viewModel,$("#main-content #remote-repository-edit").get(0));
      activateRemoteRepositoryEditTab();
      $("#remote-repository-edit-li a").html($.i18n.prop('edit'));
      activateRemoteRepositoryFormValidation();
    }

    removeRemoteRepository=function(remoteRepository){
      $.ajax("restServices/archivaServices/remoteRepositoriesService/deleteRemoteRepository/"+remoteRepository.id(),
        {
          type: "GET",
          success: function(data) {
            self.remoteRepositories.remove(remoteRepository);
            displaySuccessMessage($.i18n.prop('remoterepository.deleted'));
          },
          error: function(data) {
            var res = $.parseJSON(data.responseText);
            displayRestError(res);
          }
        }
      );
    }

    scheduleDownloadRemoteIndex=function(remoteRepository){
      openDialogConfirm(
        function(){

          var url = "restServices/archivaServices/repositoriesService/scheduleDownloadRemoteIndex?";
          url += "repositoryId="+encodeURIComponent(remoteRepository.id());

          var now = $("#remoterepository-scan-now").get(0).checked;

          var full = $("#remoterepository-scan-full").get(0).checked;

          url += "&now="+(now==true?"true":"false");
          url += "&fullDownload="+(full==true?"true":"false");
          $.ajax(url,
            {
              type: "GET",
                success: function(data) {
                  displaySuccessMessage($.i18n.prop("remoterepository.scanned.scheduled",remoteRepository.name()));

                },
                error: function(data) {
                  var res = $.parseJSON(data.responseText);
                  displayRestError(res);
                },
                complete: function(){
                  closeDialogConfirm();
                }
            }
          );

        },
        $.i18n.prop("ok"),
        $.i18n.prop("cancel"),
        $.i18n.prop("remoterepository.scan.confirm",remoteRepository.name()),
        $("#remote-repository-scan-modal-tmpl").tmpl(remoteRepository));
    }
  }

  activateRemoteRepositoryFormValidation=function(){
    // FIXME find a way to activate cronExpression validation only if downloadRemote is activated !
    $("#main-content #remote-repository-edit-form").validate({
      /*rules: {
        daysOlder : {
          digits: true,
          min: 1
        },
        retentionCount : {
          digits: true,
          min: 1,
          max: 100
        }
      },*/
      showErrors: function(validator, errorMap, errorList) {
        customShowError(validator,errorMap,errorMap);
      }
    });
  }

  activateRemoteRepositoriesGridTab=function(){
    $("#main-content #remote-repository-edit-li").removeClass("active");
    $("#main-content #remote-repository-edit").removeClass("active");

    $("#main-content #remote-repositories-view-li").addClass("active");
    $("#main-content #remote-repositories-view").addClass("active");
    $("#main-content #remote-repository-edit-li a").html($.i18n.prop("add"));
  }

  activateRemoteRepositoryEditTab=function(){
    $("#main-content #remote-repositories-view-li").removeClass("active");
    $("#main-content #remote-repositories-view").removeClass("active");

    $("#main-content #remote-repository-edit-li").addClass("active");
    $("#main-content #remote-repository-edit").addClass("active");
  }

  //---------------------------
  // Screen loading
  //---------------------------

  displayRepositoriesGrid=function(){
    clearUserMessages();
    $("#main-content").html(mediumSpinnerImg());
    $("#main-content").html($("#repositoriesMain").tmpl());
    $("#main-content #repositories-tabs").tabs();

    $("#main-content #managed-repositories-content").append(mediumSpinnerImg());
    $("#main-content #remote-repositories-content").append(mediumSpinnerImg());

    var managedRepositoriesViewModel = new ManagedRepositoriesViewModel();
    var remoteRepositoriesViewModel = new RemoteRepositoriesViewModel();

    $.ajax("restServices/archivaServices/managedRepositoriesService/getManagedRepositories", {
        type: "GET",
        dataType: 'json',
        success: function(data) {
          managedRepositoriesViewModel.managedRepositories(mapManagedRepositories(data));
          managedRepositoriesViewModel.gridViewModel = new ko.simpleGrid.viewModel({
            data: managedRepositoriesViewModel.managedRepositories,
            columns: [
              {
                headerText: $.i18n.prop('identifier'),
                rowText: "id"
              },
              {
                headerText: $.i18n.prop('name'),
                rowText: "name"
              },
              {
              headerText: $.i18n.prop('type'),
              rowText: "getTypeLabel",
              // FIXME i18n
              title: "Repository type (default is Maven 2)"
              }
            ],
            pageSize: 5,
            gridUpdateCallBack: function(){
              $("#main-content #managed-repositories-table [title]").twipsy();
            }
          });
          ko.applyBindings(managedRepositoriesViewModel,$("#main-content #managed-repositories-table").get(0));
          $("#main-content #managed-repositories-pills").pills();
          $("#managed-repositories-view").addClass("active");
          removeMediumSpinnerImg("#main-content #managed-repositories-content");
          activateManagedRepositoriesGridTab();
        }
      }
    );

    $.ajax("restServices/archivaServices/remoteRepositoriesService/getRemoteRepositories", {
        type: "GET",
        dataType: 'json',
        success: function(data) {
          remoteRepositoriesViewModel.remoteRepositories(mapRemoteRepositories(data));
          remoteRepositoriesViewModel.gridViewModel = new ko.simpleGrid.viewModel({
            data: remoteRepositoriesViewModel.remoteRepositories,
            columns: [
              {
                headerText: $.i18n.prop('identifier'),
                rowText: "id"
              },
              {
                headerText: $.i18n.prop('name'),
                rowText: "name"
              },
              {
                headerText: $.i18n.prop('url'),
                rowText: "url"
              },
              {
              headerText: $.i18n.prop('type'),
              rowText: "getTypeLabel",
              // FIXME i18n
              title: "Repository type (default is Maven 2)"
              }
            ],
            pageSize: 5,
            gridUpdateCallBack: function(){
              $("#main-content #remote-repositories-table [title]").twipsy();
            }
          });
          ko.applyBindings(remoteRepositoriesViewModel,$("#main-content #remote-repositories-table").get(0));
          $("#main-content #remote-repositories-pills").pills();
          $("#remote-repositories-view").addClass("active");
          removeMediumSpinnerImg("#main-content #remote-repositories-content");
        }
      }
    );


    $("#main-content #managed-repositories-pills").bind('change', function (e) {
      if ($(e.target).attr("href")=="#managed-repository-edit") {
        var viewModel = new ManagedRepositoryViewModel(new ManagedRepository(),false,managedRepositoriesViewModel);
        ko.applyBindings(viewModel,$("#main-content #managed-repository-edit").get(0));
        activateManagedRepositoryFormValidation();
      }
      if ($(e.target).attr("href")=="#managed-repositories-view") {
        $("#main-content #managed-repository-edit-li a").html($.i18n.prop("add"));
      }

    });

    $("#main-content #remote-repositories-pills").bind('change', function (e) {
      if ($(e.target).attr("href")=="#remote-repository-edit") {
        var viewModel = new RemoteRepositoryViewModel(new RemoteRepository(),false,remoteRepositoriesViewModel);
        ko.applyBindings(viewModel,$("#main-content #remote-repository-edit").get(0));
        activateRemoteRepositoryFormValidation();
      }
      if ($(e.target).attr("href")=="#remote-repositories-view") {
        $("#main-content #remote-repository-edit-li a").html($.i18n.prop("add"));
      }

    });

  }

});