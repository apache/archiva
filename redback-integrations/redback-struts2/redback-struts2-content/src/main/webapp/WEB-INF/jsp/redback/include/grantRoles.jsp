<%--
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
<%@ taglib uri="/struts-tags" prefix="s" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

    <s:set name="availableRoles" value="#availableRoles"/>
    <s:form action="user" name="grantRole" method="post" namespace="/security/admin">
      <s:hidden name="principal">${username}"</s:hidden>
      <s:select name="roleName" list="availableRoles" labelposition="top" />
      <s:submit value="%{getText('grant')}" />
    </s:form>
