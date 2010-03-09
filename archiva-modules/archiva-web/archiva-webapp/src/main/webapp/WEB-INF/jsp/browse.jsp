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
<%@ taglib prefix="archiva" uri="/WEB-INF/taglib.tld" %>
<%@ taglib prefix="redback" uri="http://plexus.codehaus.org/redback/taglib-1.0"  %>

<html>
<head>
  <title>Browse Repository</title>
  <s:head/>
  
  <script type="text/javascript" src="<c:url value='/js/jquery-1.3.2.min.js'/>"></script>
  <script type="text/javascript">
    $(document).ready(function(){
    
    $("table.infoTable").hide();
    $("a.expand").click(function(event){
      event.preventDefault();
      $(this).next().toggle("slow");
    });
  });
  </script>
  
</head>

<body>

<h1>Browse Repository</h1>

<div id="contentArea">
  <c:if test="${not empty groupId}">
    <p>
      <archiva:groupIdLink var="${groupId}" includeTop="true" />
      <c:if test="${not empty artifactId}">
        <strong>${artifactId}</strong>
      </c:if>      
    </p>
  </c:if>

  <c:if test="${not empty namespaces}">
    <div id="nameColumn">
      <h2>Groups</h2>
      <ul>
        <c:forEach items="${namespaces}" var="groupId">
          <c:set var="url">
            <s:url action="browseGroup" namespace="/">
              <s:param name="groupId" value="%{#attr.groupId}"/>
            </s:url>
          </c:set>
          <li><a href="${url}">${groupId}/</a></li>
        </c:forEach>
      </ul>
    </div>
  </c:if>
  
  <c:if test="${not empty projectIds}">
    <div id="nameColumn">
      <h2>Artifacts</h2>
      <ul>
        <c:url var="rssFeedIconUrl" value="/images/icons/rss-feed.png"/>
        <c:forEach items="${projectIds}" var="artifactId">
          <c:set var="url">
            <s:url action="browseArtifact" namespace="/">
              <s:param name="groupId" value="%{#attr.groupId}"/>
              <s:param name="artifactId" value="%{#attr.artifactId}"/>
            </s:url>
          </c:set>
          <c:url var="rssUrl" value="/feeds/${groupId}/${artifactId}"/>
          <li>
          <a href="${url}">${artifactId}/</a>
          <a href="${rssUrl}">
            <img src="${rssFeedIconUrl}" />
          </a>
          </li>
        </c:forEach>
      </ul>
    </div>
  </c:if>  
  
  <c:if test="${not empty projectVersions}">
    <%-- show shared project information (MRM-1041) TODO - share JSP code with artifactInfo.jspf --%>

    <c:set var="mavenFacet" value="${sharedModel.facets['org.apache.archiva.metadata.repository.storage.maven2.project']}" />
    <h2>Versions</h2>
    <div id="nameColumn" class="versions">  
      <a class="expand" href="#">Artifact Info</a>      
      <table class="infoTable">        
        <tr>
          <th>Group ID</th>
          <td>${mavenFacet.groupId}</td>
        </tr>
        <tr>
          <th>Artifact ID</th>
          <td>${mavenFacet.artifactId}</td>
        </tr>        
        <c:if test="${(mavenFacet.packaging != null) && (!empty mavenFacet.packaging)}">
        <tr>
          <th>Packaging</th>
          <td><code>${mavenFacet.packaging}</code></td>
        </tr>
        </c:if>
        <c:if test="${(sharedModel.name != null) && (!empty sharedModel.name)}">
        <tr>
          <th>Name</th>
          <td><code>${sharedModel.name}</code></td>
        </tr>
        </c:if>
        <c:if test="${sharedModel.organization != null}">
        <tr>
          <th>Organisation</th>
          <td>
            <c:choose>
              <c:when test="${(sharedModel.organization.url != null) && (!empty sharedModel.organization.url)}">
                <a href="${sharedModel.organization.url}">${sharedModel.organization.name}</a>
              </c:when>
              <c:otherwise>
                ${sharedModel.organization.name}
              </c:otherwise>
            </c:choose>
          </td>
        </tr>
        </c:if>
        <c:if test="${sharedModel.issueManagement != null}">
        <tr>
          <th>Issue Tracker</th>
          <td>
            <c:choose>
              <c:when test="${!empty (sharedModel.issueManagement.url)}">
                <a href="${sharedModel.issueManagement.url}">${sharedModel.issueManagement.system}</a>
              </c:when>
              <c:otherwise>
                ${sharedModel.issueManagement.system}
              </c:otherwise>
            </c:choose>
          </td>
        </tr>
        </c:if>
        <c:if test="${sharedModel.ciManagement != null}">
        <tr>
          <th>Continuous Integration</th>
          <td>
            <c:choose>
              <c:when test="${!empty (sharedModel.ciManagement.url)}">
                <a href="${sharedModel.ciManagement.url}">${sharedModel.ciManagement.system}</a>
              </c:when>
              <c:otherwise>
                ${sharedModel.ciManagement.system}
              </c:otherwise>
            </c:choose>
          </td>
        </tr>
        </c:if>
      </table>
    </div>      

    <ul>
      <c:forEach items="${projectVersions}" var="version">
        <c:set var="url">
          <s:url action="showArtifact" namespace="/">
            <s:param name="groupId" value="%{#attr.groupId}"/>
            <s:param name="artifactId" value="%{#attr.artifactId}"/>
            <s:param name="version" value="%{#attr.version}"/>
          </s:url>
        </c:set>
        <li><a href="${url}">${version}/</a></li>
      </c:forEach>
    </ul>    
  </c:if>  

</div>

</body>
</html>
