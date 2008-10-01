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

<%@ taglib prefix="s" uri="/struts-tags" %>
<html>
<head>
  <title>Advanced Search</title>
  <s:head/>
</head>

<s:if test="%{infoMessage != null}">
  <p>${infoMessage}</p>
</s:if>

<body>

<h1>Advanced Search</h1>


<div id="contentArea">

  <div id="searchBox">
  
    <s:form method="get" action="filteredSearch" validate="true">
      <s:textfield label="Row Count" size="50" name="rowCount"/>
      <s:textfield label="Group Id" size="50" name="groupId"/>
      <s:textfield label="Artifact Id" size="50" name="artifactId"/>
      <s:textfield label="Version" size="50" name="version"/>
      <s:textfield label="Class / Package" size="50" name="className"/>
      <s:select name="repositoryId" label="Repository ID" list="managedRepositoryList"/>
      <s:hidden name="completeQueryString" value="${completeQueryString}"/>
      <s:hidden name="fromFilterSearch" value="${fromFilterSearch}"/>
      <s:submit label="Go!"/>
    </s:form>
  
    <s:url id="indexUrl" action="index"/>
      <s:a href="%{indexUrl}">
        Quick Search Page
    </s:a>

  </div>

  <script type="text/javascript">
    document.getElementById("filteredSearch_groupId").focus();
  </script>
  <s:actionerror/>

</div> 

</body>
</html>
