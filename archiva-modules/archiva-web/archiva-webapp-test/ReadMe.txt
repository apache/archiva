ReadMe
----------
Run selenium tests in src/test/testng with Maven and TestNG
  - mvn clean install 

Run Selenium tests in src/test/testng against an existing Archiva instance
  - Start Archiva
  - Configure admin user for archiva (match values in src/test/resources/testng.properties )
  - modify src/test/resources/testng.properties as needed
  - mvn clean install -Dcontainer-existing

For the default values in the scripts, to pass all the tests, you need to add an artifact in internal repository.

Run Selenium tests in src/test/it with Maven and JUnit
  - mvn clean install -f junit-pom.xml