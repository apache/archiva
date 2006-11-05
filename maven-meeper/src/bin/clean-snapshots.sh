#!/bin/sh

base=/home/projects/maven/repository-staging
cd $base/to-ibiblio/maven2
dest=$base/snapshots/maven2

find . -name '*SNAPSHOT*' -type d | while read t1
do
  t2=`echo $t1 | sed 's#/[^/]*$##'`
  mkdir -p $dest/$t2
  mv $t1 $dest/$t2
  rmdir $t1
done
