
def labels = 'ubuntu'
def buildJdk = 'JDK 1.8 (latest)'
def buildMvn = 'Maven 3.5.2'  
def deploySettings = 'DefaultMavenSettingsProvider.1331204114925'

node (labels) {
  try
  {
    stage ('Checkout') {
      checkout scm
    }  
  } catch (Exception e) {
    //notifyBuild("Checkout Failure")
    throw e
  }

  try
  {
    stage ('Build') {
      timeout(120) {
        withMaven(maven: buildMvn, jdk: buildJdk,
                      mavenSettingsConfig: deploySettings,
                      mavenLocalRepo: ".repository"                  
                 )
          {
            sh "rm -rf .repository"
            // Run test phase / ignore test failures
            sh "mvn -B clean deploy -Dmaven.test.failure.ignore=true -T2"
          }  
        // Report failures in the jenkins UI
        //step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/TEST-*.xml'])
      }
      if(isUnstable())
      {
        //notifyBuild("Unstable / Test Errors")
      }
    }  
  } catch(Exception e) {
    notifyBuild("Test Failure")
    throw e
  }
}

// Test if the Jenkins Pipeline or Step has marked the
// current build as unstable
def isUnstable()
{
  return currentBuild.result == "UNSTABLE"
}

// Send a notification about the build status
def notifyBuild(String buildStatus)
{
  // default the value
  buildStatus = buildStatus ?: "UNKNOWN"

  def email = "notifications@archiva.apache.org"
  def summary = "${env.JOB_NAME}#${env.BUILD_NUMBER} - ${buildStatus}"
  def detail = """<h4>Job: <a href='${env.JOB_URL}'>${env.JOB_NAME}</a> [#${env.BUILD_NUMBER}]</h4>
  <p><b>${buildStatus}</b></p>
  <table>
    <tr><td>Build</td><td><a href='${env.BUILD_URL}'>${env.BUILD_URL}</a></td><tr>
    <tr><td>Console</td><td><a href='${env.BUILD_URL}console'>${env.BUILD_URL}console</a></td><tr>
    <tr><td>Test Report</td><td><a href='${env.BUILD_URL}testReport/'>${env.BUILD_URL}testReport/</a></td><tr>
  </table>
  """

  emailext (
    to: email,
    subject: summary,
    body: detail
  )
}

// vim: et:ts=2:sw=2:ft=groovy
