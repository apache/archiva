LABEL = 'ubuntu'
buildJdk = 'JDK 1.8 (latest)'
buildMvn = 'Maven 3.5.2'
deploySettings = 'DefaultMavenSettingsProvider.1331204114925'

pipeline {
    agent {
        label "${LABEL}"
    }

    stages {


        stage('Checkout') {
            steps {
                script {
                    echo "Info: Job-Name=${JOB_NAME}, Branch=${BRANCH_NAME}, Workspace=${PWD}"
                }
                checkout scm
            }
            post {
                failure {
                    notifyBuild("Checkout failure")
                }
            }
        }

        stage('Build') {
            steps {
                timeout(120) {
                    withMaven(maven: buildMvn, jdk: buildJdk,
                            mavenSettingsConfig: deploySettings,
                            mavenLocalRepo: ".repository",
                            publisherStrategy='EXPLICIT'
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
                                sh "mvn clean deploy -B -U -e -fae -Dmaven.test.failure.ignore=true -T2 -Dmaven.compiler.fork=false -Pci-build"
                            }
                }
            }
            post {
                always {
                    junit testResults: '**/target/surefire-reports/TEST-*.xml'
                }
                success {
                    archiveArtifacts '**/target/*.war,**/target/*-bin.zip'
                    script {
                        def previousResult = currentBuild.previousBuild?.result
                        if (previousResult && previousResult != currentBuild.result) {
                            notifyBuild("Fixed")
                        }
                    }
                }
                failure {
                    notifyBuild("Build / Test failure")
                }
            }
        }

    }
    post {
        unstable {
            notifyBuild("Unstable Build")
        }
        always {
            cleanWs deleteDirs: true, notFailBuild: true, patterns: [[pattern: '.repository', type: 'EXCLUDE']]
        }
    }
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
