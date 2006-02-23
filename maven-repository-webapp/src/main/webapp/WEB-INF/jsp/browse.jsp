<%@ taglib uri="webwork" prefix="ww" %>
<%@page import="java.util.*"%>
<html>
<head>
<title>Repository Browser</title>
</head>
<body>
<h3><a href="<ww:url value="browse!edit.action"><ww:param name="idx" value="0"/></ww:url>">basedir</a> /
<ww:set name="previousFolder" value="''"/>
<ww:set name="counter" value="0"/>
<ww:if test="folder != ''">
  <ww:set name="folderHeader" value="folder.split('/')"/>
  <ww:iterator value="#folderHeader">
    <ww:set name="counter" value="#counter + 1"/>
    <ww:if test="#previousFolder == ''">
      <ww:set name="previousFolder" value="top"/>
    </ww:if>
    <ww:else>
      <ww:set name="previousFolder" value="#previousFolder + '/' + top"/>
    </ww:else>
    <ww:if test="idx > (#counter + 1)"><a href="<ww:url value="browse!edit.action"><ww:param name="idx"><ww:property value="#counter"/></ww:param><ww:param name="folder"></ww:param></ww:url>"></ww:if><ww:property/></a> /
  </ww:iterator>
</ww:if>
</h3>
<br/>

<ww:set name="previousFolder" value="'the previous folder'"/>
<ww:set name="in" value="idx" scope="page"/>
<ww:iterator value="artifactMap.keySet().iterator()">
  <ww:set name="groupName" value="top"/>
<ww:if test="idx == 1 || (folder != '' and  #groupName.startsWith(folder))">
<%
int ctr = 1;
%>
  <ww:set name="groupFolder" value="#groupName.split('/')"/>
    <ww:iterator value="#groupFolder">
<%
if (ctr == ((Integer)pageContext.getAttribute("in")).intValue()) {%>
      <ww:if test="top != #previousFolder">
        <ww:set name="previousFolder" value="top"/>
        <a href="<ww:url value="browse!edit.action"><ww:param name="folder"><ww:property value="folder"/><ww:if test="folder != ''">/</ww:if><ww:property/></ww:param><ww:param name="idx" value="idx"/></ww:url>"">
        <ww:property/>/
        </a><br>
      </ww:if>
<%
}
ctr++;
%>
    </ww:iterator>
</ww:if>
</ww:iterator>

<ww:if test="folder != ''">
  <ww:set name="previousFolder" value="''"/>
  <ww:set name="artifactList" value="artifactMap.get(folder)"/>
  <ww:iterator value="#artifactList">
<table border="1">
          <tr align="left">
            <th>Group ID</th>
            <td><ww:property value="groupId"/></td>
          </tr>
          <tr align="left">
            <th>Artifact ID</th>
            <td><ww:property value="artifactId"/></td>
          </tr>
          <tr align="left">
            <th>Version</th>
            <td><ww:property value="version"/></td>
          </tr>
          <tr align="left">
            <th>Derivatives</th>
            <td><ww:property value="groupId"/></td>
          </tr>
          <tr align="left">
            <th>Parent</th>
            <td><ww:property value="folder"/></td>
          </tr>
</table><br/>
  </ww:iterator>
</ww:if>
</body>
</html>
