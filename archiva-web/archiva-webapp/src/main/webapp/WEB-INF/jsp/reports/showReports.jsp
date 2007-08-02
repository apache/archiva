<%--
  ~ Licensed to the Apache Software Foundation (ASF) under one
  ~ or more contributor license agreements.  See the NOTICE file
  ~ distributed with this work for additional information
  ~ regarding copyright ownership.  The ASF licenses this file
  ~ to you under the Apache License, Version 2.0 (the
  ~ "License"); you may not use this file except in compliance
  ~ with the License.  You may obtain a copy of the License at
  ~
  ~   http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing,
  ~ software distributed under the License is distributed on an
  ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  ~ KIND, either express or implied.  See the License for the
  ~ specific language governing permissions and limitations
  ~ under the License.
  --%>

<%@ taglib prefix="ww" uri="/webwork" %>

<html>
<head>
  <title>Reports</title>
  <ww:head/>
</head>

<body>
<script>
  function getRowCount()
  {
    document.getElementById("rowCount1_id").value = document.getElementById("rowCountSource_id").value;
    document.getElementById("rowCount2_id").value = document.getElementById("rowCountSource_id").value;
    document.getElementById("rowCount3_id").value = document.getElementById("rowCountSource_id").value;
    document.getElementById("rowCount4_id").value = document.getElementById("rowCountSource_id").value;
  }
</script>

<h1>Reports</h1>

<div id="contentArea">

  <h2>Global Settings</h2>
  <ww:textfield id="rowCountSource_id" label="Row Count" value="100"/>
  <br><br>

  <h2>All Problematic Artifacts</h2>
  <ww:form onsubmit="getRowCount();" action="allProblematicArtifacts" namespace="/report">
    <ww:hidden id="rowCount1_id" name="rowCount"/>
    <ww:submit value="Show All Problematic Artifacts"/>
  </ww:form>
  <br>

  <h2>Problematic Artifacts by Group Id</h2>
  <ww:form action="byGroupId" namespace="/report">
    <ww:hidden id="rowCount2_id" name="rowCount"/>
    <ww:textfield name="groupId"/>
    <ww:submit value="Problematic Artifacts by Group Id"/>
  </ww:form>
  <br>

  <h2>Problematic Artifacts by Artifact Id</h2>
  <ww:form action="byArtifactId" namespace="/report">
    <ww:hidden id="rowCount3_id" name="rowCount"/>
    <ww:textfield name="artifactId"/>
    <ww:submit value="Problematic Artifacts by Artifact Id"/>
  </ww:form>
  <br>

  <h2>Problematic Artifacts by Repository Id</h2>
  <ww:form action="byRepositoryId" namespace="/report">
    <ww:hidden id="rowCount4_id" name="rowCount"/>
    <ww:textfield name="repositoryId"/>
    <ww:submit value="Problematic Artifacts by Repository Id"/>
  </ww:form>

</div>

</body>
</html>
