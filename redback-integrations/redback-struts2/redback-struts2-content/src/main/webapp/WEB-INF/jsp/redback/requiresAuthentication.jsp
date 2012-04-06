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

<%@ taglib prefix="s" uri="/struts-tags" %>

<html>

<s:i18n name="org.codehaus.plexus.redback.struts2.default">
<head>
  <title><s:text name="requires.authentication.page.title"/></title>
</head>

<body>

<h4><s:text name="requires.authentication.section.title"/></h4>

<div id="results">
  <%-- This is where the "Account Created Successfully" type message goes. --%>
  <div class="success">
    <s:actionmessage />
  </div>
  <%-- This is where errors from the action and other non-form field specific errors appear. --%>
  <div class="errors">
    <s:actionerror />
  </div>
</div>

<p>
  <s:text name="requires.authentication.message"/>
</p>

<ol>
<li>
  <s:url id="login" action="login" namespace="/security" includeParams="none"/>
  <s:text name="requires.authentication.go.ahead"/><s:a href="%{login}"><s:text name="login"/></s:a>
</li>
</ol>

</body>
</s:i18n>
</html>
