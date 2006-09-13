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

<%@ taglib prefix="ww" uri="/webwork" %>

<html>
<head>
  <title>Configuration</title>
  <ww:head/>
</head>

<body>

<h1>Configuration</h1>

<div class="errors">
  <ww:actionerror />
</div>

<div id="contentArea">
  <ww:actionmessage/>
  <ww:form method="post" action="saveConfiguration" namespace="/admin" validate="true">
    <ww:textfield name="indexPath" label="Index Directory" size="100" required="true"/>
    <!--ww:textfield name="indexerCronExpression" label="Indexing Schedule"/-->

    <ww:label value="Indexing Schedule" labelposition="top"/>
      <ww:div>
        <ww:textfield name="second" label="Second" size="2"/>
        <ww:textfield name="minute" label="Minute" labelposition="left" size="2"/>
        <ww:textfield name="hour" label="Hour" size="2"/>
        <ww:textfield name="dayOfMonth" label="Day Of Month" size="2"/>
        <ww:textfield name="month" label="Month" size="2"/>
        <ww:textfield name="dayOfWeek" label="Day Of Week" size="2"/>
        <ww:textfield name="year" label="Year" size="4"/>
      </ww:div>

    <ww:hidden name="proxy.protocol" value="http"/>
    <ww:textfield name="proxy.host" label="HTTP Proxy Host"/>
    <ww:textfield name="proxy.port" label="HTTP Proxy Port"/>
    <ww:textfield name="proxy.username" label="HTTP Proxy Username"/>
    <ww:password name="proxy.password" label="HTTP Proxy Password"/>

    <ww:submit value="Save Configuration"/>

    <ww:div>
      <ww:label value="Indexing Schedule Keys:" labelposition="top"/>
      <ww:label value="* = every" labelposition="top"/>
      <ww:label value="? = any" labelposition="top"/>
      <ww:label value="- = ranges" labelposition="top"/>
      <ww:label value="/ = increments" labelposition="top"/>
    </ww:div>
  </ww:form>

  <ww:div>
    <p><i>For valid cron expression values for the Indexing Schedule, see <ww:a href="http://www.opensymphony.com/quartz/api/org/quartz/CronExpression.html">here</ww:a></i></p>
  </ww:div>

  <script type="text/javascript">
    document.getElementById("saveConfiguration_indexPath").focus();
  </script>

</div>

</body>
</html>
