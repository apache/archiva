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

<%@ taglib prefix="decorator" uri="http://www.opensymphony.com/sitemesh/decorator" %>
<%@ taglib prefix="page"      uri="http://www.opensymphony.com/sitemesh/page" %>
<%@ taglib prefix="ww"        uri="/webwork" %>
<%@ taglib prefix="c"         uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="redback"   uri="http://plexus.codehaus.org/redback/taglib-1.0"  %>
<%@ taglib prefix="my"        tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="archiva"   uri="http://archiva.apache.org" %>

<page:applyDecorator name="default">

<html>
<head>
  <title>Browse Repository</title>
  <ww:head/>
</head>

<body>

<ww:set name="model" value="model"/>
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
    <c:when test="${empty(model.name)}">
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
        <ww:url action="showArtifact">
          <ww:param name="groupId" value="%{groupId}"/>
          <ww:param name="artifactId" value="%{artifactId}"/>
          <ww:param name="version" value="%{version}"/>
        </ww:url>
      </c:set>
      <my:currentWWUrl url="${url}">Info</my:currentWWUrl>
      <c:set var="url">
        <ww:url action="showArtifactDependencies">
          <ww:param name="groupId" value="%{groupId}"/>
          <ww:param name="artifactId" value="%{artifactId}"/>
          <ww:param name="version" value="%{version}"/>
        </ww:url>
      </c:set>
      <my:currentWWUrl url="${url}">Dependencies</my:currentWWUrl>
      <c:set var="url">
        <ww:url action="showArtifactDependencyTree">
          <ww:param name="groupId" value="%{groupId}"/>
          <ww:param name="artifactId" value="%{artifactId}"/>
          <ww:param name="version" value="%{version}"/>
        </ww:url>
      </c:set>
      <my:currentWWUrl url="${url}">Dependency Tree</my:currentWWUrl>
      <c:set var="url">
        <ww:url action="showArtifactDependees">
          <ww:param name="groupId" value="%{groupId}"/>
          <ww:param name="artifactId" value="%{artifactId}"/>
          <ww:param name="version" value="%{version}"/>
        </ww:url>
      </c:set>
      <my:currentWWUrl url="${url}">Used By</my:currentWWUrl>
      <c:set var="url">
        <ww:url action="showArtifactMailingLists">
          <ww:param name="groupId" value="%{groupId}"/>
          <ww:param name="artifactId" value="%{artifactId}"/>
          <ww:param name="version" value="%{version}"/>
        </ww:url>
      </c:set>
      <my:currentWWUrl url="${url}">Mailing Lists</my:currentWWUrl>
      <%-- POSTPONED to 1.0-alpha-2
      <redback:ifAnyAuthorized permissions="archiva-access-reports">
        <c:set var="url">
        <ww:url action="showArtifactReports">
          <ww:param name="groupId" value="%{groupId}"/>
          <ww:param name="artifactId" value="%{artifactId}"/>
          <ww:param name="version" value="%{version}"/>
        </ww:url>
      </c:set>
      <my:currentWWUrl url="${url}">Reports</my:currentWWUrl>
      </redback:ifAnyAuthorized>
        --%>
      
    </span>
  </div>

<div class="sidebar3">
  <archiva:downloadArtifact groupId="${groupId}" artifactId="${artifactId}" version="${model.version}" />
</div>
  
  <decorator:body />
</div>

</body>
</html>

</page:applyDecorator>
