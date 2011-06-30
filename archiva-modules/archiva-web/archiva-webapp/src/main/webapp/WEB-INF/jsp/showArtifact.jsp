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
  <script type="text/javascript" src="<c:url value='/js/jquery-1.6.1.min.js'/>"></script>
  <script type="text/javascript" src="<c:url value='/js/jquery-ui-1.8.14.custom.min.js'/>"></script>
  <script type="text/javascript">
	$(function() {
		$("#accordion").accordion({autoHeight:false});
	});
	</script>
  <link rel="stylesheet" href="<c:url value='/css/no-theme/jquery-ui-1.8.14.custom.css'/>" type="text/css" media="all"/>
</head>

<body>

<s:set name="model" value="model"/>
<c:set var="mavenFacet" value="${model.facets['org.apache.archiva.metadata.repository.storage.maven2.project']}" />

<c:choose>
  <c:when test="${mavenFacet.packaging == 'maven-plugin'}">
    <c:url var="imageUrl" value="/images/mavenplugin.gif"/>
    <c:set var="packageName">Maven Plugin</c:set>
  </c:when>
  <c:when test="${mavenFacet.packaging == 'pom'}">
    <c:url var="imageUrl" value="/images/pom.gif"/>
    <c:set var="packageName">POM</c:set>
  </c:when>
  <%-- These types aren't usually set in the POM yet, so we fudge them for the well known ones --%>
  <c:when test="${mavenFacet.packaging == 'maven-archetype' or mavenFacet.groupId == 'org.apache.maven.archetypes'}">
    <c:url var="imageUrl" value="/images/archetype.gif"/>
    <c:set var="packageName">Maven Archetype</c:set>
  </c:when>
  <c:when test="${mavenFacet.packaging == 'maven-skin' or mavenFacet.groupId == 'org.apache.maven.skins'}">
    <c:url var="imageUrl" value="/images/skin.gif"/>
    <c:set var="packageName">Maven Skin</c:set>
  </c:when>
  <%-- Must be last so that the above get picked up if possible --%>
  <c:when test="${mavenFacet.packaging == 'jar'}">
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
      ${mavenFacet.artifactId}
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
      <my:currentWWUrl url="${url}" useParams="true">Info</my:currentWWUrl>
      <c:set var="url">
        <s:url action="showArtifactDependencies">
          <s:param name="groupId" value="%{groupId}"/>
          <s:param name="artifactId" value="%{artifactId}"/>
          <s:param name="version" value="%{version}"/>
        </s:url>
      </c:set>
      <my:currentWWUrl url="${url}" useParams="true">Dependencies</my:currentWWUrl>
      <c:set var="url">
        <s:url action="showArtifactDependencyTree">
          <s:param name="groupId" value="%{groupId}"/>
          <s:param name="artifactId" value="%{artifactId}"/>
          <s:param name="version" value="%{version}"/>
        </s:url>
      </c:set>
      <my:currentWWUrl url="${url}" useParams="true">Dependency Tree</my:currentWWUrl>
      <c:set var="url">
        <s:url action="showArtifactDependees">
          <s:param name="groupId" value="%{groupId}"/>
          <s:param name="artifactId" value="%{artifactId}"/>
          <s:param name="version" value="%{version}"/>
        </s:url>
      </c:set>
      <my:currentWWUrl url="${url}" useParams="true">Used By</my:currentWWUrl>
      <c:set var="url">
        <s:url action="showArtifactMailingLists">
          <s:param name="groupId" value="%{groupId}"/>
          <s:param name="artifactId" value="%{artifactId}"/>
          <s:param name="version" value="%{version}"/>
        </s:url>
      </c:set>
      <my:currentWWUrl url="${url}" useParams="true">Mailing Lists</my:currentWWUrl>
      <c:set var="url">
        <s:url action="showProjectMetadata">
          <s:param name="groupId" value="%{groupId}"/>
          <s:param name="artifactId" value="%{artifactId}"/>
          <s:param name="version" value="%{version}"/>
        </s:url>
      </c:set>
      <my:currentWWUrl url="${url}" useParams="true">Metadata</my:currentWWUrl>
      <%-- TODO
      <redback:ifAnyAuthorized permissions="archiva-access-reports">
        <c:set var="url">
          <s:url action="showArtifactReports">
            <s:param name="groupId" value="%{groupId}"/>
            <s:param name="artifactId" value="%{artifactId}"/>
            <s:param name="version" value="%{version}"/>
          </s:url>
        </c:set>
        <my:currentWWUrl url="${url}" useParams="true">Reports</my:currentWWUrl>
      </redback:ifAnyAuthorized>
        --%>
      
    </span>
  </div>

  <div id="download">
    <h2>Download</h2>

    <div id="accordion">
      <c:forEach items="${snapshotVersions}" var="v">
        <p><a href="#">${v}</a></p>
        <div>
          <table cellpadding="0" cellspacing="0" border="0" width="100%">
            <tbody>
            <c:forEach items="${artifacts[v]}" var="a">
              <c:choose>
                <c:when test="${a.type == 'maven-plugin'}">
                  <c:url var="imageUrl" value="/images/download-type-maven-plugin.png"/>
                  <c:set var="packageName">Maven Plugin</c:set>
                </c:when>
                <c:when test="${a.type == 'pom'}">
                  <c:url var="imageUrl" value="/images/download-type-pom.png"/>
                  <c:set var="packageName">POM</c:set>
                </c:when>
                <%-- These types aren't usually set in the POM yet, so we fudge them for the well known ones --%>
                <c:when test="${a.type == 'maven-archetype' or a.namespace == 'org.apache.maven.archetypes'}">
                  <c:url var="imageUrl" value="/images/download-type-archetype.png"/>
                  <c:set var="packageName">Maven Archetype</c:set>
                </c:when>
                <c:when test="${a.type == 'maven-skin' or a.namespace == 'org.apache.maven.skins'}">
                  <c:url var="imageUrl" value="/images/download-type-skin.png"/>
                  <c:set var="packageName">Maven Skin</c:set>
                </c:when>
                <c:when test="${a.type == 'java-source'}">
                  <c:url var="imageUrl" value="/images/download-type-jar.png"/>
                  <c:set var="packageName">Java Sources</c:set>
                </c:when>
                <c:when test="${a.type == 'javadoc'}">
                  <c:url var="imageUrl" value="/images/download-type-other.png"/>
                  <c:set var="packageName">JavaDoc Archive</c:set>
                </c:when>
                <c:when test="${a.type == 'library'}">
                  <c:url var="imageUrl" value="/images/download-type-other.png"/>
                  <c:set var="packageName">.NET Library</c:set>
                </c:when>
                <%-- TODO: other NPanday types, and move this code into the plugin somehow --%>
                <%-- Must be last so that the above get picked up if possible --%>
                <c:when test="${a.type == 'jar'}">
                  <c:url var="imageUrl" value="/images/download-type-jar.png"/>
                  <c:set var="packageName">JAR</c:set>
                </c:when>
                <c:otherwise>
                  <c:url var="imageUrl" value="/images/download-type-other.png"/>
                  <c:set var="packageName">${a.type}</c:set>
                </c:otherwise>
              </c:choose>
              <c:url var="url" value="/repository/${a.repositoryId}/${a.path}" />
              <tr>
                <td><a href="${url}" title="Download ${a.id}"><img src="${imageUrl}" alt="" width="24" height="24"/></a></td>
                <td class="type"><a href="${url}" title="Download ${a.id}">${packageName}</a></td>
                <td class="size">${a.size}</td>
              </tr>
            </c:forEach>
            </tbody>
          </table>
        </div>
      </c:forEach>
    </div>
  </div>

  <%-- TODO: perhaps using ajax? --%>
  <%-- TODO: panels? this is ugly as is --%>
  <div id="tabArea">
    <c:choose>
      <c:when test="${genericMetadata != null}">
        <%@ include file="/WEB-INF/jsp/include/projectMetadata.jspf" %>
      </c:when>
      <c:when test="${dependencies != null}">
        <%@ include file="/WEB-INF/jsp/include/artifactDependencies.jspf" %>
      </c:when>
      <c:when test="${dependencyTree}">
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

    <s:if test="hasActionMessages()">
      <div id="messagesinfo">
        <s:actionmessage />
      </div>
    </s:if>
    <s:if test="hasActionErrors()">
      <div id="messages">
        <s:actionerror/>
      </div>
    </s:if>
  </div>
</div>
</body>
</html>
