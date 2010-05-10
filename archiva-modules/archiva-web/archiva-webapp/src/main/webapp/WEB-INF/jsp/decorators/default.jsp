<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">

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
<%@ taglib uri="http://www.opensymphony.com/sitemesh/decorator" prefix="decorator" %>
<%@ taglib uri="/struts-tags" prefix="s" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="my" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="redback" uri="http://plexus.codehaus.org/redback/taglib-1.0" %>
<%@ page import="org.apache.maven.archiva.web.startup.ArchivaVersion" %>
<%@ page import="java.util.Calendar" %>

<html>
<head>
  <title>Apache Archiva \
    <decorator:title default="Apache Archiva"/>
  </title>

  <link rel="stylesheet" href="<c:url value="/css/maven-base.css"/>" type="text/css" media="all"/>
  <link rel="stylesheet" href="<c:url value="/css/maven-theme.css"/>" type="text/css" media="all"/>
  <link rel="stylesheet" href="<c:url value="/css/redback/table.css"/>" type="text/css" media="all"/>
  <link rel="stylesheet" href="<c:url value="/css/site.css"/>" type="text/css" media="all"/>
  <link rel="stylesheet" href="<c:url value="/css/print.css"/>" type="text/css" media="print"/>
  <link rel="shortcut icon" href="<c:url value="/favicon.ico" />"/>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <decorator:head />
</head>

<body onload="<decorator:getProperty property="body.onload" />" class="composite">
<div id="breadcrumbs">
  <div class="xright">
    <%@ include file="/WEB-INF/jsp/redback/include/securityLinks.jsp" %>
  </div>
  <div class="clear">
    <hr/>
  </div>
</div>


<div id="topSearchBox">
    <s:form method="get" action="quickSearch" namespace="/" validate="true">
        <s:textfield label="Search for" size="30" name="q"/>
    </s:form>
</div>

<div id="leftColumn">
  <div id="navcolumn">
    <s:action namespace="/components" name="companyInfo" executeResult="true"/>
    <h5>Find</h5>
    <ul>
      <li class="none">
        <my:currentWWUrl action="index" namespace="/">Search</my:currentWWUrl>
      </li>

      <s:if test="%{#application['uiOptions'].showFindArtifacts}">
        <li class="none">
          <my:currentWWUrl action="findArtifact" namespace="/">Find Artifact</my:currentWWUrl>
        </li>
      </s:if>

      <li class="none">
        <my:currentWWUrl action="browse" namespace="/">Browse</my:currentWWUrl>
      </li>
    </ul>

    <redback:ifAnyAuthorized permissions="archiva-upload-repository,archiva-delete-artifact,archiva-manage-users,archiva-access-reports,archiva-manage-configuration,archiva-view-audit-logs">
      <h5>Manage</h5>
      <ul>
        <redback:ifAuthorized permission="archiva-access-reports">
          <li class="none">
            <my:currentWWUrl action="pickReport" namespace="/report">Reports</my:currentWWUrl>
          </li>
        </redback:ifAuthorized>
        <redback:ifAuthorized permission="archiva-view-audit-logs">
          <li class="none">
            <my:currentWWUrl action="queryAuditLogReport" namespace="/report">Audit Log Report</my:currentWWUrl>
          </li>
        </redback:ifAuthorized>
        <redback:ifAuthorized permission="archiva-manage-users">
          <li class="none">
            <my:currentWWUrl action="userlist" namespace="/security">User Management</my:currentWWUrl>
          </li>
        </redback:ifAuthorized>
        <redback:ifAuthorized permission="archiva-manage-users">
          <li class="none">
            <my:currentWWUrl action="roles" namespace="/security">User Roles</my:currentWWUrl>
          </li>
        </redback:ifAuthorized>        
        <redback:ifAuthorized permission="archiva-manage-configuration">
          <li class="none">
            <my:currentWWUrl action="configureAppearance" namespace="/admin">Appearance</my:currentWWUrl>
          </li>
        </redback:ifAuthorized>
        <redback:ifAuthorized permission="archiva-upload-repository">
          <li class="none">
            <my:currentWWUrl action="upload" namespace="/">Upload Artifact</my:currentWWUrl>
          </li>
        </redback:ifAuthorized>
        <redback:ifAuthorized permission="archiva-delete-artifact">
          <li class="none">
            <my:currentWWUrl action="deleteArtifact" namespace="/">Delete Artifact</my:currentWWUrl>
          </li>
        </redback:ifAuthorized>        
          <%-- TODO: future options here.
             * Repository Statistics.
             * Web Services Statistics.
          --%>
      </ul>
    </redback:ifAnyAuthorized>

    <redback:ifAuthorized permission="archiva-manage-configuration">
      <h5>Administration</h5>
      <ul>
        <li class="none">
          <my:currentWWUrl action="repositoryGroups" namespace="/admin">Repository Groups</my:currentWWUrl>
        </li>
        <li class="none">
          <my:currentWWUrl action="repositories" namespace="/admin">Repositories</my:currentWWUrl>
        </li>
        <li class="none">
          <my:currentWWUrl action="proxyConnectors" namespace="/admin">Proxy Connectors</my:currentWWUrl>
        </li>
        <li class="none">
          <my:currentWWUrl action="legacyArtifactPath" namespace="/admin">Legacy Support</my:currentWWUrl>
        </li>
        <li class="none">
          <my:currentWWUrl action="networkProxies" namespace="/admin">Network Proxies</my:currentWWUrl>
        </li>
        <li class="none">
          <my:currentWWUrl action="repositoryScanning" namespace="/admin">Repository Scanning</my:currentWWUrl>
        </li>
        <li class="none">
          <my:currentWWUrl action="database" namespace="/admin">Database</my:currentWWUrl>
        </li>
          <%-- TODO: future options here.
               * Repository Syncing Connectors. (rsync, ftp, scp, etc...)
               * Web Services (enable / disable), role based?
            --%>
      </ul>
    </redback:ifAuthorized>

  </div>
</div>

<div id="bodyColumn">
  <div id="contentBox">
    <decorator:body/>
  </div>
</div>

<div class="clear">
  <hr/>
</div>

<%
  int inceptionYear = 2005;
  int currentYear = Calendar.getInstance().get( Calendar.YEAR );
  String copyrightRange = String.valueOf( inceptionYear );
  if ( inceptionYear != currentYear )
  {
    copyrightRange = copyrightRange + "-" + String.valueOf( currentYear );
  }
%>
<div id="footer">
  <div class="xleft">
    <a target="_blank" href="http://archiva.apache.org/">Apache Archiva <%= ArchivaVersion.getVersion()%></a>
  </div>
  <div class="xright">
    Copyright &#169; <%= copyrightRange%> <a target="_blank" href="http://www.apache.org/">The Apache Software Foundation</a>
  </div>

  <div class="clear">
    <hr/>

  </div>
</div>
</body>
</html>
