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
<html>
<head>
  <title>Maven Repository Manager ::
    <decorator:title default="Maven Repository Manager" />
  </title>

  <style type="text/css" media="all">
    @import url( "<%= request.getContextPath() %>/css/maven-base.css" );
    @import url( "<%= request.getContextPath() %>/css/maven-theme.css" );
    @import url( "<%= request.getContextPath() %>/css/site.css" );
  </style>
  <link rel="stylesheet" href="<%= request.getContextPath() %>/css/print.css" type="text/css" media="print" />
  <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
</head>

<body onload="<decorator:getProperty property="body.onload" />" class="composite">
<div id="banner">
  <span id="bannerLeft">
    <%--
        <img src="http://www.apache.org/images/asf_logo_wide.gif" alt="" width="537" height="51" />
    --%>
  </span>
  <span id="bannerRight">
    <!-- img src="..." alt="" /> -->
  </span>

  <div class="clear">
    <hr />
  </div>
</div>

<div id="breadcrumbs">


  <div class="xleft">
  </div>

  <div class="xright">
  </div>

  <div class="clear">
    <hr />
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
    <h5>Manage</h5>
    <ul>

      <%-- TODO
            <li class="none">
              <a href="#">Reports</a>
            </li>

            <li class="none">
              <a href="#">Synchronisation</a>
            </li>
      --%>

      <li class="expanded">
        <my:currentWWUrl action="index" namespace="/admin">Administration</my:currentWWUrl>
        <ul>
          <li class="none">
            <my:currentWWUrl action="configure" namespace="/admin">Configuration</my:currentWWUrl>
          </li>
        </ul>
      </li>
    </ul>

    <br />
  </div>
</div>

<div id="bodyColumn">
  <div id="contentBox">
    <decorator:body />
  </div>

</div>

<div class="clear">
  <hr />
</div>

<div id="footer">
  <div class="xright">&#169;
    2005-2006 Apache Software Foundation
  </div>

  <div class="clear">
    <hr />

  </div>
</div>
</body>
</html>
