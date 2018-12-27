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
buildJdk9 = 'JDK 1.9 (latest)'
buildJdk10 = 'JDK 10 (latest)'
buildJdk11 = 'JDK 11 (latest)'
buildMvn = 'Maven 3.5.2'
deploySettings = 'archiva-uid-jenkins'
localRepository = "../.archiva-master-repository"
mavenOpts = '-Xms1g -Xmx2g -Djava.awt.headless=true'

INTEGRATION_PIPELINE = "Archiva-IntegrationTests-Gitbox"

pipeline {
    agent {
        label "${LABEL}"
    }
    options {
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '15', artifactNumToKeepStr: '15'))
    }
    parameters {
        booleanParam(name: 'PRECLEANUP', defaultValue: false, description: 'Clears the local maven repository before build.')
    }


    stages {

        stage('PreCleanup') {
            when {
                expression {
                    params.PRECLEANUP
                }
            }
            steps {
                sh "rm -rf ${localRepository}"
            }
        }

        stage('BuildAndDeploy') {
            environment {
                ARCHIVA_USER_CONFIG_FILE = '/tmp/archiva-master-jdk-8-${env.JOB_NAME}.xml'
            }

            steps {
                timeout(120) {
                    withMaven(maven: buildMvn, jdk: buildJdk,
                            mavenSettingsConfig: deploySettings,
                            mavenLocalRepo: localRepository,
                            publisherStrategy: 'EXPLICIT',
                            mavenOpts: mavenOpts,
                            options: [artifactsPublisher(disabled: false),
                                      junitPublisher(disabled: false, ignoreAttachments: false),
                                      pipelineGraphPublisher(disabled: false)]
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
                                // -Dmaven.compiler.fork=true: Do compile in a separate forked process
                                // -Dmaven.test.failure.ignore=true: Do not stop, if some tests fail
                                // -Pci-build: Profile for CI-Server
                                sh "mvn clean deploy -B -U -e -fae -Dmaven.compiler.fork=true -Pci-build"
                            }
                }
            }
            post {
                always {
                    sh "rm -f /tmp/archiva-master-jdk-8-${env.JOB_NAME}.xml"
                }
                failure {
                    notifyBuild("Failure in BuildAndDeploy stage")
                }
            }
        }



        stage('Postbuild') {
            parallel {
                stage('IntegrationTest') {
                    steps {
                        build(job: "${INTEGRATION_PIPELINE}/archiva/${env.BRANCH_NAME}", propagate: false, quietPeriod: 5, wait: false)
                    }
                }
                stage('JDK9') {
                    environment {
                        ARCHIVA_USER_CONFIG_FILE = '/tmp/archiva-master-jdk-9-${env.JOB_NAME}.xml'
                    }
                    steps {
                        ws("${env.JOB_NAME}-JDK9") {
                            checkout scm
                            timeout(120) {
                                withMaven(maven: buildMvn, jdk: buildJdk9,
                                          publisherStrategy: 'EXPLICIT',
                                          mavenOpts: mavenOpts,
                                          mavenSettingsConfig: deploySettings,
                                        mavenLocalRepo: ".repository",
                                        options: [junitPublisher(disabled: false, ignoreAttachments: false)]
                                )
                                        {
                                            sh "mvn clean install -U -B -e -fae -Dmaven.compiler.fork=true -Pci-build"
                                        }
                            }
                        }
                    }
                    post {
                        always {
                            sh "rm -f /tmp/archiva-master-jdk-9-${env.JOB_NAME}.xml"
                        }
                        success {
                            cleanWs deleteDirs: true, notFailBuild: true, patterns: [[pattern: '.repository', type: 'EXCLUDE']]
                        }
                    }
                }
                stage('JDK10') {
                    environment {
                        ARCHIVA_USER_CONFIG_FILE = '/tmp/archiva-master-jdk-10-${env.JOB_NAME}.xml'
                    }
                    steps {
                        ws("${env.JOB_NAME}-JDK10") {
                            checkout scm
                            timeout(120) {
                                withMaven(maven: buildMvn, jdk: buildJdk10,
                                        mavenSettingsConfig: deploySettings,
                                        mavenLocalRepo: ".repository",
                                          publisherStrategy: 'EXPLICIT',
                                          mavenOpts: mavenOpts,
                                          options: [junitPublisher(disabled: false, ignoreAttachments: false)]
                                )
                                        {
                                            sh "mvn clean install -U -B -e -fae -Dmaven.compiler.fork=true -Pci-build"
                                        }
                            }
                        }
                    }
                    post {
                        always {
                            sh "rm -f /tmp/archiva-master-jdk-10-${env.JOB_NAME}.xml"
                        }
                        success {
                            cleanWs deleteDirs: true, notFailBuild: true, patterns: [[pattern: '.repository', type: 'EXCLUDE']]
                        }
                    }
                }
//                stage('JDK11') {
//                    environment {
//                        ARCHIVA_USER_CONFIG_FILE = '/tmp/archiva-master-jdk-11-${env.JOB_NAME}.xml'
//                    }
//                    steps {
//                        ws("${env.JOB_NAME}-JDK10") {
//                            checkout scm
//                            timeout(120) {
//                                withMaven(maven: buildMvn, jdk: buildJdk11,
//                                          mavenSettingsConfig: deploySettings,
//                                          mavenLocalRepo: ".repository",
//                                          publisherStrategy: 'EXPLICIT',
//                                          mavenOpts: mavenOpts,
//                                          options: [junitPublisher(disabled: false, ignoreAttachments: false)]
//                                )
//                                        {
//                                            sh "mvn clean install -U -B -e -fae -Dmaven.compiler.fork=true -Pci-build"
//                                        }
//                            }
//                        }
//                    }
//                    post {
//                        always {
//                            sh "rm -f /tmp/archiva-master-jdk-11-${env.JOB_NAME}.xml"
//                        }
//                        success {
//                            cleanWs deleteDirs: true, notFailBuild: true, patterns: [[pattern: '.repository', type: 'EXCLUDE']]
//                        }
//                    }
//                }
            }
        }

    }

    post {
        unstable {
            notifyBuild("Unstable Build")
        }
        success {
            script {
                def previousResult = currentBuild.previousBuild?.result
                if (previousResult && !currentBuild.resultIsWorseOrEqualTo(previousResult)) {
                    notifyBuild("Fixed")
                }
            }
        }
    }
}

// Send a notification about the build status
def notifyBuild(String buildStatus) {
    // default the value
    buildStatus = buildStatus ?: "UNKNOWN"

    def email = "notifications@archiva.apache.org"
    def summary = "${env.JOB_NAME}#${env.BUILD_NUMBER} - ${buildStatus} - ${currentBuild?.currentResult}"
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

// vim: et:ts=4:sw=4:ft=groovy
