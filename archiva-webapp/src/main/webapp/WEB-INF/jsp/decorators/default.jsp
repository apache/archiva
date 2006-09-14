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
<%@ taglib prefix="pss" uri="plexusSecuritySystem" %>
<html>
<head>
  <title>Maven Archiva ::
    <decorator:title default="Maven Archiva"/>
  </title>

  <style type="text/css" media="all">
    @import url( "<c:url value="/css/maven-base.css" />" );
    @import url( "<c:url value="/css/maven-theme.css" />" );
    @import url( "<c:url value="/css/site.css" />" );
  </style>
  <link rel="stylesheet" href="<c:url value="/css/print.css"/>" type="text/css" media="print"/>
  <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1"/>
</head>

<body onload="<decorator:getProperty property="body.onload" />" class="composite">
<div id="banner">
  <span id="bannerLeft">
    <a href="http://maven.apache.org/archiva/">
      <img src="http://maven.apache.org/images/maven.jpg" alt="" width="267" height="70"/>
      <%-- TODO: logo instead
            <img src="http://ci.codehaus.org/continuum_logo_75.gif" alt="" width="188" height="89" />
      --%>
    </a>
  </span>
  <span id="bannerRight">
    <%-- TODO: configured corporate banner
        <a href="http://www.apache.org/">
          <img src="http://www.apache.org/images/asf_logo_wide.gif" alt="" width="537" height="51" />
        </a>
    --%>
  </span>

  <div class="clear">
    <hr/>
  </div>
</div>

<div id="breadcrumbs">
  <div class="xleft">
    <ww:url id="loginUrl" action="login" method="input" namespace="/" includeParams="none"/>
    <ww:url id="registerUrl" action="register" method="input" namespace="/" includeParams="none"/>

    <ww:if test="${sessionScope.authStatus != true}">
      <ww:a href="%{loginUrl}">Login</ww:a> - <ww:a href="%{registerUrl}">Register</ww:a>

    </ww:if>
    <ww:else>
      <ww:url id="logoutUrl" action="logout" namespace="/" includeParams="none"/>
      <ww:url id="manageUserUrl" action="user" namespace="/admin">
        <ww:param name="username">${sessionScope.SecuritySessionUser.username}</ww:param>
      </ww:url>

      Welcome, <b>${sessionScope.SecuritySessionUser.username}</b> -
      <ww:a href="%{manageUserUrl}">Settings</ww:a> -
      <ww:a href="%{logoutUrl}">Logout</ww:a>
    </ww:else>
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
    <pss:ifAnyAuthorized permissions="edit-all-users,access-reports,edit-configuration">
      <h5>Manage</h5>
      <ul>
        <pss:ifAuthorized permission="access-reports">
          <li class="none">
            <my:currentWWUrl action="reports" namespace="/admin">Reports</my:currentWWUrl>
          </li>
        </pss:ifAuthorized>
          <%-- TODO
                <li class="none">
                  <a href="#">Synchronisation</a>
                </li>
          --%>
        <pss:ifAnyAuthorized permissions="edit-configuration,edit-all-users">
          <li class="expanded">
            <pss:ifAuthorized permission="edit-all-users">
              <my:currentWWUrl action="userManagement!input" namespace="/admin">User Management</my:currentWWUrl>               
            </pss:ifAuthorized>
          </li>
          <li>
            <pss:ifAuthorized permission="edit-configuration">
              <my:currentWWUrl action="index" namespace="/admin">Administration</my:currentWWUrl>
            </pss:ifAuthorized>

            <ul>
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
        </pss:ifAnyAuthorized>
      </ul>
    </pss:ifAnyAuthorized>
    <br/>
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

<div id="footer">
  <div class="xright">&#169;
    2005-2006 Apache Software Foundation
  </div>

  <div class="clear">
    <hr/>

  </div>
</div>
</body>
</html>
