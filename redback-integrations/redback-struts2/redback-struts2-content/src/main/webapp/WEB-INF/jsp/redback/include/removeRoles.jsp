<%--
  Created by IntelliJ IDEA.
  User: jesse
  Date: Sep 18, 2006
  Time: 1:50:21 AM
  To change this template use File | Settings | File Templates.
--%>
<%@ taglib uri="/struts-tags" prefix="s" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
    <s:form action="user" name="removeRole" method="post" namespace="/security/admin">
      <s:hidden name="principal">${username}</s:hidden>
      <s:select name="roleName" list="assignedRoles" labelposition="top" />
      <s:submit value="%{getText('remove')}" />
    </s:form>
