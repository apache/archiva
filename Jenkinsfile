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
LABEL = 'ubuntu && !H23'
buildJdk = 'jdk_1.8_latest'
buildJdk9 = 'jdk_1.9_latest'
buildJdk10 = 'jdk_10_latest'
buildJdk11 = 'jdk_11_latest'
buildMvn = 'maven_3.8.5'
//localRepository = ".repository"
//localRepository = "../.maven_repositories/${env.EXECUTOR_NUMBER}"
mavenOpts = '-Xms1g -Xmx2g -Djava.awt.headless=true'
publishers = [artifactsPublisher(disabled: false),
              junitPublisher(disabled: false, ignoreAttachments: false),
              pipelineGraphPublisher(disabled: false),mavenLinkerPublisher(disabled: false)]

cmdLine = (env.NONAPACHEORG_RUN != 'y' && env.BRANCH_NAME == 'master') ? "clean deploy" : "clean install"

INTEGRATION_PIPELINE = "/Archiva/Archiva-IntegrationTests"

pipeline {
    agent {
        label "${LABEL}"
    }
    // Build should also start, if redback has been built successfully
    triggers { 
        upstream(upstreamProjects: 'Archiva/archiva-projects/archiva-redback-core/master,Archiva/archiva-projects/archiva-components/master,Archiva/archiva-projects/archiva-parent/master', threshold: hudson.model.Result.SUCCESS)
    }
    options {
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '7', artifactNumToKeepStr: '1'))
    }
    parameters {
        booleanParam(name: 'PRECLEANUP', defaultValue: false, description: 'Clears the local maven repository before build.')
        string(name: 'THREADS', defaultValue: '3', description: 'Number of threads for the mvn build (-T option). Must be a integer value>0.')
    }
    environment {          
        LOCAL_REPOSITORY = "../.maven_repositories/${env.EXECUTOR_NUMBER}"
    }


    stages {

        stage('PreCleanup') {
            when {
                expression {
                    params.PRECLEANUP
                }
            }
            steps {
                sh "rm -rf ${env.LOCAL_REPOSITORY}"
            }
        }

        stage('BuildAndDeploy') {
            environment {
                ARCHIVA_USER_CONFIG_FILE = '/tmp/archiva-master-jdk-8-${env.JOB_NAME}.xml'
            }

            steps {
                timeout(120) {
                    withMaven(maven: buildMvn, jdk: buildJdk,
                            mavenLocalRepo: env.LOCAL_REPOSITORY,
                            publisherStrategy: 'EXPLICIT',
                            mavenOpts: mavenOpts,
                            options: publishers )
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
                                sh "mvn ${cmdLine} -B -U -e -fae -Dorg.slf4j.simpleLogger.showThreadName=true -Pci-build -T${params.THREADS}"
                            }
                }
            }
            post {
                always {
                    sh "rm -f /tmp/archiva-master-jdk-8-${env.JOB_NAME}.xml"
                }
                failure {
                    script{
                        asfStandardBuild.notifyBuild("Failure in BuildAndDeploy stage")
                    }
                }
            }
        }

        stage('Test htmlunit') {
            steps {
                timeout(120) {
                    withMaven(maven: buildMvn, jdk: buildJdk,
                            mavenSettingsConfig: deploySettings,
                            mavenLocalRepo: localRepository,
                            publisherStrategy: 'EXPLICIT',
                            options: [concordionPublisher(disabled: true), dependenciesFingerprintPublisher(disabled: true),
                                      findbugsPublisher(disabled: true), artifactsPublisher(disabled: true),
                                      invokerPublisher(disabled: true), jgivenPublisher(disabled: true),
                                      junitPublisher(disabled: true, ignoreAttachments: false),
                                      openTasksPublisher(disabled: true), pipelineGraphPublisher(disabled: true)]
                    )
                            {
                                sh "chmod 755 ./src/ci/scripts/prepareWorkspace.sh"
                                sh "./src/ci/scripts/prepareWorkspace.sh -d '.repository'"
                                // Needs a lot of time to reload the repository files, try without cleanup
                                // Not sure, but maybe
                                // sh "rm -rf .repository"

                                // Run test phase / ignore test failures
                                // -B: Batch mode
                                // -U: Force snapshot update
                                // -e: Produce execution error messages
                                // -fae: Fail at the end
                                // -Dmaven.compiler.fork=true: Compile in a separate forked process
                                // -Pci-server: Profile for CI-Server
                                // -Pit-js: Run the selenium test
                                sh "mvn clean verify -B -V -U -e -fae -DmaxWaitTimeInMs=2000 -Pci-server -Pit-js -DtrimStackTrace=false -Djava.io.tmpdir=.tmp -pl :archiva-webapp-test"

                            }
                }
            }
            post {
                always {
                    junit testResults: '**/target/failsafe-reports/TEST-*.xml'
                }
                failure {
                    notifyBuild("Failure in Htmlunit test stage")
                }
            }
        }

        // Uses a docker container that is started by script. Maybe we could use the docker functionality
        // of the jenkins pipeline in the future.
        stage('Test chrome') {
            steps {
                timeout(120) {
                    withCredentials([[$class : 'UsernamePasswordMultiBinding', credentialsId: DOCKERHUB_CREDS,
                                      usernameVariable: 'DOCKER_HUB_USER', passwordVariable: 'DOCKER_HUB_PW']]) {
                        withMaven(maven: buildMvn, jdk: buildJdk,
                                mavenSettingsConfig: deploySettings,
                                mavenLocalRepo: localRepository,
                                publisherStrategy: 'EXPLICIT',
                                options: [concordionPublisher(disabled: true), dependenciesFingerprintPublisher(disabled: true),
                                          findbugsPublisher(disabled: true), artifactsPublisher(disabled: true),
                                          invokerPublisher(disabled: true), jgivenPublisher(disabled: true),
                                          junitPublisher(disabled: true, ignoreAttachments: false),
                                          openTasksPublisher(disabled: true), pipelineGraphPublisher(disabled: true)]
                        )
                                {
                                    sh "chmod 755 ./src/ci/scripts/prepareWorkspace.sh"
                                    sh "./src/ci/scripts/prepareWorkspace.sh"
                                    sh "chmod 755 src/ci/scripts/container_webtest.sh"
                                    sh "src/ci/scripts/container_webtest.sh start"
                                    // Needs a lot of time to reload the repository files, try without cleanup
                                    // Not sure, but maybe
                                    // sh "rm -rf .repository"

                                    // Run test phase / ignore test failures
                                    // -B: Batch mode
                                    // -U: Force snapshot update
                                    // -e: Produce execution error messages
                                    // -fae: Fail at the end
                                    // -Pci-server: Profile for CI Server
                                    // -Pit-js: Runs the Selenium tests
                                    // -Pchrome: Activates the Selenium Chrome Test Agent
                                    sh "mvn clean verify -B -V -e -fae -DmaxWaitTimeInMs=2000 -DseleniumRemote=true -Pci-server -Pit-js -Pchrome -pl :archiva-webapp-test -DtrimStackTrace=false"
                                }
                    }
                }
            }
            post {
                always {
                    sh "src/ci/scripts/container_webtest.sh stop"
                    junit testResults: '**/target/failsafe-reports/TEST-*.xml'
                }
                failure {
                    notifyBuild("Failure in Chrome test stage")
                }
            }
        }



        stage('Postbuild') {
            parallel {
                stage('JDK11') {
                    environment {
                        ARCHIVA_USER_CONFIG_FILE = '/tmp/archiva-master-jdk-11-${env.JOB_NAME}.xml'
                    }
                    steps {
                        ws("${env.JOB_NAME}-JDK11") {
                            checkout scm
                            timeout(120) {
                                withMaven(maven: buildMvn, jdk: buildJdk11,
                                          mavenLocalRepo: ".repository",
                                          publisherStrategy: 'EXPLICIT',
                                          mavenOpts: mavenOpts,
                                          options: publishers
                                )
                                        {
                                            sh "chmod 755 ./src/ci/scripts/prepareWorkspace.sh"
                                            sh "./src/ci/scripts/prepareWorkspace.sh"
                                            sh "mvn clean install -U -B -e -fae -Dorg.slf4j.simpleLogger.showThreadName=true -Pci-build -T${params.THREADS}"
                                        }
                            }
                        }
                    }
                    post {
                        always {
                            sh "rm -f /tmp/archiva-master-jdk-11-${env.JOB_NAME}.xml"
                        }
                        success {
                            cleanWs()
                        }
                    }
                }
            }
        }

    }

    post {
        unstable {
            script {
                asfStandardBuild.notifyBuild("Unstable Build")
            }
        }
        success {
            script {
                def previousResult = currentBuild.previousBuild?.result
                if (previousResult && !currentBuild.resultIsWorseOrEqualTo(previousResult)) {
                    asfStandardBuild.notifyBuild("Fixed")
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
