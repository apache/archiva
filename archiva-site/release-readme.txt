Archiva release process.

1) Perform a release:prepare at root of subversion tree.

2) Perform a release:peform at the root of subversion tree.

   Be sure you have the appropriate $HOME/.m2/settings.xml settings.

  <profiles>
    <profile>
      <id>release</id>
      <properties>
        <gpg.passphrase>********</gpg.passphrase>
      </properties>
    </profile>
  </profiles>

  Be sure you update your gpg passphrase!!

3) Create the vote for release email.

4) Wait 72 hours

5) Tabulate votes

   If a negative binding vote occurs, the release must be reviewed.
   If 3 or more positive binding votes occur, then the release is blessed.

6) Perform a stage:copy of the staged repository to the m2-ibiblio-rsync-repository location.
   This plugin can be found at the subversion url below:
     https://svn.apache.org/repos/asf/maven/plugins/trunk/maven-stage-plugin

   $ mvn stage:copy -Dsource="http://people.apache.org/builds/maven/archiva/1.0-alpha-1/m2-staging-repository/" -Dtarget="scpexe://people.apache.org/www/people.apache.org/repo/m2-ibiblio-rsync-repository" -Dversion=1.0-alpha-1 -DrepositoryId=apache.releases

7) Copy the binaries over to the dist directory.

   $ ssh people.apache.org cp /www/people.apache.org/builds/maven/archiva/1.0-alpha-1/archiva-*-bin* /www/www.apache.org/dist/maven/binaries/

   $ ssh people.apache.org cp /www/people.apache.org/builds/maven/archiva/1.0-alpha-1/archiva-*-src* /www/www.apache.org/dist/maven/source/

8) Update the archiva-site documents to point to the new downloads.

   Change the download box in the archiva-site/src/site/xdoc/index.xml
   To point to the new binaries and source files.

9) Deploy the site.

   $ mvn clean site:site site:deploy

