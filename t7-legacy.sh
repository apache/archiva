set -x
mvn tomcat7:run -Ptomcat -pl :archiva-webapp -am $@
