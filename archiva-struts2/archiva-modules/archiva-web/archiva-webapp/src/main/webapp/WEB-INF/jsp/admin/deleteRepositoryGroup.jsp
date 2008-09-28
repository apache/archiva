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
  <title>Admin: Delete Repository Group</title>
  <ww:head/>
</head>

<body>

<h1>Admin: Delete Repository Group</h1>

<ww:actionerror/>

<div id="contentArea">

  <div class="warningbox">
    <p>
      <strong>WARNING: This operation can not be undone.</strong>
    </p>
  </div>
  
  <p>
    Are you sure you want to delete the following repository group?
  </p>

  <div class="infobox">
    <table class="infotable">
      <tr>
        <td>ID:</td>
        <td><code>${repositoryGroup.id}</code></td>
      </tr>
    </table>
  </div>

  <ww:form method="post" action="deleteRepositoryGroup" namespace="/admin" validate="true" theme="simple">
    <ww:hidden name="repoGroupId"/>
    <div class="buttons">
      <ww:submit value="Confirm" method="delete"/>
      <ww:submit value="Cancel" method="execute"/>
    </div>
  </ww:form>
</div>

</body>
</html>