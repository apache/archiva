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

  ManagedRepository=function(id,name,layout,indexDirectory,location,snapshots,releases,blockRedeployments,cronExpression,
                             scanned,daysOlder,retentionCount,deleteReleasedSnapshots,stageRepoNeeded){
    //private String id;
    id=ko.observable(id);

    //private String name;
    name=ko.observable(name);

    //private String layout = "default";
    layout=ko.observable(layout);

    //private String indexDirectory;
    indexDirectory=ko.observable(indexDirectory);

    //private String location;
    location=ko.observable(location);

    //private boolean snapshots = false;
    snapshots=ko.observable(snapshots);

    //private boolean releases = true;
    releases=ko.observable(releases);

    //private boolean blockRedeployments = false;
    blockRedeployments=ko.observable(blockRedeployments);

    //private String cronExpression = "0 0 * * * ?";
    cronExpression=ko.observable(cronExpression);

    //private ManagedRepository stagingRepository;

    //private boolean scanned = false;
    scanned=ko.observable(scanned);

    //private int daysOlder = 100;
    daysOlder=ko.observable(daysOlder);

    //private int retentionCount = 2;
    retentionCount=ko.observable(retentionCount);

    //private boolean deleteReleasedSnapshots;
    deleteReleasedSnapshots=ko.observable(deleteReleasedSnapshots);

    //private boolean stageRepoNeeded;
    stageRepoNeeded=ko.observable(stageRepoNeeded);
  }


  displayRepositoriesGrid=function(){
    clearUserMessages();

  }

  mapManagedRepository=function(data){
    return new ManagedRepository(data.id,data.name,data.layout,data.indexDirectory,data.location,data.snapshots,data.releases,
                                 data.blockRedeployments,data.cronExpression,
                                 data.scanned,data.daysOlder,data.retentionCount,data.deleteReleasedSnapshots,data.stageRepoNeeded);
  }

});