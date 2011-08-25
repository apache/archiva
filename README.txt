Apache Archiva
==============

To get involved in Archiva development, contact dev@archiva.apache.org.

Running from Source Code
========================

With maven 3 and the tomcat-maven-plugin, you will be able to run the webapp from the top
and include all the other modules in the webapp classloader.
No more need to install everything to run the jetty plugin.
So just use : mvn tomcat:run -Pdev
and hit in your browser : http://localhost:9091/archiva

