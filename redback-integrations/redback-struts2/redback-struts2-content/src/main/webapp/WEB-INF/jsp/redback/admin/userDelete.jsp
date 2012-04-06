<%--
  ~ Copyright 2005-2006 The Codehaus.
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

<%@ taglib prefix="s" uri="/struts-tags"%>

<html>
<s:i18n name="org.codehaus.plexus.redback.struts2.default">
<head>
  <title><s:text name="user.delete.page.title"/></title>
</head>

<body>

<h2><s:text name="user.delete.section.title"/></h2>

<s:form action="userdelete!submit" namespace="/security">
  <p>
    <s:text name="user.delete.message"/>:
  </p>
  <p>
  	<s:text name="username"/>: <s:property value="user.username"/><br/>
  	<s:text name="full.name"/>: <s:property value="user.fullName"/><br/>
  	<s:text name="email"/>: <s:property value="user.email"/><br/>
  </p>
  <s:hidden label="Username" name="username" />
  <s:token/>
  <s:submit value="%{getText('user.delete')}" theme="simple" id="userDeleteSubmit"/>
  <s:submit value="%{getText('cancel')}" action="userdelete!cancel" theme="simple"/>
</s:form>

</body>
</s:i18n>
</html>
