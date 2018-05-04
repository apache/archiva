/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * Main build file for Jenkins Multibranch pipeline.
 *
 * The pipeline builds, runs the test and deploys to the archiva snapshot repository.
 *
 * Uses one stage for build and deploy to avoid running it multiple times.
 * The settings for deployment with the credentials must be provided by a MavenSettingsProvider.
 *
 * Only the war and zip artifacts are archived in the jenkins build archive.
 */
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
                    notifyBuild("Checkout failure (${currentBuild.currentResult})")
                }
            }
        }

        stage('Build') {
            steps {
                timeout(120) {
                    withMaven(maven: buildMvn, jdk: buildJdk,
                            mavenSettingsConfig: deploySettings,
                            mavenLocalRepo: ".repository",
                            options: [concordionPublisher(disabled: true), dependenciesFingerprintPublisher(disabled: true),
                                      findbugsPublisher(disabled: true), artifactsPublisher(disabled: true),
                                      invokerPublisher(disabled: true), jgivenPublisher(disabled: true),
                                      junitPublisher(disabled: true, ignoreAttachments: false),
                                      openTasksPublisher(disabled: true), pipelineGraphPublisher(disabled: true)]
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
                        if (previousResult && !currentBuild.isWorseOrEqual(previousResult)) {
                            notifyBuild("Fixed: ${currentBuild.currentResult}")
                        }
                    }
                }
                failure {
                    notifyBuild("Build / Test failure (${currentBuild.currentResult})")
                }
            }
        }

    }
    post {
        unstable {
            notifyBuild("Unstable Build (${currentBuild.currentResult})")
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
    def summary = "${env.JOB_NAME}#${env.BUILD_NUMBER} - ${buildStatus} - ${currentBuild?.currentStatus}"
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
