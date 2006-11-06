#!/bin/sh

syncProperties=$1

. $syncProperties

java -jar archiva-cli-1.0-SNAPSHOT-cli.jar -c synchronize.properties
#java -jar archiva-cli-1.0-SNAPSHOT-cli.jar -c synchronize-java.net.properties

dir=/home/maven/repository-staging/to-ibiblio
src=$dir/maven2-converted-from-maven1
dst=$dir/maven2
log=$SYNC_REPORTS/last-changes.log

rsync --ignore-existing -rvpl $src/ $dst/ > $log

for f in `cat $log | grep maven-metadata.xml$` ; do
  md5sum $dst/$f > $dst/$f.md5;
  sha1sum $dst/$f > $dst/$f.sha1;
  md5sum $src/$f > $src/$f.md5;
  sha1sum $src/$f > $src/$f.sha1;
done
