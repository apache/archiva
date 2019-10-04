Apache Archiva
==============

Licensing information
=====================

Archiva is developed under the Apache License Version 2.0 

Please notice, the download distribution includes third party Java libraries that are not covered by Apache license, namely:
- Common Development and Distribution License (CDDL)
- Mozilla License
- Day Specification License


Archiva Development
===================

To get involved in Archiva development, contact dev@archiva.apache.org.

NOTE: you will need a MAVEN_OPTS with some memory setup as sample :
export MAVEN_OPTS="-Xmx768m -Xms768m -XX:MaxPermSize=256m"

Running from Source Code
========================

As webapp js is in dev and won't probably be released soon, the module is not activated by default and it's included only in a profile
mvn jetty:run -pl :archiva-webapp -am  (to save fingers :-) use sh ./jetty.sh ) (debug with sh ./jetty-debug.sh debug port 8000)


hit your browser: http://localhost:9091/archiva/index.html

Test Registration email
========================
Redback can send email on registration by default the mail jndi si configured to use localhost.
You can use your gmail accout for testing purpose
In your ~/.m2/settings.xml add a property with a path to a tomcat context file:
```
<tomcatContextXml>/Users/olamy/dev/tomcat-context-archiva-gmail.xml</tomcatContextXml>
```
This file must contains:

```
<Context path="/archiva">
  <Resource name="jdbc/users" auth="Container" type="javax.sql.DataSource"
            username="sa"
            password=""
            driverClassName="org.apache.derby.jdbc.EmbeddedDriver"
            url="jdbc:derby:${catalina.base}/target/database/users;create=true"
  />
  <Resource name="mail/Session" auth="Container"
          type="javax.mail.Session"
          mail.smtp.host="smtp.gmail.com"
          mail.smtp.port="465"
          mail.smtp.auth="true"
          mail.smtp.user="your gmail account"
          password="your gmail password"
          mail.smtp.starttls.enable="true"
          mail.smtp.socketFactory.class="javax.net.ssl.SSLSocketFactory"/>

</Context>
```

Using with cassandra as metadata storage
========================
You can run the application using cassandra as storage.
sh ./jetty.sh -Pcassandra

Default cassandra host is localhost and port 9160

You can override using:

 * -Dcassandra.host=
 * -Dcassandra.port=




