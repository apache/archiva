ReadMe
----------
Run selenium tests in src/test/testng with Maven and TestNG
  - mvn clean install 

Run Selenium tests in src/test/testng against an existing Archiva instance
  - Start Archiva
  - Configure admin user for archiva (match values in src/test/resources/testng.properties )
  - modify src/test/resources/testng.properties as needed
  - mvn clean install -Dcontainer-existing

The Cargo installations are stored outside of target to avoid multiple downloads.
To remove the Cargo installations and re-download them next run, use:
  - mvn -Pclean-cargo clean

Run Selenium tests in src/test/it with Maven and JUnit
  - mvn clean install -f junit-pom.xml
