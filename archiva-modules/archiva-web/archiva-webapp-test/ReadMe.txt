ReadMe
----------
Run selenium tests in src/test/testng with Maven and TestNG
  - mvn clean install 

Run Selenium tests in src/test/testng against an existing Archiva instance
  - Start Archiva
  - Configure admin user for archiva (match values in src/test/resources/testng.properties )
  - modify src/test/resources/testng.properties as needed
  - mvn clean install -Dcontainer-existing

Internet Explorer and Safari users must disable their popup blockers. Using *iexplore as
the browser requires running as an Administrator on Windows 7/Vista, or alternatively you
can use *iexploreproxy.


  

IMPORTANT:

When writing Selenium tests for artifact upload, please avoid using the "test" syllable/word for 
the groupId or artifactId (ex. test.group:testAddArtifactValidValues:1.0) as this is used for the 
search tests. The tests explicitly assert the returned number of hits for searching an artifact with 
a groupId or artifactId containing the word "test", so if you upload or add a new artifact which has
the term "test", the number of hits will be different and the search tests will fail.

See org.apache.archiva.web.test.SearchTest.java or read the related thread discussion at
http://old.nabble.com/Selenium-tests-failure-in-trunk-td27830786.html
   

