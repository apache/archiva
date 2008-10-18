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
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="redback" uri="http://plexus.codehaus.org/redback/taglib-1.0" %>
<%@ taglib prefix="archiva" uri="http://archiva.apache.org" %>

<html>
<head>
  <title>Administration - Repository Groups</title>
  <s:head/>
</head>

<body>

<h1>Administration - Repository Groups</h1>

<c:url var="iconDeleteUrl" value="/images/icons/delete.gif"/>
<c:url var="iconEditUrl" value="/images/icons/edit.png"/>
<c:url var="iconCreateUrl" value="/images/icons/create.png"/>
<c:url var="iconUpUrl" value="/images/icons/up.gif"/>
<c:url var="iconDownUrl" value="/images/icons/down.gif"/>

<div id="contentArea">

<s:actionerror/>
<s:actionmessage/>

<div align="right">
  <redback:ifAnyAuthorized permissions="archiva-manage-configuration">
    <s:form action="addRepositoryGroup" namespace="/admin">
      <span class="label">Identifier<span style="color:red">*</span>:</span> 
      <s:textfield size="10" label="Identifier" theme="simple" name="repositoryGroup.id"/>
      <s:submit value="Add Group" theme="simple" cssClass="button"/>
    </s:form>
  </redback:ifAnyAuthorized>
</div>

<h2>Repository Groups</h2>

<c:choose>
<c:when test="${empty (repositoryGroups)}">
  <strong>No Repository Groups Defined.</strong>
</c:when>
<c:otherwise>

<div class="admin">

<c:forEach items="${repositoryGroups}" var="repositoryGroup" varStatus="i">

<div class="repoGroup">
  <div class="managedRepo">
    
    <div style="float:right">
      <s:url id="deleteRepositoryGroupUrl" action="confirmDeleteRepositoryGroup">
        <s:param name="repoGroupId" value="%{#attr.repositoryGroup.key}" />
      </s:url>
      <s:a href="%{deleteRepositoryGroupUrl}" cssClass="delete">
        <img src="${iconDeleteUrl}"/>
      </s:a>
    </div>
    
    <img src="<c:url value="/images/archiva-splat-32.gif"/>"/>
    <p class="id">${repositoryGroup.key}</p>
    <p><a href="${baseUrl}/${repositoryGroup.key}/">${baseUrl}/${repositoryGroup.key}/</a></p>
  </div>

  <c:if test="${!empty (groupToRepositoryMap[repositoryGroup.key])}">
  <div class="repos">
    <s:form name="form%{#attr.i}" action="addRepositoryToGroup" namespace="/admin" validate="true">
      <s:hidden name="repoGroupId" value="%{#attr.repositoryGroup.key}"/>
      <s:select list="%{#attr.groupToRepositoryMap[#attr.repositoryGroup.key]}" name="repoId" theme="simple"/>
      <s:submit value="Add Repository" theme="simple" cssClass="button"/>
    </s:form>
  </div>
  </c:if>
  
  <c:set var="numberOfRepos" value="${fn:length(repositoryGroup.value.repositories)}" />

  <c:forEach items="${repositoryGroup.value.repositories}" var="repository" varStatus="r">
  
  <c:choose>
    <c:when test='${(r.index)%2 eq 0}'>
      <c:set var="rowColor" value="dark" scope="page"/>
    </c:when>
    <c:otherwise>
      <c:set var="rowColor" value="lite" scope="page"/>
    </c:otherwise>
  </c:choose>

  <div class="connector ${rowColor}"> 
    <div class="controls">
      <redback:ifAnyAuthorized permissions="archiva-manage-configuration">
        <s:url id="sortDownRepositoryUrl" action="sortDownRepositoryFromGroup">
          <s:param name="repoGroupId" value="%{#attr.repositoryGroup.key}"/>
          <s:param name="targetRepo" value="managedRepositories['%{#attr.repository}'].id"/>
        </s:url>
        <s:url id="sortUpRepositoryUrl" action="sortUpRepositoryFromGroup">
          <s:param name="repoGroupId" value="%{#attr.repositoryGroup.key}"/>
          <s:param name="targetRepo" value="managedRepositories['%{#attr.repository}'].id"/>
        </s:url>
        <s:url id="removeRepositoryUrl" action="removeRepositoryFromGroup">
          <s:param name="repoGroupId" value="%{#attr.repositoryGroup.key}"/>
          <s:param name="repoId" value="managedRepositories[%{#attr.repository}].id"/>
        </s:url>
        <c:if test="${r.count > 1}">
          <s:a href="%{sortUpRepositoryUrl}" cssClass="up" title="Move Repository Up">
            <img src="${iconUpUrl}"/>
          </s:a>
        </c:if>
        <c:if test="${r.count < numberOfRepos}">
          <s:a href="%{sortDownRepositoryUrl}" cssClass="down" title="Move Repository Down">
            <img src="${iconDownUrl}"/>
          </s:a>
        </c:if>
        <s:a href="%{removeRepositoryUrl}" cssClass="delete" title="Delete Repository">
          <img src="${iconDeleteUrl}"/>
        </s:a>
      </redback:ifAnyAuthorized>
    </div>
  
    <h4>Repository</h4>
    
    <div class="managedRepo">
      <img src="<c:url value="/images/archiva-splat-32.gif"/>"/>
      <p class="id">${repository}</p>
      <p class="name">${managedRepositories[repository].name}</p>
      <p class="url"><a href="${baseUrl}/${managedRepositories[repository].id}/">${baseUrl}/${managedRepositories[repository].id}</a></p>
    </div>
  </div> <%-- repository --%> 
  </c:forEach>

</div> <%-- repository group --%>
</c:forEach>
</div> <%-- admin --%>

</c:otherwise>
</c:choose>
</div> <%-- content area --%>

</body>
</html>

