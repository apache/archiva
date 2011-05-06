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

<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="my" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="archiva"   uri="/WEB-INF/taglib.tld" %>
<%@ taglib prefix="redback" uri="http://plexus.codehaus.org/redback/taglib-1.0" %>

<html>
<head>
  <title>Browse Repository</title>
  <s:head/>
</head>

<body>

<s:set name="model" value="model"/>
<c:choose>
  <c:when test="${model.packaging == 'maven-plugin'}">
    <c:url var="imageUrl" value="/images/mavenplugin.gif"/>
    <c:set var="packageName">Maven Plugin</c:set>
  </c:when>
  <c:when test="${model.packaging == 'pom'}">
    <c:url var="imageUrl" value="/images/pom.gif"/>
    <c:set var="packageName">POM</c:set>
  </c:when>
  <%-- These types aren't usually set in the POM yet, so we fudge them for the well known ones --%>
  <c:when test="${model.packaging == 'maven-archetype' or model.groupId == 'org.apache.maven.archetypes'}">
    <c:url var="imageUrl" value="/images/archetype.gif"/>
    <c:set var="packageName">Maven Archetype</c:set>
  </c:when>
  <c:when test="${model.packaging == 'maven-skin' or model.groupId == 'org.apache.maven.skins'}">
    <c:url var="imageUrl" value="/images/skin.gif"/>
    <c:set var="packageName">Maven Skin</c:set>
  </c:when>
  <%-- Must be last so that the above get picked up if possible --%>
  <c:when test="${model.packaging == 'jar'}">
    <c:url var="imageUrl" value="/images/jar.gif"/>
    <c:set var="packageName">JAR</c:set>
  </c:when>
  <c:otherwise>
    <c:url var="imageUrl" value="/images/other.gif"/>
    <c:set var="packageName"></c:set>
  </c:otherwise>
</c:choose>
<img src="${imageUrl}" width="66" height="66" alt="${packageName}" title="${packageName}" style="float: left"/>

<h1>
  <c:choose>
    <c:when test="${empty (model.name)}">
      ${model.artifactId}
    </c:when>
    <c:otherwise>
      ${model.name}
    </c:otherwise>
  </c:choose>
</h1>

<div id="contentArea">
  <div id="tabs">
    <span>
      <c:set var="url">
        <s:url action="showArtifact">
          <s:param name="groupId" value="%{groupId}"/>
          <s:param name="artifactId" value="%{artifactId}"/>
          <s:param name="version" value="%{version}"/>
        </s:url>
      </c:set>
      <my:currentWWUrl url="${url}">Info</my:currentWWUrl>
      <c:set var="url">
        <s:url action="showArtifactDependencies">
          <s:param name="groupId" value="%{groupId}"/>
          <s:param name="artifactId" value="%{artifactId}"/>
          <s:param name="version" value="%{version}"/>
        </s:url>
      </c:set>
      <my:currentWWUrl url="${url}">Dependencies</my:currentWWUrl>

      <%-- disabled for now to avoid too much cpu usage (see MRM-1457)
      <c:set var="url">
        <s:url action="showArtifactDependencyTree">
          <s:param name="groupId" value="%{groupId}"/>
          <s:param name="artifactId" value="%{artifactId}"/>
          <s:param name="version" value="%{version}"/>
        </s:url>
      </c:set>
      <my:currentWWUrl url="${url}">Dependency Tree</my:currentWWUrl>
      --%>

      <c:set var="url">
        <s:url action="showArtifactDependees">
          <s:param name="groupId" value="%{groupId}"/>
          <s:param name="artifactId" value="%{artifactId}"/>
          <s:param name="version" value="%{version}"/>
        </s:url>
      </c:set>
      <my:currentWWUrl url="${url}">Used By</my:currentWWUrl>
      <c:set var="url">
        <s:url action="showArtifactMailingLists">
          <s:param name="groupId" value="%{groupId}"/>
          <s:param name="artifactId" value="%{artifactId}"/>
          <s:param name="version" value="%{version}"/>
        </s:url>
      </c:set>
      <my:currentWWUrl url="${url}">Mailing Lists</my:currentWWUrl>
      <%-- POSTPONED to 1.0-alpha-2
      <redback:ifAnyAuthorized permissions="archiva-access-reports">
        <c:set var="url">
          <s:url action="showArtifactReports">
            <s:param name="groupId" value="%{groupId}"/>
            <s:param name="artifactId" value="%{artifactId}"/>
            <s:param name="version" value="%{version}"/>
          </s:url>
        </c:set>
        <my:currentWWUrl url="${url}">Reports</my:currentWWUrl>
      </redback:ifAnyAuthorized>
        --%>
      
    </span>
  </div>

  <div class="sidebar3">
    <archiva:downloadArtifact groupId="${model.groupId}" artifactId="${model.artifactId}" version="${model.version}"/>
  </div>

  <%-- TODO: perhaps using ajax? --%>
  <%-- TODO: panels? this is ugly as is --%>
  <div id="tabArea">
    <c:choose>
      <c:when test="${dependencies != null}">
        <%@ include file="/WEB-INF/jsp/include/artifactDependencies.jspf" %>
      </c:when>
      <c:when test="${dependencyTree != null}">
        <%@ include file="/WEB-INF/jsp/include/dependencyTree.jspf" %>
      </c:when>
      <c:when test="${dependees != null}">
        <%@ include file="/WEB-INF/jsp/include/projectDependees.jspf" %>
      </c:when>
      <c:when test="${mailingLists != null}">
        <%@ include file="/WEB-INF/jsp/include/mailingLists.jspf" %>
      </c:when>
      <c:when test="${reports != null}">
        <%@ include file="/WEB-INF/jsp/include/artifactReports.jspf" %>
      </c:when>
      <c:otherwise>
        <%@ include file="/WEB-INF/jsp/include/artifactInfo.jspf" %>
      </c:otherwise>
    </c:choose>
  </div>
</div>

</body>
</html>
