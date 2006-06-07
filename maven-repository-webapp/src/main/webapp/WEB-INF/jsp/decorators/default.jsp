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
<html>
<head>
  <title><decorator:title default="Maven Repository Manager" /></title>

  <style type="text/css" media="all">
    @import url( "./css/maven-base.css" );
    @import url( "./css/maven-theme.css" );
    @import url( "./css/site.css" );
  </style>
  <link rel="stylesheet" href="./css/print.css" type="text/css" media="print" />
  <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
</head>

<body class="composite">
<div id="banner">
  <span id="bannerLeft">
    <img src="http://www.apache.org/images/asf_logo_wide.gif" alt="" width="537" height="51" />
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
        <a href="index.html">Search</a>
      </li>

      <li class="none">
        <a href="find-artifact.html">Find Artifact</a>

      </li>

      <li class="none">
        <a href="browse.html">Browse</a>
      </li>
    </ul>
    <h5>Manage</h5>
    <ul>

      <li class="none">

        <a href="reports.html">Reports</a>
      </li>

      <li class="none">
        <strong>Synchronisation</strong>
      </li>

      <li class="none">
        <a href="admin.html">Administration</a>

      </li>
    </ul>


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
