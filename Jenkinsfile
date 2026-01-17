pipeline {
    agent {
        docker {
            image 'maven:3.9.4-eclipse-temurin-21'
            args '''
                --user root
                -v /var/run/docker.sock:/var/run/docker.sock
            '''.stripIndent()
        }
    }

    parameters {
        string(name: 'ENV_NAME', defaultValue: 'dev', description: 'Environment profile (dev, staging, prod, etc.)')
    }

    environment {
        HOME = "/root"
        MAVEN_CONFIG = "/root/.m2"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Install Tools') {
            steps {
                sh '''
                    apt-get update
                    apt-get install -y docker.io curl
                    curl -sSLo /usr/bin/yq https://github.com/mikefarah/yq/releases/latest/download/yq_linux_amd64
                    chmod +x /usr/bin/yq
                    docker --version
                    yq --version
                '''
            }
        }

        stage('Extract Configuration') {
            steps {
                script {
                    def profile = params.ENV_NAME?.trim()
                    if (!profile) {
                        error "❌ ENV_NAME is not set!"
                    }

                    def ymlFile = "src/main/resources/application-${profile}.yaml"
                    def defaultYmlFile = "src/main/resources/application.yaml"

                    def exists = sh(script: "[ -f ${ymlFile} ] && echo yes || echo no", returnStdout: true).trim()
                    if (exists != "yes") {
                        echo "⚠️ ${ymlFile} not found. Falling back to ${defaultYmlFile}"
                        ymlFile = defaultYmlFile
                    } else {
                        echo "✅ Using profile-specific config: ${ymlFile}"
                    }

                    def appName = sh(script: "yq '.spring.application.name' ${ymlFile}", returnStdout: true).trim()
                    def containerPort = sh(script: "yq '.server.port' ${ymlFile}", returnStdout: true).trim()
                    def imageName = sh(script: "yq '.spring.application.name' ${ymlFile}", returnStdout: true).trim()
                    def imageTag = profile

                    if (!imageName) {
                        imageName = sh(script: "mvn help:evaluate -Dexpression=project.artifactId -q -DforceStdout", returnStdout: true).trim()
                    }
                    if (!imageTag) {
                        imageTag = sh(script: "mvn help:evaluate -Dexpression=project.version -q -DforceStdout", returnStdout: true).trim()
                    }
                    if (!containerPort) {
                        containerPort = "9090"
                    }

                    env.APP_NAME = appName ?: imageName
                    env.IMAGE_NAME = imageName
                    env.IMAGE_TAG = imageTag
                    env.CONTAINER_PORT = containerPort

                    echo "✔ ENV_NAME: ${profile}"
                    echo "✔ APP_NAME: ${env.APP_NAME}"
                    echo "✔ IMAGE_NAME: ${env.IMAGE_NAME}"
                    echo "✔ IMAGE_TAG: ${env.IMAGE_TAG}"
                    echo "✔ CONTAINER_PORT: ${env.CONTAINER_PORT}"
                }
            }
        }

        stage('Build with Jib') {
		    steps {
		        sh """
		            mvn clean compile jib:dockerBuild \\
		                -Dimage=${IMAGE_NAME}:${IMAGE_TAG}
		        """
		    }
		}


        stage('Run Docker Container') {
            steps {
                sh """
                    docker stop ${IMAGE_NAME} || true
                    docker rm ${IMAGE_NAME} || true
                    docker run -d --name ${IMAGE_NAME} \\
                        -p ${CONTAINER_PORT}:${CONTAINER_PORT} \\
                        -e SPRING_PROFILES_ACTIVE=${params.ENV_NAME} \\
                        -e SERVER_PORT=${CONTAINER_PORT} \\
                        ${IMAGE_NAME}:${IMAGE_TAG}
                """
            }
        }
    }
}
