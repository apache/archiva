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
<%@ taglib prefix="archiva"   uri="/WEB-INF/taglib.tld" %>
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
  <c:if test="${not empty results.selectedGroupId}">
    <p>
      <archiva:groupIdLink var="${results.selectedGroupId}" includeTop="true" />
      <c:if test="${not empty results.selectedArtifactId}">
        <strong>${artifactId}</strong>
      </c:if>      
    </p>
  </c:if>

  <c:if test="${not empty results.groupIds}">
    <div id="nameColumn">
      <h2>Groups</h2>
      <ul>
        <c:forEach items="${results.groupIds}" var="groupId">
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
  
  <c:if test="${not empty results.artifacts}">
    <div id="nameColumn">
      <h2>Artifacts</h2>
      <ul>
        <c:url var="rssFeedIconUrl" value="/images/icons/rss-feed.png"/>
        <c:forEach items="${results.artifacts}" var="artifactId">
          <c:set var="url">
            <s:url action="browseArtifact" namespace="/">
              <s:param name="groupId" value="%{#attr.results.selectedGroupId}"/>
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
  
  <c:if test="${not empty results.versions}">
    <%-- show shared project information (MRM-1041) --%>    
    
    <h2>Versions</h2>
    <div id="nameColumn" class="versions">  
      <a class="expand" href="#">Artifact Info</a>      
      <table class="infoTable">        
        <tr>
          <th>Group ID</th>
          <td>${sharedModel.groupId}</td>
        </tr>
        <tr>
          <th>Artifact ID</th>
          <td>${sharedModel.artifactId}</td>
        </tr>        
        <c:if test="${(sharedModel.packaging != null) && (!empty sharedModel.packaging)}">
        <tr>
          <th>Packaging</th>
          <td><code>${sharedModel.packaging}</code></td>
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
                <a href="${sharedModel.organization.url}">${sharedModel.organization.organizationName}</a>
              </c:when>
              <c:otherwise>
                ${sharedModel.organization.organizationName}
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
              <c:when test="${!empty (sharedModel.issueManagement.issueManagementUrl)}">
                <a href="${sharedModel.issueManagement.issueManagementUrl}">${sharedModel.issueManagement.system}</a>
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
              <c:when test="${!empty (sharedModel.ciManagement.ciUrl)}">
                <a href="${sharedModel.ciManagement.ciUrl}">${sharedModel.ciManagement.system}</a>
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
      <c:forEach items="${results.versions}" var="version">
        <c:set var="url">
          <s:url action="showArtifact" namespace="/">
            <s:param name="groupId" value="%{#attr.results.selectedGroupId}"/>
            <s:param name="artifactId" value="%{#attr.results.selectedArtifactId}"/>
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