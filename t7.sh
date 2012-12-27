set -x
mvn tomcat7:run -pl :archiva-webapp -am  $@
