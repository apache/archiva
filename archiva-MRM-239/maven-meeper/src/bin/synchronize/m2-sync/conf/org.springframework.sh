#!/bin/sh

CONTACTS="Ben Hale <bhale@interface21.com>" 
MODE=svn
SVN_URL=https://svn.sourceforge.net/svnroot/springframework/repos/repo

FROM=/home/maven/repository-staging/to-ibiblio/maven-spring
GROUP_DIR=org/springframework/

svn up $FROM/$GROUP_DIR