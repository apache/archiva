
############
# README.txt
############

# Explanation of files found within maven-meeper

##########################
# One-button synchronizing
##########################

## Script to synchronize m1 repositories, convert them to m2, fix them, 
## sync the m2 repositories to ibiblio and update the mod_rewrite rules
./synchronize.sh

## Configuration for the above
./synchronize.properties

####################################################
# Upload bundles from the Maven JIRA to a repository
####################################################

## dos2unix function
./bundle-upload/d2u

## See: http://maven.apache.org/guides/mini/guide-ibiblio-upload.html
## Pulls a bundle down via wget, deploys jar, pom, license file, java-sources, javadocs
./bundle-upload/deploy-bundle

##############################################
# Convert an m1 repository to an m2 repository
##############################################

## java.net uses CVS and only certain parts need to be synced.
./m1-m2-conversion/java.net/sync-repoclean.sh

## configuration for the above
./m1-m2-conversion/java.net/synchronize.properties

## Uses http://svn.apache.org/repos/asf/maven/sandbox/repoclean/src/main/bash/repoclean.sh
## which runs Java code to do the bulk of the work.
./m1-m2-conversion/sync-repoclean.sh

## configuration for the above
./m1-m2-conversion/synchronize.properties

## Maven 1 -> Maven 2 mod-rewriting
./synchronize/m1-m2-mod-rewrite-rules.txt

## rsyncs from an m2 repository somewhere onto a subdirectory of 
## the central repository. By choosing the subdirectory, some security is ensured.
./synchronize/m2-sync/m2-sync.sh

## Bash scripts that set variables to be used by the above script
./synchronize/m2-sync/conf/codehaus.sh
./synchronize/m2-sync/conf/displaytag.sh
./synchronize/m2-sync/conf/net.databinder.sh
./synchronize/m2-sync/conf/net.sourceforge.jwebunit.sh
./synchronize/m2-sync/conf/net.sourceforge.maven-taglib.sh
./synchronize/m2-sync/conf/org.acegisecurity.sh
./synchronize/m2-sync/conf/org.apache.sh
./synchronize/m2-sync/conf/org.mortbay.sh
./synchronize/m2-sync/conf/org.objectweb.sh
./synchronize/m2-sync/conf/org.springframework.sh
./synchronize/m2-sync/conf/wicket.sh

## rsyncs the central m2 repository over to the ibiblio mirror
./synchronize/sync-central-to-ibiblio.sh

##################################################
# M1 synchronize from upstream repositories script
##################################################
## INI configuration files
./synchronize/syncopate/conf/apache.conf
./synchronize/syncopate/conf/codehaus.conf
./synchronize/syncopate/conf/maven-plugins-sf.conf
./synchronize/syncopate/conf/mortbay.conf
./synchronize/syncopate/conf/objectweb.conf
./synchronize/syncopate/conf/opensymphony.conf
./synchronize/syncopate/conf/osjava.conf
./synchronize/syncopate/conf/stage.conf
./synchronize/syncopate/conf/test.conf

## standard exclusions. you can add to it in the configuration files
./synchronize/syncopate/exclusions.txt

## INI perl parser - pulled down from CPAN etc
./synchronize/syncopate/IniFiles.pm

## The perl synchronization file
./synchronize/syncopate/sync

## top level configuration
./synchronize/syncopate/syncopate.conf

###############################
# DELETE THESE IF INDEED UNUSED
###############################
./unused/artifact-map.txt
./unused/rewrite.conf
