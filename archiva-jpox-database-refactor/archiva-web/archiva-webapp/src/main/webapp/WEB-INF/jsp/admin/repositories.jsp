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
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="pss" uri="/plexusSecuritySystem" %>
<%@ taglib prefix="archiva" uri="http://maven.apache.org/archiva" %>

<html>
<head>
  <title>Administration - Repositories</title>
  <ww:head/>
</head>

<body>

<h1>Administration - Repositories</h1>

<div id="contentArea">

<ww:actionerror />
<ww:actionmessage />

<div>
  <div style="float: right">
    <%-- TODO replace with icons --%>
    <pss:ifAuthorized permission="archiva-manage-configuration">
      <ww:url id="addRepositoryUrl" action="addRepository"/>
      <ww:a href="%{addRepositoryUrl}">Add Repository</ww:a>
    </pss:ifAuthorized>
  </div>
  <h2>Managed Repositories</h2>
</div>

<c:choose>
  <c:when test="${empty(model.managedRepositories)}">
    <%-- No Managed Repositories. --%>
    <strong>There are no managed repositories configured yet.</strong>
  </c:when>
  <c:otherwise>
    <%-- Display the repositories. --%>
    
	<c:forEach items="${model.managedRepositories}" var="repository" varStatus="i">
  
      <div>

        <div style="float: right">
          <%-- TODO: make some icons --%>
          <pss:ifAnyAuthorized permissions="archiva-manage-configuration">
            <ww:url id="editRepositoryUrl" action="editRepository">
              <ww:param name="repoid" value="%{'${repository.id}'}" />
            </ww:url>
            <ww:url id="deleteRepositoryUrl" action="deleteRepository" method="confirm">
              <ww:param name="repoid" value="%{'${repository.id}'}" />
            </ww:url>
            <ww:a href="%{editRepositoryUrl}">Edit Repository</ww:a>
            <ww:a href="%{deleteRepositoryUrl}">Delete Repository</ww:a>
          </pss:ifAnyAuthorized>
        </div>
        
        <h3>${repository.name}</h3>
        
        <table class="infoTable">
          <tr>
            <th>Identifier</th>
            <td>
              <code>${repository.id}</code>
            </td>
          </tr>
          <tr>
            <th>Directory</th>
            <td>${repository.directory} 
            <c:if test="${not(repository.directoryExists)}">
              <span class="missing">Directory Does Not Exist</span>
            </c:if>
            </td>
          </tr>
          <tr>
            <th>WebDAV URL</th>
            <td><a href="${model.baseUrl}/${repository.id}/">${model.baseUrl}/${repository.id}/</a></td>
          </tr>
          <tr>
            <th>Type</th>
              <%-- TODO: can probably just use layout appended to a key prefix in i18n to simplify this --%>
            <td>
              <c:choose>
                <c:when test="${repository.layout == 'default'}">
                  Maven 2.x Repository
                </c:when>
                <c:otherwise>
                  Maven 1.x Repository
                </c:otherwise>
              </c:choose>
            </td>
          </tr>
          <tr>
            <th>Releases Included</th>
            <td class="${repository.releases ? 'donemark' : 'errormark'} booleanIcon"> ${repository.releases}</td>
          </tr>
          <tr>
            <th>Snapshots Included</th>
            <td class="${repository.snapshots ? 'donemark' : 'errormark'} booleanIcon"> ${repository.snapshots}</td>
          </tr>
          <tr>
            <th>Indexed</th>
            <td class="${repository.indexed ? 'donemark' : 'errormark'} booleanIcon"> ${repository.indexed}</td>
          </tr>
          <c:if test="${repository.indexed}">
            <tr>
              <th>Indexing Cron</th>
              <td>${repository.refreshCronExpression}</td>
            </tr>
            <tr>
              <th>Stats</th>
              <td>
                <div style="float: right">
                  <pss:ifAuthorized permission="archiva-run-indexer">
                    <ww:url id="indexRepositoryUrl" action="indexRepository">
                      <ww:param name="repoid" value="%{'${repository.id}'}" />
                    </ww:url>
                    <ww:a href="%{indexRepositoryUrl}">Index Repository</ww:a>
                  </pss:ifAuthorized>
                </div>
                <c:choose>
                  <c:when test="${empty(repository.stats)}">
                    Never indexed.
                  </c:when>
                  <c:otherwise>
                    <table>
                      <tr>
                        <th>Last Indexed</th>
                        <td>${repository.stats.whenStarted}</td>
                      </tr>
                      <tr>
                        <th>Duration</th>
                        <td>${repository.stats.duration} ms</td>
                      </tr>
                      <tr>
                        <th>Total File Count</th>
                        <td>${repository.stats.totalFileCount}
                      </tr>
                      <tr>
                        <th>New Files Found</th>
                        <td>${repository.stats.newFileCount}
                      </tr>
                    </table>
                  </c:otherwise>
                </c:choose>
              </td>
            </tr>
          </c:if>
          <tr>
            <th>POM Snippet</th>
            <td><a href="#" onclick="Effect.toggle('repoPom${repository.id}','slide'); return false;">Show POM Snippet</a><br/>
<pre class="pom" style="display: none;" id="repoPom${repository.id}"><code><archiva:copy-paste-snippet object="${repository}" /></code></pre>
            </td>
          </tr>
        </table>
      
      </div>
	</c:forEach>
	
  </c:otherwise>
</c:choose>

</div>

</body>
</html>
