def labels = 'ubuntu'
def buildJdk = 'JDK 1.8 (latest)'
def buildMvn = 'Maven 3.5.2'
def deploySettings = 'DefaultMavenSettingsProvider.1331204114925'

node(labels) {

    def PWD = pwd();
    echo "Workspace: ${PWD}"

    try {
        stage('Checkout') {
            checkout scm
        }
    } catch (Exception e) {
        notifyBuild("Checkout Failure")
        throw e
    }

    try {
        stage('Build') {
            timeout(120) {
                withMaven(maven: buildMvn, jdk: buildJdk,
                        mavenSettingsConfig: deploySettings,
                        mavenLocalRepo: ".repository"
                )
                        {
                            sh "chmod 755 ./src/ci/scripts/prepareWorkspace.sh"
                            sh "./src/ci/scripts/prepareWorkspace.sh"
                            // Needs a lot of time to reload the repository files, try without cleanup
                            // Not sure, but maybe
                            // sh "rm -rf .repository"


                            // Run test phase / ignore test failures
                            // -B: Batch mode
                            // -U: Force snapshot update
                            // -e: Produce execution error messages
                            // -fae: Fail at the end
                            // -Dmaven.compiler.fork=false: Do not compile in a separate forked process
                            // -Dmaven.test.failure.ignore=true: Do not stop, if some tests fail
                            // -Pci-build: Profile for CI-Server
                            sh "mvn clean install -B -U -e -fae -Dmaven.test.failure.ignore=true -T2 -Dmaven.compiler.fork=false -Pci-build"
                        }
            }
        }
    } catch (Exception e) {
        notifyBuild("Test Failure")
        throw e
    }

    try {
        stage('Deploy') {
            timeout(120) {
                withMaven(maven: buildMvn, jdk: buildJdk,
                        mavenSettingsConfig: deploySettings,
                        mavenLocalRepo: ".repository"
                )
                        {
                            // Repository cleanup causes a lot of time to reload them. We try without cleanup
                            // sh "rm -rf .repository"
                            // Run test phase / ignore test failures
                            sh "mvn -B deploy"
                        }
            }
        }
    } catch (Exception e) {
        notifyBuild("Deploy Failure")
        throw e
    }

    post {
        always {
            if (isUnstable()) {
                notifyBuild("Unstable Build")
            }
            junit testDataPublishers: [[$class: 'StabilityTestDataPublisher']], '**/target/surefire-reports/TEST-*.xml'
            archiveArtifacts '**/target/*.war,**/target/*-bin.zip'
        }
    }
}

// Test if the Jenkins Pipeline or Step has marked the
// current build as unstable
def isUnstable() {
    return currentBuild.result == "UNSTABLE"
}

// Send a notification about the build status
def notifyBuild(String buildStatus) {
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

    emailext(
            to: email,
            subject: summary,
            body: detail,
            mimeType: 'text/html'
    )
}

// vim: et:ts=2:sw=2:ft=groovy
