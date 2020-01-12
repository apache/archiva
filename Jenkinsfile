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
buildJdk = 'JDK 1.8 (latest)'
buildJdk9 = 'JDK 1.9 (latest)'
buildJdk10 = 'JDK 10 (latest)'
buildJdk11 = 'JDK 11 (latest)'
buildMvn = 'Maven 3.5.4'
//localRepository = ".repository"
//localRepository = "../.maven_repositories/${env.EXECUTOR_NUMBER}"
mavenOpts = '-Xms1g -Xmx2g -Djava.awt.headless=true'
publishers = [artifactsPublisher(disabled: false),
              junitPublisher(disabled: false, ignoreAttachments: false),
              pipelineGraphPublisher(disabled: false),mavenLinkerPublisher(disabled: false)]

cmdLine = (env.NONAPACHEORG_RUN != 'y' && env.BRANCH_NAME == 'master') ? "clean deploy" : "clean install"


        INTEGRATION_PIPELINE = "Archiva-IntegrationTests-Gitbox"

pipeline {
    agent {
        label "${LABEL}"
    }
    // Build should also start, if redback has been built successfully
    triggers { 
        upstream(upstreamProjects: 'Archiva-TLP-Gitbox/archiva-redback-core/master,Archiva-TLP-Gitbox/archiva-parent/master', threshold: hudson.model.Result.SUCCESS) 
    }
    options {
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '7', artifactNumToKeepStr: '5'))
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
                                sh "mvn ${cmdLine} -B -U -e -fae -Dmaven.compiler.fork=true -Pci-build -T${THREADS}"
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



        stage('Postbuild') {
            parallel {
                stage('IntegrationTest') {
                    steps {
                        build(job: "${INTEGRATION_PIPELINE}/archiva/${env.BRANCH_NAME}", propagate: false, quietPeriod: 5, wait: false)
                    }
                }

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
                                            sh "mvn clean install -U -B -e -fae -Dmaven.compiler.fork=true -Pci-build -T${THREADS}"
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

// vim: et:ts=4:sw=4:ft=groovy
