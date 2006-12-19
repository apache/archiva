<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<%--
  ~ Copyright 2005-2006 The Apache Software Foundation.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~      http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  --%>

<%@ taglib uri="http://www.opensymphony.com/sitemesh/decorator" prefix="decorator" %>
<%@ taglib uri="/webwork" prefix="ww" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="my" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="pss" uri="/plexusSecuritySystem" %>
<%@ page import="java.util.Calendar" %>
<html>
<head>
  <title>Maven Archiva ::
    <decorator:title default="Maven Archiva"/>
  </title>

  <style type="text/css" media="all">
    @import url( "<c:url value="/css/maven-base.css" />" );
    @import url( "<c:url value="/css/maven-theme.css" />" );
    @import url( "<c:url value="/css/pss/table.css" />" );
    @import url( "<c:url value="/css/site.css" />" );
  </style>
  <link rel="stylesheet" href="<c:url value="/css/print.css"/>" type="text/css" media="print"/>
  <script type="text/javascript" src="<c:url value="/js/scriptaculous/prototype.js"/>"></script>
  <script type="text/javascript" src="<c:url value="/js/scriptaculous/scriptaculous.js"/>"></script>
  <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1"/>
</head>

<body onload="<decorator:getProperty property="body.onload" />" class="composite">
<div id="banner">
  <span id="bannerLeft">
    <a href="http://maven.apache.org/archiva/">
      <img src="<c:url value='/images/archiva.png' />" alt="" width="188" height="69"/>
    </a>
  </span>
  <span id="bannerRight">
    <ww:action namespace="/components" name="companyInfo" executeResult="true"/>
  </span>

  <div class="clear">
    <hr/>
  </div>
</div>

<div id="breadcrumbs">
  <div class="xleft">
    <c:import url="/WEB-INF/jsp/pss/include/securityLinks.jsp"/>
  </div>

  <div class="xright">
    <a href="http://maven.apache.org/archiva">Archiva</a> |
    <a href="http://maven.apache.org/">Maven</a> |
    <a href="http://www.apache.org/">Apache</a>
  </div>

  <div class="clear">
    <hr/>
  </div>
</div>

<div id="leftColumn">

  <div id="navcolumn">

    <h5>Find</h5>
    <ul>

      <li class="none">
        <my:currentWWUrl action="index" namespace="/">Search</my:currentWWUrl>
      </li>

      <li class="none">
        <my:currentWWUrl action="findArtifact" namespace="/">Find Artifact</my:currentWWUrl>
      </li>

      <li class="none">
        <my:currentWWUrl action="browse" namespace="/">Browse</my:currentWWUrl>
      </li>
    </ul>
    <pss:ifAnyAuthorized permissions="archiva-manage-users,archiva-access-reports,archiva-manage-configuration">
      <h5>Manage</h5>
      <ul>
        <pss:ifAuthorized permission="archiva-access-reports">
          <li class="none">
            <my:currentWWUrl action="reports" namespace="/admin">Reports</my:currentWWUrl>
          </li>
        </pss:ifAuthorized>
          <%-- TODO
                <li class="none">
                  <a href="#">Synchronisation</a>
                </li>
          --%>
        <pss:ifAuthorized permission="archiva-manage-users">
          <li class="none">
            <my:currentWWUrl action="userlist" namespace="/security">User Management</my:currentWWUrl>
          </li>
        </pss:ifAuthorized>
        <pss:ifAuthorized permission="archiva-manage-configuration">
          <li class="none">
            <my:currentWWUrl action="configureAppearance" namespace="/admin">Appearance</my:currentWWUrl>
          </li>
          <li class="expanded">
            <my:currentWWUrl action="index" namespace="/admin">Administration</my:currentWWUrl>


            <ul>
              <li class="none">
                <my:currentWWUrl action="managedRepositories" namespace="/admin">Managed Repositories</my:currentWWUrl>
              </li>
              <li class="none">
                <my:currentWWUrl action="proxiedRepositories" namespace="/admin">Proxied Repositories</my:currentWWUrl>
              </li>

                <%-- TODO: add back after synced repos are implemented
                          <li class="none">
                            <my:currentWWUrl action="syncedRepositories" namespace="/admin">Synced Repositories</my:currentWWUrl>
                          </li>
                --%>
            </ul>
          </li>
        </pss:ifAuthorized>
      </ul>
    </pss:ifAnyAuthorized>

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
  <div class="xright">&#169;
    Copyright &copy; <%= copyrightRange %> Apache Software Foundation
  </div>

  <div class="clear">
    <hr/>

  </div>
</div>
</body>
</html>
