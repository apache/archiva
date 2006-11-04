#!/bin/sh

java -jar archiva-cli-1.0-SNAPSHOT-cli.jar -c \
 -o /home/maven/repository-staging/to-ibiblio/maven \
 -n /home/maven/repository-staging/to-ibiblio/maven2-repoclean \
 -b **/*.pom,**/activation/**,**/javamail/**,**/jaxm/**,**/jaxp/**,**/jaxrpc/**,**/jca/**,**/jce/**,**/jdbc/**,**/jdo/**,**/jms/**,**/jndi/**,**/saaj/**
