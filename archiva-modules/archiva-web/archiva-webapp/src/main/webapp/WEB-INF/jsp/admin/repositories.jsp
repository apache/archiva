<%@ page import="java.io.File" %>
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
<%@ taglib prefix="redback" uri="http://plexus.codehaus.org/redback/taglib-1.0" %>
<%@ taglib prefix="archiva"   uri="/WEB-INF/taglib.tld" %>

<html>
<head>
  <title>Administration - Repositories</title>
  <s:head/>
  <script type="text/javascript" src="<c:url value='/js/jquery-1.3.2.min.js'/>"></script>
  <script type="text/javascript">
  $(document).ready(function(){

 $(".pom").hide();
 $("a.expand").click(function(event){
   event.preventDefault();
   $(this).siblings("pre").toggle("slow");
 });

  });
  </script>
</head>

<body>

<h1>Administration - Repositories</h1>

<div id="contentArea">

  <%-- changed the structure of displaying errorMessages & actionMessages in order for them to be escaped. --%>
  <s:if test="hasActionErrors()">
      <ul>
      <s:iterator value="actionErrors">
          <li><span class="errorMessage"><s:property escape="true" /></span></li>
      </s:iterator>
      </ul>
  </s:if>
  <s:actionmessage />
  <s:if test="hasActionMessages()">
      <ul>
      <s:iterator value="actionMessages">
          <li><span class="actionMessage"><s:property escape="true" /></span></li>
      </s:iterator>
      </ul>
  </s:if>

<div class="admin">
<div class="controls">
  <redback:ifAuthorized permission="archiva-manage-configuration">
    <s:url id="addRepositoryUrl" action="addRepository"/>
    <s:a href="%{addRepositoryUrl}">
      <img src="<c:url value="/images/icons/create.png" />" alt="" width="16" height="16"/>
      Add
    </s:a>
  </redback:ifAuthorized>
</div>
<h2>Managed Repositories</h2>

<c:choose>
<c:when test="${empty (managedRepositories)}">
  <%-- No Managed Repositories. --%>
  <strong>There are no managed repositories configured yet.</strong>
</c:when>
<c:otherwise>
<%-- Display the repositories. --%>

<c:forEach items="${managedRepositories}" var="repository" varStatus="i">
<c:choose>
  <c:when test='${(i.index)%2 eq 0}'>
    <c:set var="rowColor" value="dark" scope="page"/>
  </c:when>
  <c:otherwise>
    <c:set var="rowColor" value="lite" scope="page"/>
  </c:otherwise>
</c:choose>

<div class="repository ${rowColor}">

<div class="controls">
    <%-- TODO: make some icons --%>
  <redback:ifAnyAuthorized permissions="archiva-manage-configuration">
      <s:url id="editRepositoryUrl" encode="true" action="editRepository">
      <s:param name="repoid" value="%{#attr.repository.id}"/>
    </s:url>
    <s:token/>
    <s:url id="deleteRepositoryUrl" encode="true" action="confirmDeleteRepository">
      <s:param name="repoid" value="%{#attr.repository.id}"/>
      <s:param name="struts.token.name">struts.token</s:param>
      <s:param name="struts.token"><s:property value="struts.token"/></s:param>
    </s:url>
    <s:a href="%{editRepositoryUrl}">
      <img src="<c:url value="/images/icons/edit.png" />" alt="" width="16" height="16"/>
      Edit
    </s:a>
    <s:a href="%{deleteRepositoryUrl}">
      <img src="<c:url value="/images/icons/delete.gif" />" alt="" width="16" height="16"/>
      Delete
    </s:a>
  </redback:ifAnyAuthorized>
  <c:url var="rssFeedIconUrl" value="/images/icons/rss-feed.png"/>
  <a href='/archiva/feeds/<c:out value="${repository.id}" />'>
	<img src="${rssFeedIconUrl}" />
  </a>
</div>

<%-- used c:out in displaying EL's for them to be escaped.  --%>
<div style="float: left">
  <img src="<c:url value="/images/archiva-splat-32.gif"/>" alt="" width="32" height="32"/>
</div>

<h3 class="repository"><c:out value="${repository.name}" /></h3>

<table class="infoTable">
<tr>
  <th>Identifier</th>
  <td>
    <code><c:out value="${repository.id}" /></code>
  </td>
</tr>
<tr>
  <th>Name</th>
  <td>
    <code><c:out value="${repository.name}" /></code>
  </td>
</tr>
<tr>
  <th>Directory</th>
  <td><c:out value="${repository.location}" /></td>
</tr>
<c:if test="${!empty (repository.indexDir)}">
	<tr>
	  <th>Index Directory</th>
	  <td><c:out value="${repository.indexDir}" /></td>
	</tr>
</c:if>
<tr>
  <th>WebDAV URL</th>
  <td><a href='<c:out value="${baseUrl}" />/<c:out value="${repository.id}" />/' ><c:out value="${baseUrl}" />/<c:out value="${repository.id}" />/</a></td>
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
<c:if test="${!empty (repositoryToGroupMap[repository.id])}">
  <tr>
    <th>Groups</th>
    <td>
      <c:forEach items="${repositoryToGroupMap[repository.id]}" varStatus="i" var="group">
        <c:out value="${group}" /><c:if test="${!i.last}">,</c:if>
      </c:forEach>
    </td>
  </tr>
</c:if>
<tr>
  <th>Releases Included</th>
  <td class="${repository.releases ? 'donemark' : 'errormark'} booleanIcon"> </td>
</tr>
<tr>
  <th>Snapshots Included</th>
  <td class="${repository.snapshots ? 'donemark' : 'errormark'} booleanIcon"> </td>
</tr>
<c:if test="${repository.snapshots}">
  <tr>
    <th>Delete Released Snapshots</th>
    <td class="${repository.deleteReleasedSnapshots ? 'donemark' : 'errormark'} booleanIcon"> </td>
  </tr>
  <tr>
    <th>Repository Purge By Days Older Than</th>
    <td><c:out value="${repository.daysOlder}" /></td>
  </tr>
  <tr>
    <th>Repository Purge By Retention Count</th>
    <td><c:out value="${repository.retentionCount}" /></td>
  </tr>
</c:if>
<tr>
  <th>Scanned</th>
  <td class="${repository.scanned ? 'donemark' : 'errormark'} booleanIcon"> </td>
</tr>
<c:if test="${repository.scanned}">
  <tr>
    <th>Scanning Cron</th>
    <td><c:out value="${repository.refreshCronExpression}" /></td>
  </tr>
  <tr>
    <th>
      Actions
    </th>
    <td>
      <redback:ifAuthorized permission="archiva-run-indexer">
        <s:form action="indexRepository" theme="simple">
        <s:hidden name="repoid" value="%{#attr.repository.id}"/>
        <table>
          <tr>
            <td><s:checkbox name="scanAll" value="scanAll"/>Process All Artifacts</td>
          </tr>
          <tr>
            <td><s:submit value="Scan Repository Now" id="scanRepoNow%{#attr.repository.id}"/></td>
          </tr>
        </table>
        </s:form>
      </redback:ifAuthorized>
    </td>
  </tr>
  <tr>
    <th>Stats</th>
    <td>
      <c:set var="stats" value="${repositoryStatistics[repository.id]}"/>
      <c:choose>
        <c:when test="${empty (stats)}">
          No Statistics Available.
        </c:when>
        <c:otherwise>
          <table>
            <tr>
              <th>Last Scanned</th>
              <td><c:out value="${stats.scanStartTime}" /></td>
            </tr>
            <tr>
              <th>Duration</th>
              <td><c:out value="${stats.duration}" /> ms</td>
            </tr>
            <tr>
              <th>Total File Count</th>
              <td><c:out value="${stats.totalFileCount}" />
            </tr>
            <tr>
              <th>New Files Found</th>
              <td><c:out value="${stats.newFileCount}" />
            </tr>
          </table>
        </c:otherwise>
      </c:choose>
    </td>
  </tr>
</c:if>
<tr>
  <th>POM Snippet</th>
  <td>
    <archiva:copy-paste-snippet object="${repository}" wrapper="toggle" />
  </td>
</tr>


  <c:set var="str" value="${repository.id}" />
  <jsp:useBean id="str" type="java.lang.String" scope="page"/>
  <c:set var="location" value="${repository.location}"/>
  <jsp:useBean id="location" type="java.lang.String" scope="page"/>

  <c:if
      test='<%= !( (str.equalsIgnoreCase("internal") ) || (str.equalsIgnoreCase( "snapshots" )) ) &&
      new File (new File(location ).getParent() ,str + "-stage" ).exists()%>'>
     <tr>
       <th>
         stage repository location
       </th>
    <td>
      ${repository.location}${'-stage'}
    </td>
  </tr>
    <tr>
      <th>Merge Actions</th>
      <td>
        <redback:ifAuthorized permission="archiva-run-indexer">
          <s:form action="merge" theme="simple">
            <s:hidden name="repoid" value="%{#attr.repository.id}"/>
            <%--<s:hidden name="repository" value="%{repository}"/>--%>
            <table>
              <tr>
                <td><s:submit id="Merge" value="Merge"/></td>
              </tr>

            </table>
          </s:form>
        </redback:ifAuthorized>
      </td>
    </tr>


  </c:if>



</table>

</div>
</c:forEach>

</c:otherwise>
</c:choose>

<div class="controls">
  <redback:ifAuthorized permission="archiva-manage-configuration">
    <s:url id="addRepositoryUrl" action="addRemoteRepository"/>
    <s:a href="%{addRepositoryUrl}">
      <img src="<c:url value="/images/icons/create.png" />" alt="" width="16" height="16"/>
      Add
    </s:a>
  </redback:ifAuthorized>
</div>
<h2>Remote Repositories</h2>

<c:choose>
  <c:when test="${empty (remoteRepositories)}">
    <%-- No Remote Repositories. --%>
    <strong>There are no remote repositories configured yet.</strong>
  </c:when>
  <c:otherwise>
    <%-- Display the repositories. --%>
    <c:forEach items="${remoteRepositories}" var="repository" varStatus="i">
      <c:choose>
        <c:when test='${(i.index)%2 eq 0}'>
          <c:set var="rowColor" value="dark" scope="page"/>
        </c:when>
        <c:otherwise>
          <c:set var="rowColor" value="lite" scope="page"/>
        </c:otherwise>
      </c:choose>

      <div class="repository ${rowColor}">

        <div class="controls">
          <redback:ifAnyAuthorized permissions="archiva-manage-configuration">
              <s:url id="editRepositoryUrl" encode="true" action="editRemoteRepository">
                <s:param name="repoid" value="%{#attr.repository.id}"/>
            </s:url>
            <s:a href="%{editRepositoryUrl}">
              <img src="<c:url value="/images/icons/edit.png" />" alt="" width="16" height="16"/>
              Edit
            </s:a>
            <s:token/>
            <s:url id="deleteRepositoryUrl" encode="true" action="confirmDeleteRemoteRepository">
              <s:param name="repoid" value="%{#attr.repository.id}"/>
              <s:param name="struts.token.name">struts.token</s:param>
              <s:param name="struts.token"><s:property value="struts.token"/></s:param>
            </s:url>
            <s:a href="%{deleteRepositoryUrl}">
              <img src="<c:url value="/images/icons/delete.gif" />" alt="" width="16" height="16"/>
              Delete
            </s:a>
          </redback:ifAnyAuthorized>
        </div>

        <div style="float: left">
          <img src="<c:url value="/images/archiva-world.png"/>" alt="" width="32" height="32"/>
        </div>

        <h3 class="repository"><c:out value="${repository.name}" /></h3>

        <table class="infoTable">
          <tr>
            <th>Identifier</th>
            <td>
              <code><c:out value="${repository.id}" /></code>
            </td>
          </tr>
          <tr>
            <th>Name</th>
            <td>
              <code><c:out value="${repository.name}" /></code>
            </td>
          </tr>
          <tr>
            <th>URL</th>
            <td><c:out value="${repository.url}" /></td>
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
        </table>

      </div>
    </c:forEach>
  </c:otherwise>
</c:choose>

</div>

</div>

</body>
</html>
