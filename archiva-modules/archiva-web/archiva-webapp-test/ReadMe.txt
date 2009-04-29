ReadMe
----------
Run selenium tests in src/test/testng with Maven and TestNG
  - Start Archiva
  - Configure admin user for archiva (match values in src/test/resources/testng.properties )
  - modify src/test/resources/testng.properties as needed
  - mvn clean install -f testng-pom.xml

For the default values in the scripts, to pass all the tests, you need to add an artifact in internal repository.
