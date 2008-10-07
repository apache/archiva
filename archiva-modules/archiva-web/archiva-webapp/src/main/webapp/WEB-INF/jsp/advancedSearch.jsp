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
  <title>Advanced Search</title>
  <ww:head/>
</head>

<ww:if test="%{infoMessage != null}">
  <p>${infoMessage}</p>
</ww:if>

<body>

<h1>Advanced Search</h1>


<div id="contentArea">

  <div id="searchBox">
  
    <ww:form method="get" action="filteredSearch" validate="true">
      <ww:textfield label="Row Count" size="50" name="rowCount"/>
      <ww:textfield label="Group Id" size="50" name="groupId"/>
      <ww:textfield label="Artifact Id" size="50" name="artifactId"/>
      <ww:textfield label="Version" size="50" name="version"/>
      <ww:textfield label="Class / Package" size="50" name="className"/>
      <ww:select name="repositoryId" label="Repository ID" list="managedRepositoryList"/>
      <ww:hidden name="completeQueryString" value="${completeQueryString}"/>
      <ww:hidden name="fromFilterSearch" value="${fromFilterSearch}"/>
      <ww:submit label="Go!"/>
    </ww:form>
  
    <ww:url id="indexUrl" action="index"/>
      <ww:a href="%{indexUrl}">
        Quick Search Page
    </ww:a>

  </div>

  <script type="text/javascript">
    document.getElementById("filteredSearch_groupId").focus();
  </script>
  <ww:actionerror/>

</div> 

</body>
</html>
