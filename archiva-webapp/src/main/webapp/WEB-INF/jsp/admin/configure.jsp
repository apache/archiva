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

<div id="contentArea">
  <ww:actionmessage/>
  <ww:form method="post" action="saveConfiguration" namespace="/admin" validate="true">

    <div>
    <table>
        <tbody>
          <tr>
            <th><font size="2"><ww:label theme="simple" value="Indexing Directory*:"/></font></th>
            <td><ww:textfield name="indexPath" theme="simple" size="140" required="true"/></td>
          </tr>
          <tr>
            <th><font size="2"><ww:label theme="simple" value="Indexing Schedule:"/></font></th>
            <td>
              <table>
                <tr>
                  <th><ww:label theme="simple" value="Second:"/></th>
                  <td><ww:textfield name="second" theme="simple" size="2"/></td>

                  <th><ww:label theme="simple" value="Minute:"/></th>
                  <td><ww:textfield name="minute" theme="simple" size="2"/></td>

                  <th><ww:label theme="simple" value="Hour:"/></th>
                  <td><ww:textfield name="hour" theme="simple" size="2"/></td>

                  <th><ww:label theme="simple" value="Day of Month:"/></th>
                  <td><ww:textfield name="dayOfMonth" theme="simple" size="2"/></td>

                  <th><ww:label theme="simple" value="Month:"/></th>
                  <td><ww:textfield name="month" theme="simple" size="2"/></td>

                  <th><ww:label theme="simple" value="Day of Week:"/></th>
                  <td><ww:textfield name="dayOfWeek" theme="simple" size="2"/></td>

                  <th><ww:label theme="simple" value="Year [optional]:"/></th>
                  <td><ww:textfield name="year" theme="simple" size="4"/></td>
                </tr>
              </table>
            </td>
          </tr>
        <ww:hidden name="proxy.protocol" value="http"/>
        <tr>
            <th><font size="2"><ww:label theme="simple" value="HTTP Proxy Host:"/></font></th>
            <td><ww:textfield name="proxy.host" theme="simple"/></td>
        </tr>
        <tr>
            <th><font size="2"><ww:label theme="simple" value="HTTP Proxy Port:"/></font></th>
            <td><ww:textfield name="proxy.port" theme="simple"/></td>
        </tr>
        <tr>
            <th><font size="2"><ww:label theme="simple" value="HTTP Proxy Username:"/></font></th>
            <td><ww:textfield name="proxy.username" theme="simple"/></td>
        </tr>
        <tr>
            <th><font size="2"><ww:label theme="simple" value="HTTP Proxy Password:"/></font></th>
            <td><ww:textfield name="proxy.password" theme="simple"/></td>
        </tr>
        </tbody>
    </table>
    </div>

    <div>
      <p><i>For valid cron expression values for the Indexing Schedule, see <ww:a href="http://www.opensymphony.com/quartz/api/org/quartz/CronExpression.html">here</ww:a></i></p>
    </div>
    <div>
      <table>
        <tr>
          <b>Indexing Schedule Keys:</b>
        </tr>
        <tr>
          <th>*</th>
          <td>every</td>
        </tr>
        <tr>
          <th>?</th>
          <td>any</td>
        </tr>
        <tr>
          <th>-</th>
          <td>ranges</td>
        </tr>
        <tr>
          <th>/</th>
          <td>increments</td>
        </tr>
      </table>
    </div>
    <ww:submit value="Save Configuration"/>
  </ww:form>

  <script type="text/javascript">
    document.getElementById("saveConfiguration_indexPath").focus();
  </script>

</div>

</body>
</html>
