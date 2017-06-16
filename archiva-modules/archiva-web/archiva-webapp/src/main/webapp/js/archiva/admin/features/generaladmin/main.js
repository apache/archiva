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
define("archiva/admin/features/generaladmin/main",["jquery","jquery.ui","i18n","utils","jquery.tmpl","knockout","knockout.simpleGrid",
  "knockout.sortable","jquery.validate","bootstrap","select2","knockout.select2"]
    , function(jquery,jqueryUi,i18n,utils,jqueryTmpl,ko,simpleGrid,sortable,validate,bootstrap,select2) {

  showMenu = function( administrationMenuItems) {
        administrationMenuItems.push({  text : $.i18n.prop('menu.repository-scanning')      , order:2000, id: "menu-repository-scanning-list-a"    , href: "#scanningList"         , redback: "{permissions: ['archiva-manage-configuration']}", func: function(){displayRepositoryScanning();}});
        administrationMenuItems.push({  text : $.i18n.prop('menu.runtime-configuration')    , order:2010, id: "menu-runtime-configuration-list-a"  , href: "#runtimeconfig"        , redback: "{permissions: ['archiva-manage-configuration']}", func: function(){displayRuntimeConfiguration();}});
        administrationMenuItems.push({  text : $.i18n.prop('menu.system-status')            , order:2020, id: "menu-system-status-list-a"          , href: "#status"               , redback: "{permissions: ['archiva-manage-configuration']}", func: function(){displaySystemStatus();}});
        administrationMenuItems.push({  text : $.i18n.prop('menu.ui-configuration')         , order:2040, id: "menu-ui-configuration-list-a"       , href: "#uiconfig"             , redback: "{permissions: ['archiva-manage-configuration']}", func: function(){displayUiConfiguration();}});
        administrationMenuItems.push({  text : $.i18n.prop('menu.reports')                  , order:2050, id: "menu-report-list-a"                 , href: "#reports"              , redback: "{permissions: ['archiva-manage-configuration']}", func: function(){displayReportsPage();}});
 
  };
 
  //---------------------------
  // repository scanning part
  //---------------------------

  FileType=function(id,patterns){
    //private String id;
    this.id=ko.observable(id);

    //private List<String> patterns;
    this.patterns=ko.observableArray(patterns);

  };

  mapFileType=function(data){
    return new FileType(data.id,data.patterns);
  };

  mapFileTypes=function(data){
    if (data!=null){
      return $.isArray(data)? $.map(data,function(item){
        return mapFileType(item)
      }):[mapFileType(data)];
    }
    return [];
  };

  AdminRepositoryConsumer=function(enabled,id,description){
    //private boolean enabled = false;
    this.enabled=ko.observable(enabled);

    //private String id;
    this.id=ko.observable(id)

    //private String description;
    this.description=ko.observable(description);
  }

  mapAdminRepositoryConsumer=function(data){
    return new AdminRepositoryConsumer(data.enabled,data.id,data.description);
  }

  mapAdminRepositoryConsumers=function(data){
    if (data!=null){
      return $.isArray(data)? $.map(data,function(item){
        return mapAdminRepositoryConsumer(item)
      }):[mapAdminRepositoryConsumer(data)];
    }
    return [];
  }



  RepositoryScanningViewModel=function(){
    var self=this;
    this.fileTypes=ko.observableArray([]);
    this.knownAdminRepositoryConsumers=ko.observableArray([]);
    this.invalidAdminRepositoryConsumers=ko.observableArray([]);

    this.findFileType=function(id){
      var fileType=null;
      for (var i=0;i<self.fileTypes().length;i++){
        if (id==self.fileTypes()[i].id()){
          fileType=self.fileTypes()[i];
        }
      }
      return fileType;
    }

    removeFileTypePattern=function(id,pattern){
      clearUserMessages();
      var url="restServices/archivaServices/archivaAdministrationService/removeFileTypePattern?"
      url+="fileTypeId="+encodeURIComponent(id);
      url+="&pattern="+encodeURIComponent(pattern);
      $.ajax(url, {
          type: "GET",
          dataType: 'json',
          success: function(data){
            self.findFileType(id ).patterns.remove(pattern);
            displaySuccessMessage( $.i18n.prop("repository-scanning.file-types.removed.pattern",id,pattern));

          }
      });
    }

    addFileTypePattern=function(id){
      var pattern=$("#main-content #pattern-"+id ).val();
      $.log("addFileTypePattern:"+id+":"+pattern);
      clearUserMessages();
      var url="restServices/archivaServices/archivaAdministrationService/addFileTypePattern?"
      url+="fileTypeId="+encodeURIComponent(id);
      url+="&pattern="+encodeURIComponent(pattern);
      $.ajax(url, {
          type: "GET",
          dataType: 'json',
          success: function(data){
            self.findFileType(id ).patterns.push(pattern);
            displaySuccessMessage( $.i18n.prop("repository-scanning.file-types.added.pattern",id,pattern));

          }
      });
    }

    disableKnowContentConsumer=function(adminRepositoryConsumer){
      $.log("disableKnowContentConsumer");
      clearUserMessages();
      var userMessages=$("#user-messages" )
      userMessages.html(mediumSpinnerImg());
      var url="restServices/archivaServices/archivaAdministrationService/disabledKnownContentConsumer/"
      url+=encodeURIComponent(adminRepositoryConsumer.id());
      $.ajax(url, {
          type: "GET",
          dataType: 'json',
          success: function(data){
            adminRepositoryConsumer.enabled(false);
            displaySuccessMessage( $.i18n.prop("repository-scanning.consumers.know.disabled",adminRepositoryConsumer.id()));
            removeMediumSpinnerImg(userMessages);
          }
      });
    }

    enableKnowContentConsumer=function(adminRepositoryConsumer){
      clearUserMessages();
      var userMessages=$("#user-messages" )
      userMessages.html(mediumSpinnerImg());
      var url="restServices/archivaServices/archivaAdministrationService/enabledKnownContentConsumer/"
      url+=encodeURIComponent(adminRepositoryConsumer.id());
      $.ajax(url, {
          type: "GET",
          dataType: 'json',
          success: function(data){
            adminRepositoryConsumer.enabled(true);
            displaySuccessMessage( $.i18n.prop("repository-scanning.consumers.know.enabled",adminRepositoryConsumer.id()));
            removeMediumSpinnerImg(userMessages);
          }
      });
    }

    disableInvalidContentConsumer=function(adminRepositoryConsumer){
      clearUserMessages();
      var url="restServices/archivaServices/archivaAdministrationService/disabledInvalidContentConsumer/"
      url+=encodeURIComponent(adminRepositoryConsumer.id());
      $.ajax(url, {
          type: "GET",
          dataType: 'json',
          success: function(data){
            adminRepositoryConsumer.enabled(false);
            displaySuccessMessage( $.i18n.prop("repository-scanning.consumers.invalid.disabled",adminRepositoryConsumer.id()));
          }
      });
    }

    enableInvalidContentConsumer=function(adminRepositoryConsumer){
      clearUserMessages();
      var url="restServices/archivaServices/archivaAdministrationService/enabledInvalidContentConsumer/"
      url+=encodeURIComponent(adminRepositoryConsumer.id());
      $.ajax(url, {
          type: "GET",
          dataType: 'json',
          success: function(data){
            adminRepositoryConsumer.enabled(true);
            displaySuccessMessage( $.i18n.prop("repository-scanning.consumers.invalid.enabled",adminRepositoryConsumer.id()));
          }
      });
    }

  }

  displayRepositoryScanning=function(){
    screenChange();
    var mainContent=$("#main-content");

    mainContent.html($("#repository-scanning-main").tmpl());
    mainContent.find("#file-types-content").html(mediumSpinnerImg());
    mainContent.find("#consumers-known-content").html(mediumSpinnerImg());
    mainContent.find("#consumers-invalid-content").html(mediumSpinnerImg());

    var repositoryScanningViewModel=new RepositoryScanningViewModel();

    $.ajax("restServices/archivaServices/archivaAdministrationService/getFileTypes", {
        type: "GET",
        dataType: 'json',
        success: function(data){
          var fileTypes=mapFileTypes(data);
          repositoryScanningViewModel.fileTypes(fileTypes);
          ko.applyBindings(repositoryScanningViewModel,mainContent.find("#file-types-content").get(0));
        }
    });

    $.ajax("restServices/archivaServices/archivaAdministrationService/getKnownContentAdminRepositoryConsumers", {
        type: "GET",
        dataType: 'json',
        success: function(data){
          var knownAdminRepositoryConsumers=mapAdminRepositoryConsumers(data);
          repositoryScanningViewModel.knownAdminRepositoryConsumers(knownAdminRepositoryConsumers);
          ko.applyBindings(repositoryScanningViewModel,mainContent.find("#consumers-known-content").get(0));
        }
    });

    $.ajax("restServices/archivaServices/archivaAdministrationService/getInvalidContentAdminRepositoryConsumers", {
        type: "GET",
        dataType: 'json',
        success: function(data){
          var invalidAdminRepositoryConsumers=mapAdminRepositoryConsumers(data);
          repositoryScanningViewModel.invalidAdminRepositoryConsumers(invalidAdminRepositoryConsumers);
          ko.applyBindings(repositoryScanningViewModel,mainContent.find("#consumers-invalid-content").get(0));
        }
    });

  }

  //---------------------------
  // network configuration part
  //---------------------------

  NetworkConfiguration=function(maxTotal,maxTotalPerHost,usePooling){
    //private int maxTotal = 30;
    this.maxTotal=ko.observable(maxTotal);

    //private int maxTotalPerHost = 30;
    this.maxTotalPerHost=ko.observable(maxTotalPerHost);

    //private boolean usePooling = true;
    this.usePooling=ko.observable(usePooling);
  }

  NetworkConfigurationViewModel=function(networkConfiguration){
    var self=this;
    this.networkConfiguration=ko.observable(networkConfiguration);

    save=function(){
      var userMessages=$("#user-messages");

      var mainContent=$("#main-content");

      if (!mainContent.find("#network-configuration-edit-form").valid()){
        return;
      }
      userMessages.html(mediumSpinnerImg());
      mainContent.find("#network-configuration-btn-save" ).button('loading');
      $.ajax("restServices/archivaServices/archivaAdministrationService/setNetworkConfiguration", {
        type: "POST",
        contentType: 'application/json',
        data: ko.toJSON(self.networkConfiguration),
        dataType: 'json',
        success: function(data){
          displaySuccessMessage( $.i18n.prop("network-configuration.updated"));
        },
        complete: function(){
          removeMediumSpinnerImg(userMessages);
          mainContent.find("#network-configuration-btn-save" ).button('reset');
        }
      });
    }
  }

  displayRuntimeConfiguration=function(){
    screenChange();
    var mainContent=$("#main-content");

    mainContent.html($("#runtime-configuration-screen").tmpl());
    mainContent.find("#network-configuration-form" ).html(mediumSpinnerImg());
    $.ajax("restServices/archivaServices/archivaAdministrationService/getNetworkConfiguration", {
        type: "GET",
        dataType: 'json',
        success: function(data){

          var networkConfiguration=new NetworkConfiguration(data.maxTotal,data.maxTotalPerHost,data.usePooling);
          var networkConfigurationViewModel=new NetworkConfigurationViewModel(networkConfiguration);
          ko.applyBindings(networkConfigurationViewModel,mainContent.find("#network-configuration-form-content").get(0));
          var validator = mainContent.find("#network-configuration-edit-form")
                  .validate({
                              showErrors: function(validator, errorMap, errorList) {
                                customShowError(mainContent.find("#network-configuration-edit-form" ),validator,errorMap,errorMap);
                              }
                            });
        }
    });


    $.ajax("restServices/archivaServices/archivaRuntimeConfigurationService/archivaRuntimeConfiguration", {
      type: "GET",
      dataType: 'json',
      success: function(data){

        var archivaRuntimeConfiguration=mapArchivaRuntimeConfiguration(data);
        var archivaRuntimeConfigurationViewModel=new ArchivaRuntimeConfigurationViewModel(archivaRuntimeConfiguration);
        ko.applyBindings(archivaRuntimeConfigurationViewModel,mainContent.find("#cache-failure-form").get(0));
        ko.applyBindings(archivaRuntimeConfigurationViewModel,mainContent.find("#filelocking-form").get(0));
        mainContent.find("#cache-failure-form-id")
                .validate({
                            showErrors: function(validator, errorMap, errorList) {
                              customShowError(mainContent.find("#cache-failure-form-id" ),validator,errorMap,errorMap);
                            }
                          });
      }
    });

  }

  ArchivaRuntimeConfigurationViewModel=function(archivaRuntimeConfiguration){
    var self=this;
    this.archivaRuntimeConfiguration=ko.observable(archivaRuntimeConfiguration);

    save=function(){
      var userMessages=$("#user-messages");

      var mainContent=$("#main-content");

      if (!mainContent.find("#cache-failure-form-id").valid()){
        return;
      }
      userMessages.html(mediumSpinnerImg());
      mainContent.find("#cache-failure-form-btn-save" ).button('loading');
      $.ajax("restServices/archivaServices/archivaRuntimeConfigurationService/archivaRuntimeConfiguration", {
        type: "PUT",
        contentType: 'application/json',
        data: ko.toJSON(self.archivaRuntimeConfiguration),
        dataType: 'json',
        success: function(data){
          displaySuccessMessage( $.i18n.prop("archiva.runtime-configuration.updated"));
        },
        error:function(){
          displayErrorMessage("error.500");
        }
      } ).always(
          function(){
            removeMediumSpinnerImg(userMessages);
            mainContent.find("#cache-failure-form-btn-save" ).button('reset');
          }
        );
    }
  }

  FileLockConfiguration=function(skipLocking,lockingTimeout){
    //private boolean skipLocking = true;
    this.skipLocking=ko.observable(skipLocking);
    //private int lockingTimeout = 0;
    this.lockingTimeout=ko.observable(lockingTimeout) ;
  }

  ArchivaRuntimeConfiguration=function(cacheConfiguration,fileLockConfiguration){
    this.urlFailureCacheConfiguration=ko.observable(cacheConfiguration);
    this.fileLockConfiguration=ko.observable(fileLockConfiguration);
  }

  mapFileLockConfiguration=function(data){
    if (!data){
      return null;
    }
    return new FileLockConfiguration(data.skipLocking,data.lockingTimeout);
  }

  mapArchivaRuntimeConfiguration=function(data){
    if(!data){
      return null;
    }
    return new ArchivaRuntimeConfiguration(data.urlFailureCacheConfiguration?mapCacheConfiguration(data.urlFailureCacheConfiguration):null,
                                           data.fileLockConfiguration?mapFileLockConfiguration(data.fileLockConfiguration):null);
  }
  //---------------------------
  // organisation/appearance configuration part
  //---------------------------
  OrganisationInformation=function(name,url,logoLocation){
    this.name=ko.observable(name);
    this.url=ko.observable(url);
    this.logoLocation=ko.observable(logoLocation);
  }
  mapOrganisationInformation=function(data){
    return new OrganisationInformation(data.name, data.url, data.logoLocation);
  }
  mapOrganisationInformations=function(data){
    if (data!=null){
      return $.isArray(data)? $.map(data, function(item){
        return mapOrganisationInformation(item);
      }):[mapOrganisationInformation(data)];
    }
  }
  activateOrganisationInformationFormValidation=function(){
    var validate = $("#main-content" ).find("#appearance-configuration-form-id")
      .validate({
        rules: {
          name: {
            required: true
          },
          url: {
            required:true,
            url:true
          },
          logoLocation: {
            required:false,
            url:true
          }
        },
        showErrors: function(validator, errorMap, errorList) {
          $.log("activateOrganisationInformationFormValidation#customShowError");
          customShowError($("#main-content" ).find("#appearance-configuration-form-id"), validator, errorMap, errorMap);
        }
      });
  }
  OrganisationInformationViewModel=function(organisationInformation){

    this.organisationInformation=ko.observable(organisationInformation);

    this.save=function(){
      $.log("OrganisationInformationViewModel#save");
      var mainContent=$("#main-content" );
      if (!mainContent.find("#appearance-configuration-form-id").valid()) {
        return;
      }
      clearUserMessages();
      var userMessages=$("#user-messages");
      userMessages.html(mediumSpinnerImg());
      mainContent.find("#appearance-configuration-btn-save" ).button('loading');
      $.ajax("restServices/archivaServices/archivaAdministrationService/setOrganisationInformation", {
        type: "POST",
        contentType: "application/json",
        data: ko.toJSON(this.organisationInformation),
        dataType: "json",
        success: function(data){
          displaySuccessMessage($.i18n.prop('appearance-configuration.updated'));
          updateAppearanceToolBar();
        },
        error: function(data){
          displayErrorMessage($.i18n.prop('appearance-configuration.updating-error'));
        },
        complete: function(){
          removeMediumSpinnerImg(userMessages);
          mainContent.find("#appearance-configuration-btn-save" ).button('reset');
        }
      });
    }
  }


  //---------------------------
  // UiConfiguration part
  //---------------------------

  UiConfiguration=function(showFindArtifacts,appletFindEnabled,disableEasterEggs,applicationUrl,disableRegistration){
    this.showFindArtifacts = ko.observable(showFindArtifacts);

    this.appletFindEnabled = ko.observable(appletFindEnabled);

    this.disableEasterEggs = ko.observable(disableEasterEggs);

    this.applicationUrl = ko.observable(applicationUrl);

    // default to false
    this.disableRegistration = ko.observable(disableRegistration?disableRegistration:false);
  }

  UiConfigurationViewModel=function(uiConfiguration){
    this.uiConfiguration=ko.observable(uiConfiguration);
    var self=this;
    save=function(){
      var mainContent=$("#main-content" );
      var userMessages=$("#user-messages");
      userMessages.html( mediumSpinnerImg());
      mainContent.find("#ui-configuration-btn-save" ).button('loading');
      $.ajax("restServices/archivaServices/archivaAdministrationService/setUiConfiguration", {
        type: "POST",
        contentType: 'application/json',
        data: ko.toJSON(self.uiConfiguration),
        dataType: 'json',
        success: function(data){
          displaySuccessMessage( $.i18n.prop("ui-configuration.updated"));
        },
        complete: function(){
          removeMediumSpinnerImg(userMessages);
          mainContent.find("#ui-configuration-btn-save" ).button('reset');
        }
      });
    }
  }

  displayUiConfiguration=function(){
    var mainContent=$("#main-content");

    mainContent.html($("#ui-configuration" ).tmpl());

    mainContent.find("#ui-configuration-form").html(mediumSpinnerImg());

    $.ajax("restServices/archivaServices/archivaAdministrationService/getUiConfiguration", {
      type: "GET",
      dataType: 'json',
      success: function(data){
        var uiConfiguration=new UiConfiguration(data.showFindArtifacts,data.appletFindEnabled,data.disableEasterEggs,
                                                data.applicationUrl,data.disableRegistration);
        var uiConfigurationViewModel=new UiConfigurationViewModel(uiConfiguration);
        ko.applyBindings(uiConfigurationViewModel,mainContent.find("#ui-configuration-form").get(0));
      }
    });

    $.ajax("restServices/archivaServices/archivaAdministrationService/getOrganisationInformation", {
      type: "GET",
      dataType: 'json',
      success: function(data) {
        var organisationInformation=new OrganisationInformation(data.name,data.url,data.logoLocation);
        var organisationInformationViewModel=new OrganisationInformationViewModel(organisationInformation);
        ko.applyBindings(organisationInformationViewModel, mainContent.find("#change-appearance-form").get(0));
        activateOrganisationInformationFormValidation();
      }
    });
  }

  //---------------------------
  // System status part
  //---------------------------

  QueueEntry=function(key,entriesNumber){
    this.key=key;
    this.entriesNumber=entriesNumber;
  }


  mapQueueEntries=function(data){
    if (data!=null){
      return $.map(data,function(item){
        return new QueueEntry(item.key,item.entriesNumber);
      })
    }
    return [];
  }

  CacheEntry=function(key,size,cacheHits,cacheMiss,cacheHitRate,inMemorySize){
    this.key=key;
    this.size=size;
    this.cacheHits=cacheHits;
    this.cacheMiss=cacheMiss;
    this.cacheHitRate=cacheHitRate;
    this.inMemorySize=inMemorySize;
  }

  mapCacheEntries=function(data){
    if(data!=null){
      return $.map(data,function(item){
        return new CacheEntry(item.key,item.size,item.cacheHits,item.cacheMiss,item.cacheHitRate,item.inMemorySize);
      })
    }
    return [];
  }



  displayCacheEntries=function(){
    var divContent = $("#main-content" ).find("#status_caches");
    divContent.html(smallSpinnerImg());
    $.ajax("restServices/archivaServices/systemStatusService/cacheEntries", {
        type: "GET",
        success: function(data){
          var cacheEntries=mapCacheEntries(data);
          divContent.html($("#status_caches_tmpl" ).tmpl({cacheEntries: cacheEntries}));
        }
    });
  }

  flushCache=function(key){
    clearUserMessages();
    $("#main-content" ).find("#status_caches").html(smallSpinnerImg());
    $.ajax("restServices/archivaServices/systemStatusService/clearCache/"+encodeURIComponent(key), {
        type: "GET",
        success: function(data){
          displaySuccessMessage( $.i18n.prop("system-status.caches.flushed",key));
          displayCacheEntries();
        }
    });
  }

  flushAllCaches=function(){
    clearUserMessages();
    $("#main-content" ).find("#status_caches").html(smallSpinnerImg());
    $.ajax("restServices/archivaServices/systemStatusService/clearAllCaches", {
        type: "GET",
        success: function(data){
          displaySuccessMessage( $.i18n.prop("system-status.caches.all.flushed"));
          displayCacheEntries();
        }
    });
  }

  mapRepositoryScannerStatisticsList=function(data){
    if(data!=null){
      return $.isArray(data)? $.map(data,function(item){
        return mapRepositoryScannerStatistics(item);
      }):[data];
    }
    return [];
  }


  mapRepositoryScannerStatistics=function(data){
    return new RepositoryScannerStatistics(mapManagedRepository(data.managedRepository),data.totalFileCount,
                                            data.newFileCount,data.consumerScanningStatistics);
  }

  RepositoryScannerStatistics=function(managedRepository,totalFileCount,newFileCount,consumerScanningStatisticsList){
    //private ManagedRepository managedRepository;
    this.managedRepository=managedRepository

    this.consumerScanningStatisticsList= consumerScanningStatisticsList;

    //private long totalFileCount = 0;
    this.totalFileCount=totalFileCount;

    //private long newFileCount = 0;
    this.newFileCount=newFileCount;
  }

  displayScanningStats=function(){
    var divContent = $("#main-content" ).find("#status_scanning");
    divContent.html(smallSpinnerImg());
    $.ajax("restServices/archivaServices/systemStatusService/repositoryScannerStatistics", {
        type: "GET",
        success: function(data){
          var stats= mapRepositoryScannerStatisticsList(data);
          $.log("size:"+data.length);
          divContent.html($("#status_scanning_tmpl").tmpl({repositoryScannerStatisticsList:stats}));
        }
    });
  }

  displayMemoryUsage=function(){
    var divContent = $("#main-content" ).find("#status_memory_info");
    divContent.html(smallSpinnerImg());
    $.ajax("restServices/archivaServices/systemStatusService/memoryStatus", {
        type: "GET",
        dataType: "text",
        success: function(data){
          var memUsage = data;
          $.log("memUsage:"+memUsage);
          divContent.html(memUsage);
        }
    });
  }

  displayQueueEntries=function(){
    var divContent = $("#main-content" ).find("#status_queues");
    divContent.html(smallSpinnerImg());
    $.ajax("restServices/archivaServices/systemStatusService/queueEntries", {
        type: "GET",
        success: function(data){
          var queueEntries=mapQueueEntries(data);
          divContent.html($("#status_queues_tmpl" ).tmpl({queueEntries: queueEntries}));
        }
    });
  }

  displayServerTime=function(){
    var divContent = $("#main-content" ).find("#status_current_time");
    divContent.html(smallSpinnerImg());
    $.ajax("restServices/archivaServices/systemStatusService/currentServerTime/"+encodeURIComponent(usedLang()), {
        type: "GET",
        dataType: "text",
        success: function(data){
          var curTime=data;
          $.log("currentServerTime:"+curTime);
          divContent.html(curTime);
        }
    });
  }

  displaySystemStatus=function(){
    screenChange();
    var mainContent=$("#main-content");
    mainContent.html($("#system-status-main").tmpl());

    var versionInfo=window.archivaRuntimeInfo.version+" - "
            +$.i18n.prop('system-status.header.version.buildNumber')+": "+window.archivaRuntimeInfo.buildNumber
            +" - "+$.i18n.prop('system-status.header.version.timestampStr')+": "+window.archivaRuntimeInfo.timestampStr;
    mainContent.find("#status_version_info").html(versionInfo);

    displayMemoryUsage();

    displayServerTime();

    displayQueueEntries();

    displayScanningStats();

    displayCacheEntries();
  }

  refreshSystemStatus=function(){
    displayCacheEntries();
    displayScanningStats();
    displayMemoryUsage();
    displayQueueEntries();
    displayServerTime();
  }

  //---------------------------
  // report configuration page
  //---------------------------
  StatisticsReportRequest=function() {
    this.repositories = ko.observableArray( [] );
    this.rowCount = ko.observable(100);
    this.startDate = ko.observable();
    this.endDate = ko.observable();
  }

  reportStatisticsFormValidator=function(){
    $.log("reportStatisticsFormValidator");
    var validate = $("#main-content" ).find("#report-statistics-form-id").validate({
      rules: {
        rowCountStatistics: {
          required:true,
          number: true,
          min: 10
        },
        startDate: {
          date: true
        },
        endDate: {
          date: true
        }
      },
      showErrors: function(validator, errorMap, errorList) {
        $.log("showErrors");
        customShowError("#main-content #report-statistics-form-id", validator, errorMap, errorMap);
      }
    })
  }
  ReportStatisticsViewModel=function(repositoriesAvailable){
    var mainContent=$("#main-content");
    reportStatisticsFormValidator();

    var self=this;
    this.availableRepositories = ko.observableArray( repositoriesAvailable );
    this.statisticsReport = ko.observable( new StatisticsReportRequest() );

    mainContent.find("#startDate" ).datepicker();
    mainContent.find("#endDate" ).datepicker();
    mainContent.find("#rowcount-info-button" ).popover();

    this.showStatistics=function() {
      $.log("showStatistics");
      clearUserMessages( "repositoriesErrorMessage" );
      if (!mainContent.find("#report-statistics-form-id").valid()) {
        return;
      }
      if(this.statisticsReport().repositories().length==0){
        displayErrorMessage( $.i18n.prop('report.statistics.repositories.required'), "repositoriesErrorMessage" );
        return;
      }

      var resultTabContent = mainContent.find("#report-result");

      url = "restServices/archivaServices/reportServices/getStatisticsReport/?rowCount="
        + this.statisticsReport().rowCount();

      for(var i=0;i<this.statisticsReport().repositories().length;i++){
        url += "&repository=" + this.statisticsReport().repositories()[i];
      }

      if(this.statisticsReport().startDate()!=null){
        url += "&startDate=" + this.statisticsReport().startDate();
      }
      if(this.statisticsReport().endDate()!=null){
        url += "&endDate=" + this.statisticsReport().endDate();
      }

      $.ajax(url, {
        type: "GET",
        contentType: 'application/json',
        dataType: 'json',
        success: function(data){
          resultTabContent.html( $( "#report-statistics" ).tmpl() );
          var reportStatistics = new ReportStatisticsResultViewModel( data );
          ko.applyBindings( reportStatistics, resultTabContent.get( 0 ) );
          var reportResultTabLi=$( "#report-result-tab-li");
          reportResultTabLi.removeClass( "hide" );
          reportResultTabLi.addClass( "active" );
          $( "#report-stat-tab-li" ).removeClass( "active" );
          $( "#report-stat-tab-content" ).removeClass( "active" );
          resultTabContent.addClass( "active" );
        },
        error: function(data){
          var res = $.parseJSON(data.responseText);
          displayErrorMessage($.i18n.prop(res.errorMessage));
        }
      });
    }
  }
  ReportStatisticsResultViewModel=function(report){
    this.reports = ko.observableArray( report );
    var self = this;

    this.tableReportViewModel = new ko.simpleGrid.viewModel({
      data: this.reports,
      viewModel: this,
      columns: [
        { headerText: "Repository ID", rowText: "repositoryId" },
        { headerText: "Start Date", rowText: function(item){return new Date(item.scanStartTime);}},
        { headerText: "Total File Count", rowText: "totalFileCount" },
        { headerText: "Total Size", rowText: "totalArtifactFileSize" },
        { headerText: "Artifact Count", rowText: "totalArtifactCount" },
        { headerText: "Group Count", rowText: "totalGroupCount" },
        { headerText: "Project Count", rowText: "totalProjectCount" },
        { headerText: "Archetypes", rowText: function (item) { return !item.totalCountForType.pom || item.totalCountForType.pom === "" ? "0" : item.totalCountForType.pom } },
        { headerText: "Jars", rowText: function (item) { return !item.totalCountForType.war || item.totalCountForType.jar === "" ? "0" : item.totalCountForType.jar } },
        { headerText: "Wars", rowText: function (item) { return !item.totalCountForType.war || item.totalCountForType.war === "" ? "0" : item.totalCountForType.jar } },
        { headerText: "Ears", rowText: function (item) { return !item.totalCountForType.ear || item.totalCountForType.ear === "" ? "0" : item.totalCountForType.ear } },
        { headerText: "Exes", rowText: function (item) { return !item.totalCountForType.exe || item.totalCountForType.exe === "" ? "0" : item.totalCountForType.exe } },
        { headerText: "Dlls", rowText: function (item) { return !item.totalCountForType.dll || item.totalCountForType.dll === "" ? "0" : item.totalCountForType.dll} },
        { headerText: "Zips", rowText: function (item) { return !item.totalCountForType.zip || item.totalCountForType.zip === "" ? "0" : item.totalCountForType.zip } }
      ],
      pageSize: 10
    });
  }

  HealthReportRequest=function(){
    this.repositoryId = ko.observable();
    this.rowCount = ko.observable(100);
    this.groupId = ko.observable();
  }
  HealthReportResult=function(repositoryId,namespace,project,version,id,message,problem,name,facetId){
    this.repositoryId = repositoryId;
    this.namespace = namespace;
    this.project = project;
    this.version = version;
    this.id = id;
    this.message = message;
    this.problem = problem;
    this.name = name;
    this.facetId = facetId;
  }
  mapHealthReportResult=function(data){
    if(data==null) return;
    return new HealthReportResult( data.repositoryId, data.namespace, data.project, data.version, data.id, data.message,
                                   data.problem, data.name, data.facetId );
  }
  mapHealthReportResults=function(data){
    if (data != null)
    {
      return $.isArray(data)? $.map(data, function(item){
        return mapHealthReportResult(item);
      }):[mapHealthReportResult(data)];
    }
    return [];
  }
  ReportHealthResultViewModel=function(report){
    this.reports = ko.observableArray( report );
    var self = this;
    this.tableReportViewModel = new ko.simpleGrid.viewModel({
      data: this.reports,
      viewModel: this,
      columns: [
        { headerText: "ID", rowText: "id" },
        { headerText: "Namespace", rowText: "namespace" },
        { headerText: "Project", rowText: "project" },
        { headerText: "Version", rowText: "version" },
        { headerText: "Name", rowText: "name" },
        { headerText: "Problem", rowText: "problem" },
        { headerText: "Message", rowText: "message" }
        ],
      pageSize: 10
    });
  }

  reportHealthFormValidator=function(){
    var validate = $("#main-content" ).find("#report-health-form-id").validate({
      rules: {
        rowCountHealth: {
          required: true,
          number: true,
          min: 10
        },
        repositoryId: {
          required: true
        }
      },
      showErrors: function(validator, errorMap, errorList) {
        customShowError("#main-content #report-health-form-id", validator, errorMap, errorMap);
      }
    })
  }
  ReportHealthViewModel=function(){
    reportHealthFormValidator();
    this.healthReport = ko.observable(new HealthReportRequest());

    this.showHealth=function() {
      if (!$("#main-content" ).find("#report-health-form-id").valid()) {
        return;
      }

      var resultTabContent = $("#main-content" ).find("#report-result");

      var url =
        "restServices/archivaServices/reportServices/getHealthReports/" + this.healthReport().repositoryId() + "/"
          + this.healthReport().rowCount();

      if (this.healthReport().groupId())
      {
        url += "?groupId=" + this.healthReport().groupId();
      }

      $.ajax(url, {
        type: "GET",
        contentType: 'application/json',
        dataType: 'json',
        success: function(data){
          var reports = new ReportHealthResultViewModel( mapHealthReportResults( data ) );
          resultTabContent.html( $( "#report-health" ).tmpl() );
          ko.applyBindings( reports, resultTabContent.get( 0 ) );
          var reportResultTabLi=$( "#report-result-tab-li" );
          reportResultTabLi.removeClass( "hide" );
          reportResultTabLi.addClass( "active" );
          $( "#report-health-tab-li" ).removeClass( "active" );
          $( "#report-health-tab-content" ).removeClass( "active" );
          resultTabContent.addClass( "active" );
        },
        error: function(data){
            var res = $.parseJSON(data.responseText);
            displayRestError(res);
          }
      });
    }
  }

  displayReportsPage=function(){
    screenChange();
    clearUserMessages();
    var mainContent = $("#main-content");
    mainContent.html(mediumSpinnerImg());
    $.ajax("restServices/archivaServices/searchService/observableRepoIds", {
      type: "GET",
      dataType: 'json',
      success: function(data) {
        var repos = mapStringList( data );
        mainContent.html( $( "#report-base" ).tmpl( {repositoriesList:repos} ) );
        var statisticsReportViewModel = ReportStatisticsViewModel( repos );
        var healthReportViewModel = ReportHealthViewModel( );
        ko.applyBindings( statisticsReportViewModel, mainContent.get( 0 ) );
        ko.applyBindings( healthReportViewModel, mainContent.get( 0 ) );
      }
    });
  }


  RedbackRuntimeConfiguration=function(userManagerImpls,ldapConfiguration,migratedFromRedbackConfiguration,configurationPropertiesEntries
                                      ,useUsersCache,cacheConfiguration,rbacManagerImpls){
    $.log("new RedbackRuntimeConfiguration");
    var self=this;
    this.modified=ko.observable(false);
    this.modified.subscribe(function(newValue){$.log("RedbackRuntimeConfiguration modified")});

    this.userManagerImpls=ko.observableArray(userManagerImpls);
    this.userManagerImpls.subscribe(function(newValue){self.modified(true)});

    this.rbacManagerImpls=ko.observableArray(rbacManagerImpls);
    this.rbacManagerImpls.subscribe(function(newValue){self.modified(true)});

    this.ldapConfiguration=ko.observable(ldapConfiguration);
    this.ldapConfiguration.subscribe(function(newValue){self.modified(true)});

    this.migratedFromRedbackConfiguration=ko.observable(migratedFromRedbackConfiguration);

    this.configurationPropertiesEntries=ko.observableArray(configurationPropertiesEntries?configurationPropertiesEntries:[]);
    this.configurationPropertiesEntries.subscribe(function(newValue){
      self.modified(true);
      $.log("configurationPropertiesEntries modified")
    });

    this.findPropertyValue=function(key){
      for(var i=0;i<self.configurationPropertiesEntries().length;i++){
        if(self.configurationPropertiesEntries()[i].key==key){
          var val = self.configurationPropertiesEntries()[i].value;
          $.log("findPropertyValue " + key + "->" + val);
          return val;
        }
      }
    }

    this.useUsersCache=ko.observable(useUsersCache);
    this.useUsersCache.subscribe(function(newValue){self.modified(true)});

    this.usersCacheConfiguration=ko.observable(cacheConfiguration);
    this.usersCacheConfiguration.subscribe(function(newValue){self.modified(true)});

    this.ldapGroups=ko.observableArray([]);
    this.ldapGroups.subscribe(function(newValue){self.modified(true)});

    this.ldapGroupMappings=ko.observableArray([]);
    this.ldapGroupMappings.subscribe(function(newValue){
      self.modified(true);
      $.log('ldapGroupMappings modified:'+newValue.group);
    });

  }

  mapRedbackRuntimeConfiguration=function(data){

    var ldapConfiguration=mapLdapConfiguration(data.ldapConfiguration);

    var redbackRuntimeConfiguration =
            new RedbackRuntimeConfiguration(data.userManagerImpls,ldapConfiguration,data.migratedFromRedbackConfiguration,[]
                    ,data.useUsersCache,mapCacheConfiguration(data.usersCacheConfiguration),data.rbacManagerImpls);

    $.log("redbackRuntimeConfiguration.rbacManagerImpls:"+redbackRuntimeConfiguration.rbacManagerImpls().length);
    var configurationPropertiesEntries = data.configurationPropertiesEntries == null ? []: $.each(data.configurationPropertiesEntries,function(item){
      var entry = new Entry(item.key, item.value,function(newValue){
        redbackRuntimeConfiguration.modified(true);
      });
      return entry;
    });

    if (!$.isArray(configurationPropertiesEntries)){
      configurationPropertiesEntries=[];
    }

    redbackRuntimeConfiguration.configurationPropertiesEntries(configurationPropertiesEntries);
    redbackRuntimeConfiguration.modified(false);
    return redbackRuntimeConfiguration;

  }

  LdapConfiguration=function(hostName,port,ssl,baseDn,baseGroupsDn,contextFactory,bindDn,password,authenticationMethod,
                             extraPropertiesEntries,writable,useRoleNameAsGroup,bindAuthenticatorEnabled){

    var self=this;
    this.modified=ko.observable(false);

    //private String hostName;
    this.hostName=ko.observable(hostName);
    this.hostName.subscribe(function(newValue){self.modified(true)});

    //private String port;
    this.port=ko.observable(port);
    this.port.subscribe(function(newValue){self.modified(true)});

    //private boolean ssl = false;
    this.ssl=ko.observable(ssl);
    this.ssl.subscribe(function(newValue){self.modified(true)});

    //private String baseDn;
    this.baseDn=ko.observable(baseDn);
    this.baseDn.subscribe(function(newValue){self.modified(true)});

    //private String baseGroupsDn;
    this.baseGroupsDn=ko.observable(baseGroupsDn);
    this.baseGroupsDn.subscribe(function(newValue){self.modified(true)});

    //private String contextFactory;
    this.contextFactory=ko.observable(contextFactory);
    this.contextFactory.subscribe(function(newValue){self.modified(true)});

    //private String bindDn;
    this.bindDn=ko.observable(bindDn);
    this.bindDn.subscribe(function(newValue){self.modified(true)});

    //private String password;
    this.password=ko.observable(password);
    this.password.subscribe(function(newValue){self.modified(true)});

    //private String authenticationMethod;
    this.authenticationMethod=ko.observable(authenticationMethod);
    this.authenticationMethod.subscribe(function(newValue){self.modified(true)});

    this.extraPropertiesEntries=ko.observableArray(extraPropertiesEntries);
    this.extraPropertiesEntries.subscribe(function(newValue){self.modified(true)});

    //private boolean writable = false;
    this.writable=ko.observable(writable);
    this.writable.subscribe(function(newValue){self.modified(true)});

    // useRoleNameAsGroup
    this.useRoleNameAsGroup=ko.observable(useRoleNameAsGroup);
    this.useRoleNameAsGroup.subscribe(function(newValue){self.modified(true)});

    //bindAuthenticatorEnabled
    this.bindAuthenticatorEnabled=ko.observable(bindAuthenticatorEnabled);
    this.bindAuthenticatorEnabled.subscribe(function(newValue){self.modified(true)});

  }

  mapLdapConfiguration=function(data){
    $.log("mapLdapConfiguration");
      if(data){
        var extraPropertiesEntries = data.extraPropertiesEntries == null ? []: $.each(data.extraPropertiesEntries,function(item){
            return new Entry(item.key, item.value);
        });
        if (!$.isArray(extraPropertiesEntries)){
            extraPropertiesEntries=[];
        }
        $.log("mapLdapConfiguration done");
        return new LdapConfiguration(data.hostName,data.port,data.ssl,data.baseDn,data.baseGroupsDn,data.contextFactory,data.bindDn,data.password,
                                    data.authenticationMethod,extraPropertiesEntries,data.writable,data.useRoleNameAsGroup,data.bindAuthenticatorEnabled);
      }
      return null;
  }

  RedbackRuntimeConfigurationViewModel=function(redbackRuntimeConfiguration,userManagerImplementationInformations,rbacManagerImplementationInformations){
    var self=this;
    this.redbackRuntimeConfiguration=ko.observable(redbackRuntimeConfiguration);
    this.userManagerImplementationInformations=ko.observableArray(userManagerImplementationInformations);
    this.rbacManagerImplementationInformations=ko.observableArray(rbacManagerImplementationInformations);

    this.usedUserManagerImpls=ko.observableArray([]);

    this.usedRbacManagerImpls=ko.observableArray([]);

    this.modifiesLdapGroupMappings=ko.observableArray([]);

    this.availableUserManagerImpls=ko.observableArray([]);
    this.availableRbacManagerImpls=ko.observableArray([]);

    this.allRoleNames=ko.observableArray([]);

    findUserManagerImplementationInformation=function(id){
      for(var i= 0;i<self.userManagerImplementationInformations().length;i++){
        $.log(id+""+self.userManagerImplementationInformations()[i].beanId);
        if(id==self.userManagerImplementationInformations()[i].beanId){
          return self.userManagerImplementationInformations()[i];
        }
      }
    }

    findRbacManagerImplementationInformation=function(id){
      for(var i= 0;i<self.rbacManagerImplementationInformations().length;i++){
        $.log(id+""+self.rbacManagerImplementationInformations()[i].beanId);
        if(id==self.rbacManagerImplementationInformations()[i].beanId){
          return self.rbacManagerImplementationInformations()[i];
        }
      }
    }

    checkLdapServerConfiguration=function(){
      $.log("checkLdapServerConfiguration");
      clearUserMessages();
      var btn = $("#ldap-configuration-check-server");
      btn.button('loading');
      var userMessages=$("#user-messages");
      userMessages.html(mediumSpinnerImg());
      $.log("checkChangedLdapConfiguration");
      $.ajax("restServices/archivaServices/redbackRuntimeConfigurationService/checkLdapConnection",
             {
               type: "GET",
               success: function(data) {
                 var message=$.i18n.prop('redback.runtime.ldap.verified');
                 displaySuccessMessage(message);
               },
               error: function(data) {
                 try{
                   var res = $.parseJSON(data.responseText);
                   displayRestError(res);
                 }catch (e){
                   displayErrorMessage( $.i18n.prop("error.500"));
                 }

               }
             }
      ).always(
        function(data){
          removeMediumSpinnerImg(userMessages);
          btn.button('reset');
        }
      );
    }

    checkChangedLdapConfiguration=function(){
      clearUserMessages();
      var btn = $("#ldap-configuration-check-modification");
      btn.button('loading');
      var userMessages=$("#user-messages");
      userMessages.html(mediumSpinnerImg());
      $.log("checkChangedLdapConfiguration");
      $.ajax("restServices/archivaServices/redbackRuntimeConfigurationService/checkLdapConnection",
             {
               type: "POST",
               contentType: 'application/json',
               data:ko.toJSON(self.redbackRuntimeConfiguration().ldapConfiguration),
               dataType: 'json',
               success: function(data) {
                 var message=$.i18n.prop('redback.runtime.ldap.verified');
                 displaySuccessMessage(message);
               },
               error: function(data) {
                 try{
                   var res = $.parseJSON(data.responseText);
                   displayRestError(res);
                 }catch (e){
                   displayErrorMessage( $.i18n.prop("error.500"));
                 }
               }
             }
      ).always(
        function(data){
          removeMediumSpinnerImg(userMessages);
          btn.button('reset');
        }
      );
    }

    for(var i= 0;i<redbackRuntimeConfiguration.userManagerImpls().length;i++){
      var id=redbackRuntimeConfiguration.userManagerImpls()[i];

      var userManagerImplementationInformation=findUserManagerImplementationInformation(id);

      if(userManagerImplementationInformation!=null){
        this.usedUserManagerImpls.push(userManagerImplementationInformation);
      }
    }

    $.log("init usedUserManagerImpls done");

    for(var i= 0;i<redbackRuntimeConfiguration.rbacManagerImpls().length;i++){
      var id=redbackRuntimeConfiguration.rbacManagerImpls()[i];

      var rbacManagerImplementationInformation=findRbacManagerImplementationInformation(id);

      if(rbacManagerImplementationInformation!=null){
        this.usedRbacManagerImpls.push(rbacManagerImplementationInformation);
      }
    }

    $.log("init usedUserManagerImpls done");

    isUsedUserManagerImpl=function(userManagerImplementationInformation){
      for(var i=0;i<self.usedUserManagerImpls().length;i++){
        if(self.usedUserManagerImpls()[i].beanId==userManagerImplementationInformation.beanId){
          return true;
        }
      }
      return false;
    }

    for(var i=0;i<self.userManagerImplementationInformations().length;i++){
      if(!isUsedUserManagerImpl(self.userManagerImplementationInformations()[i])){
        self.availableUserManagerImpls.push(self.userManagerImplementationInformations()[i]);
      }
    }

    userManagerImplMoved=function(arg){
      $.log("userManagerImplMoved");
      self.redbackRuntimeConfiguration().modified(true);
    }

    isUsedRbacManagerImpl=function(rbacManagerImplementationInformation){
      for(var i=0;i<self.usedRbacManagerImpls().length;i++){
        if(self.usedRbacManagerImpls()[i].beanId==rbacManagerImplementationInformation.beanId){
          return true;
        }
      }
      return false;
    }



    for(var i=0;i<self.rbacManagerImplementationInformations().length;i++){
      if(!isUsedRbacManagerImpl(self.rbacManagerImplementationInformations()[i])){
        self.availableRbacManagerImpls.push(self.rbacManagerImplementationInformations()[i]);
      }
    }

    rbacManagerImplMoved=function(arg){
      $.log("rbacManagerImplMoved");
      self.redbackRuntimeConfiguration().modified(true);
    }

    saveRedbackRuntimeConfiguration=function(){

      var mainContent=$("#main-content");
      var valid = mainContent.find("#redback-runtime-general-form-id").valid();
      if (valid==false) {
        return;
      }
      var useLdap = false;
      for(var i=0;i<self.usedUserManagerImpls().length;i++){
        var beanId=self.usedUserManagerImpls()[i].beanId;
        $.log("beanId:"+beanId);
        if(beanId=='ldap'){
          useLdap=true;
        }
      }
      $.log("useLdap:"+useLdap);
      if(useLdap==true) {
        valid = mainContent.find("#redback-runtime-ldap-form-id").valid();
        $.log("ldap valid:"+valid);
        if (valid==false) {
          return;
        }
      }

      clearUserMessages();

      if (self.usedUserManagerImpls().length<1){
        displayErrorMessage( $.i18n.prop("error.redback.runtimeconfiguration.user-managers.empty"));
        valid=false;
      }

      if (self.usedRbacManagerImpls().length<1){
        displayErrorMessage( $.i18n.prop("error.redback.runtimeconfiguration.rbac-managers.empty"));
        valid=false;
      }

      if(valid==false){
        return;
      }

      $.log("saveRedbackRuntimeConfiguration");
      var saveButton = mainContent.find("#redback-runtime-configuration-save" );
      saveButton.button('loading');
      clearUserMessages();
      var userMessages=$("#user-messages");
      userMessages.html(mediumSpinnerImg());

      self.redbackRuntimeConfiguration().userManagerImpls=ko.observableArray([]);

      for(var i=0;i<self.usedUserManagerImpls().length;i++){
        var beanId=self.usedUserManagerImpls()[i].beanId;
        $.log("beanId:"+beanId);
        self.redbackRuntimeConfiguration().userManagerImpls.push(beanId);
      }


      self.redbackRuntimeConfiguration().rbacManagerImpls=ko.observableArray([]);

      for(var i=0;i<self.usedRbacManagerImpls().length;i++){
        var beanId=self.usedRbacManagerImpls()[i].beanId;
        $.log("beanId:"+beanId);
        self.redbackRuntimeConfiguration().rbacManagerImpls.push(beanId);
      }

      var adminAvailableResponseFn = function(adminExists) {
        $.log("admin exists "+adminExists);
        if (adminExists) {
          window.sammyArchivaApplication.runRoute("get","#redbackruntimeconfig");
        } else {
          logout();
          displayWelcome();
        }
      }
      $.log("rememberme enabled:"+self.redbackRuntimeConfiguration().findPropertyValue("security.rememberme.enabled"));
      $.ajax("restServices/archivaServices/redbackRuntimeConfigurationService/redbackRuntimeConfiguration",
        {
          type: "PUT",
          contentType: 'application/json',
          data:ko.toJSON(self.redbackRuntimeConfiguration),
          dataType: 'json',
          success: function(data) {
            var message=$.i18n.prop('redback-runtime-configuration.updated');
            checkCreateAdminLink(adminAvailableResponseFn);
            displaySuccessMessage(message);
          },
          error: function(data) {
            var res = $.parseJSON(data.responseText);
            displayRestError(res);
          }
        }
      ).always(
        function(){
          removeMediumSpinnerImg(userMessages);
          saveButton.button('reset');
          self.redbackRuntimeConfiguration().modified(false);
          self.redbackRuntimeConfiguration().ldapConfiguration().modified(false);
          saveModifyLdapGroupMapping();
        }
      );



    }

    saveModifyLdapGroupMapping=function(){
      //save modified ldap group mappings if any
      if(self.modifiesLdapGroupMappings().length>0){
        $.log("save modifiesLdapGroupMappings");
        var message=$.i18n.prop('redback-runtime-ldap-group-mapping.updated');
        var userMessages=$("#user-messages");
        var ldapGroupMappings=[];
        $.each(self.modifiesLdapGroupMappings(),function(idx,item){

          if(!(item.automatic&item.roleNames().length<2)){
            $.log("update mapping for group:"+item.group());
            ldapGroupMappings.push(item);
          }
        });

        if (ldapGroupMappings.length>0){

          var mainContent=$("#main-content");
          var saveButton = mainContent.find("#redback-runtime-configuration-save" );
          saveButton.button('loading');
          userMessages.html(mediumSpinnerImg());
          $.ajax("restServices/redbackServices/ldapGroupMappingService",
                 {
                   type: "POST",
                   contentType: 'application/json',
                   data:ko.toJSON(new LdapGroupMappingUpdateRequest(ldapGroupMappings)),
                   dataType: 'json',
                   success: function(data) {
                     displaySuccessMessage(message);
                   },
                   error: function(data) {
                     var res = $.parseJSON(data.responseText);
                     displayRestError(res);
                   },
                   complete:function(data){
                     removeMediumSpinnerImg(userMessages);
                     saveButton.button('reset');
                     self.redbackRuntimeConfiguration().modified(false);
                     self.redbackRuntimeConfiguration().ldapConfiguration().modified(false);
                   }
                 }
          );
        }


      } else {
        $.log("not save modifiesLdapGroupMappings");
      }
      self.modifiesLdapGroupMappings=ko.observableArray([]);
    }
    this.modifiesLdapGroupMappings=ko.observableArray([]);

    this.modifyLdapGroupMapping=function(roleNames,ldapGroupMapping){
      var toAdd=true;
      $.each(self.modifiesLdapGroupMappings(),function(idx,item){
        if(item.group()==ldapGroupMapping.group()){
          toAdd=false;
        }
      });
      if(toAdd){
        self.modifiesLdapGroupMappings().push(ldapGroupMapping);
      }
      $.log('modifyLdapGroupMapping:'+ldapGroupMapping.group()+','+self.modifiesLdapGroupMappings().length);
    };
    //olamy could be better but some reason doesn't work and I didn't find enough to understand why :-)
    /*self.gridldapMappingsViewModel = new ko.simpleGrid.viewModel({
     data: self.redbackRuntimeConfiguration().ldapGroupMappings,
     pageSize: 10,
     gridUpdateCallBack: function(){
       $.log("ldapGroupsMapping: gridUpdateCallBack");
       $("#main-content").find("#ldap-group-mappings-div select" ).select2({width: "element"});
     }
    });*/

    self.gridViewModel = new ko.simpleGrid.viewModel({
     data: self.redbackRuntimeConfiguration().configurationPropertiesEntries,
     columns: [
       {
         headerText: $.i18n.prop('redback.runtime.properties.key.label'),
         rowText: "key"
       },
       {
         headerText: $.i18n.prop('redback.runtime.properties.value.label'),
         rowText: "value"
       }
     ],
     pageSize: 10,
     gridUpdateCallBack: function(){
       activatePopoverDoc();
     }
    });

    this.newLdapGroupMapping=ko.observable(new LdapGroupMapping("",[],false,null));

    addLdapGroupMapping=function(){
      // FIXME validate datas from ldapGroupMapping
      $.log("addLdapGroupMapping:"+self.newLdapGroupMapping().group()+",roles:"+self.newLdapGroupMapping().roleNames().length);
      clearUserMessages();

      if (self.newLdapGroupMapping().roleNames().length<1){
        displayErrorMessage( $.i18n.prop('redback-runtime-ldap-group-mapping.role.mandatory'));
        return;
      }

      var mainContent=$("#main-content");
      var saveButton = mainContent.find("#redback-runtime-configuration-save" );
      saveButton.button('loading');
      clearUserMessages();
      var userMessages=$("#user-messages");
      userMessages.html(mediumSpinnerImg());
      $.ajax("restServices/redbackServices/ldapGroupMappingService",
             {
               type: "PUT",
               contentType: 'application/json',
               data:ko.toJSON(new LdapGroupMapping(self.newLdapGroupMapping().group(),self.newLdapGroupMapping().roleNames(),false,null)),
               dataType: 'json',
               success: function(data) {
                 self.redbackRuntimeConfiguration().ldapGroupMappings
                         .unshift(new LdapGroupMapping(self.newLdapGroupMapping().group(),self.newLdapGroupMapping().roleNames(),false,self.modifyLdapGroupMapping));
                 $.log("addLdapGroupMapping:"+self.redbackRuntimeConfiguration().ldapGroupMappings().length);
                 var message=$.i18n.prop('redback-runtime-ldap-group-mapping.added');
                 displaySuccessMessage(message);
               },
               error: function(data) {
                 var res = $.parseJSON(data.responseText);
                 displayRestError(res);
               }
             }
      ).always(
        function(){
          removeMediumSpinnerImg(userMessages);
          $.log("addLdapGroupMapping#always");
          //self.newLdapGroupMapping.group("");
          //self.newLdapGroupMapping.roleNames([]);
          //self.newLdapGroupMapping(new LdapGroupMapping("",[],false,null));
          saveButton.button('reset');
        }
      );


    }

    deleteLdapGroupMapping=function(ldapGroupMapping){


      var mainContent=$("#main-content");
      var saveButton = mainContent.find("#redback-runtime-configuration-save" );
      saveButton.button('loading');
      clearUserMessages();
      var userMessages=$("#user-messages");
      userMessages.html(mediumSpinnerImg());
      $.ajax("restServices/redbackServices/ldapGroupMappingService/"+ldapGroupMapping.group(),
             {
               type: "DELETE",
               contentType: 'application/json',
               dataType: 'json',
               success: function(data) {
                 $.log("deleteLdapGroupMapping:"+ldapGroupMapping.group());
                 self.redbackRuntimeConfiguration().ldapGroupMappings.remove(ldapGroupMapping);
                 var message=$.i18n.prop('redback-runtime-ldap-group-mapping.deleted');
                 displaySuccessMessage(message);
               },
               error: function(data) {
                 var res = $.parseJSON(data.responseText);
                 displayRestError(res);
               }
             }
      ).always(
        function(){
          removeMediumSpinnerImg(userMessages);
          saveButton.button('reset');
        }
      );
    }
  }

  ManagerImplementationInformation=function(beanId,descriptionKey,readOnly){
    this.beanId=beanId;
    this.descriptionKey=descriptionKey;
    this.description= $.i18n.prop(descriptionKey);
    this.readOnly=readOnly;
  }

  mapManagerImplementationInformations=function(data){
    return $.map(data, function(item) {
      return mapManagerImplementationInformation(item);
    });
  }

  mapManagerImplementationInformation=function(data){
    if(data==null){
      return null;
    }
    return new ManagerImplementationInformation(data.beanId,data.descriptionKey,data.readOnly);
  }

  activateRedbackRuntimeGeneralFormValidation=function(){
    var formSelector=$("#main-content" ).find("#redback-runtime-general-form-id");
    var validator = formSelector.validate({
      rules: {
        usersCacheTimeToLiveSeconds : {
         digits: true,
         min: 1,
         required: true
       },
        usersCacheTimeToIdleSeconds : {
          digits: true,
          min: 1,
          required: true
        },
        maxElementsInMemory : {
          digits: true,
          min: 1,
          required: true
        },
        maxElementsOnDisk : {
          digits: true,
          min: 1,
          required: true
        }
      },
      showErrors: function(validator, errorMap, errorList) {
       customShowError(formSelector,validator,errorMap,errorMap);
      }
      });
  }

  activateLdapConfigurationFormValidation=function(){
    var formSelector=$("#main-content" ).find("#redback-runtime-ldap-form-id");
    var validator = formSelector.validate({
      rules: {
        ldapHost : {
          required: true
        },
        ldapPort : {
          digits: true,
          min: 1,
          required: true
        }
      },
      showErrors: function(validator, errorMap, errorList) {
        customShowError(formSelector,validator,errorMap,errorMap);
      }
    });
  }

  displayRedbackRuntimeConfiguration=function(){
    var jqueryUi = require("jquery.ui");
    $.log("displayRuntimeConfiguration");
    var mainContent = $("#main-content");
    mainContent.html(mediumSpinnerImg());

    $.ajax("restServices/archivaServices/redbackRuntimeConfigurationService/redbackImplementationsInformations", {
      type: "GET",
      dataType: 'json',
      success: function(data) {
        var userManagerImplementationInformations=data?mapManagerImplementationInformations(data.userManagerImplementationInformations):[];
        var rbacManagerImplementationInformations=data?mapManagerImplementationInformations(data.rbacManagerImplementationInformations):[];
        $.log("rbacManagerImplementationInformations:"+rbacManagerImplementationInformations.length);
        $.ajax("restServices/archivaServices/redbackRuntimeConfigurationService/redbackRuntimeConfiguration", {
          type: "GET",
          dataType: 'json',
          success: function(data) {
            var redbackRuntimeConfiguration = mapRedbackRuntimeConfiguration(data);
            $.log("before new RedbackRuntimeConfigurationViewModel");
            var redbackRuntimeConfigurationViewModel =
                new RedbackRuntimeConfigurationViewModel(redbackRuntimeConfiguration,userManagerImplementationInformations,rbacManagerImplementationInformations);

            var groups=[];
            $.log("before useLdap:"+$.inArray("ldap",redbackRuntimeConfiguration.userManagerImpls())
                          +','+$.inArray("ldap",redbackRuntimeConfiguration.rbacManagerImpls()));
            var useLdap = $.inArray("ldap",redbackRuntimeConfiguration.userManagerImpls())>=0
                    ||$.inArray("ldap",redbackRuntimeConfiguration.rbacManagerImpls())>=0;
            $.log("useLdap:"+useLdap);
            if(useLdap){
              $.log("in get ldap groups");

              // load ldap roles
              $.ajax("restServices/redbackServices/ldapGroupMappingService/ldapGroups", {
                type: "GET",
                dataType: 'json',
                success: function(data){
                  groups = mapStringList(data);
                  $.log("groups number:"+groups.length);
                  redbackRuntimeConfiguration.ldapGroups=ko.observableArray(groups);
                }
              } )
              .always(
                function() {
                  $.log("complete");

                  // load ldap group mappings if any
                  $.ajax("restServices/redbackServices/ldapGroupMappingService", {
                    type: "GET",
                    dataType: 'json',
                    success: function(data) {
                      var groupMappings=mapLdapGroupMappings(data,redbackRuntimeConfigurationViewModel.modifyLdapGroupMapping);

                      $.ajax("restServices/redbackServices/roleManagementService/allRoles", {
                        type: "GET",
                        dataType: 'json',
                        success: function(data) {
                          displayRuntimeConfigurationScreen(redbackRuntimeConfigurationViewModel,data,groupMappings,groups);
                        }
                      });
                    }
                  });
                }
              );
            } else {
              $.log("before displayRuntimeConfigurationScreen");
              displayRuntimeConfigurationScreen(redbackRuntimeConfigurationViewModel,null,null);
            }
          }
        });

      }
    });

  }

  displayRuntimeConfigurationScreen=function(redbackRuntimeConfigurationViewModel,allRoles,groupMappings,groups){
    var allRoleNames = $.map(allRoles?allRoles:[], function(item) {
      return item.name;
    });

    redbackRuntimeConfigurationViewModel.allRoleNames=ko.observableArray(allRoleNames);

    if (redbackRuntimeConfigurationViewModel.redbackRuntimeConfiguration().ldapConfiguration().useRoleNameAsGroup()&&groups) {
      // if using groups == roles add all as mapping except already mapped
      $.each(groups,function(idx,item){
        var exists=false;
        // avoid duplicate if mapping really exists !
        $.each(groupMappings,function(idx2,groupMapping){
          if(groupMapping.group()==item){
            exists=true;
          }
        });
        if(!exists){
          groupMappings.push(new LdapGroupMapping(item,[item],true,redbackRuntimeConfigurationViewModel.modifyLdapGroupMapping));
        }

      });
    }

    redbackRuntimeConfigurationViewModel.redbackRuntimeConfiguration().ldapGroupMappings=ko.observableArray(groupMappings?groupMappings:[]);
    redbackRuntimeConfigurationViewModel.redbackRuntimeConfiguration().modified(false);

    $("#main-content").html( $("#redback-runtime-configuration-main" ).tmpl() );
    $.log("applyBindings:#redback-runtime-configuration-content:"+redbackRuntimeConfigurationViewModel.redbackRuntimeConfiguration().ldapGroupMappings().length);
    ko.applyBindings(redbackRuntimeConfigurationViewModel,$("#redback-runtime-configuration-content" ).get(0));
    activateRedbackRuntimeGeneralFormValidation();
    activateLdapConfigurationFormValidation();
  }

  LdapGroupMapping=function(group,roleNames,automatic,subscribeFn){
    var self=this;
    this.modified=ko.observable(false);
    //private String group;
    this.group=ko.observable(group);
    this.group.subscribe(function(newValue){
      self.modified(true);
      if(subscribeFn){
        subscribeFn(newValue,self)
      }
    });

    //private Collection<String> roleNames;
    this.roleNames=ko.observableArray(roleNames);
    this.roleNames.subscribe(function(newValue){
      self.modified(true);
      $.log("roleNames modified");
      if(subscribeFn){
        subscribeFn(newValue,self);
      }
    });

    this.automatic=automatic?automatic:false;
    this.update=true;
  }

  LdapGroupMappingUpdateRequest=function(ldapGroupMappings){
    this.ldapGroupMappings=ko.observableArray(ldapGroupMappings?ldapGroupMappings:[]);
  }

  mapLdapGroupMappings=function(data,modifyLdapGroupMapping){
    if(data!=null){
      return $.map(data,function(item){
        return new LdapGroupMapping(item.group,item.roleNames?item.roleNames:[],false,modifyLdapGroupMapping);
      })
    }
    return [];
  }

  CacheConfiguration=function(timeToIdleSeconds,timeToLiveSeconds,maxElementsInMemory,maxElementsOnDisk){
    var self=this;
    this.modified=ko.observable(false);

    this.timeToIdleSeconds=ko.observable(timeToIdleSeconds);
    this.timeToIdleSeconds.subscribe(function(newValue){self.modified(true)});

    this.timeToLiveSeconds=ko.observable(timeToLiveSeconds);
    this.timeToLiveSeconds.subscribe(function(newValue){self.modified(true)});

    this.maxElementsInMemory=ko.observable(maxElementsInMemory);
    this.maxElementsInMemory.subscribe(function(newValue){self.modified(true)});

    this.maxElementsOnDisk=ko.observable(maxElementsOnDisk);
    this.maxElementsOnDisk.subscribe(function(newValue){self.modified(true)});

  }

  mapCacheConfiguration=function(data){
    if(!data){
      return new CacheConfiguration();
    }
    return new CacheConfiguration(data.timeToIdleSeconds,data.timeToLiveSeconds,data.maxElementsInMemory,data.maxElementsOnDisk);
  }

 

});
