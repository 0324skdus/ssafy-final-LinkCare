pipeline {
    agent any

    environment {
        DOCKER_REGISTRY = "docker.io"
        DOCKER_TAG = "${BUILD_NUMBER}"

        DEPLOY_USER = credentials('DEPLOY_USER')
        DEPLOY_HOST = credentials('DEPLOY_HOST')
        DEPLOY_DIR  = credentials('DEPLOY_DIR')

        JWT_SECRET = credentials('JWT_SECRET')

        DB_USER_PASSWORD = credentials('DB_USER_PASSWORD')

        GMAIL_PASSWORD = credentials('GMAIL_PASSWORD')

        GOOGLE_CLIENT_ID = credentials('GOOGLE_CLIENT_ID')
        KAKAO_CLIENT_ID = credentials('KAKAO_CLIENT_ID')
        KAKAO_REDIRECT_URI = credentials('KAKAO_REDIRECT_URI')

        AWS_ACCESS_KEY = credentials('AWS_ACCESS_KEY')
        AWS_SECRET_KEY = credentials('AWS_SECRET_KEY')

        GMS_KEY = credentials('GMS_KEY')
    }

  stages {
         stage('Check Backend Changes') {
               steps {
                   script {
                       def backendChanged = sh(
                           script: "git diff --name-only HEAD~1 HEAD | grep -E '^(Backend|backend)/' || true",
                           returnStdout: true
                       ).trim()
                       if (!backendChanged) {
                           echo "No backend changes. Skipping pipeline."
                           catchError(buildResult: 'SUCCESS', stageResult: 'SUCCESS') { error('No backend changes') }
                       }
                   }
               }
         }

         stage('Build JAR') {
             steps {
                 dir('Backend/LinkCare') {
                     sh 'chmod +x gradlew'
                     sh './gradlew clean build -x test'
                 }
             }
         }

         stage('Build & Push Docker Image') {
            steps {
                dir('Backend/LinkCare') {
                    withCredentials([usernamePassword(
                        credentialsId: 'DOCKER_HUB',
                        usernameVariable: 'DOCKER_USERNAME',
                        passwordVariable: 'DOCKER_PASSWORD'
                    )]) {
                        sh """
                            echo $DOCKER_PASSWORD | docker login -u $DOCKER_USERNAME --password-stdin
                            docker build --label project=backend -t $DOCKER_REGISTRY/$DOCKER_USERNAME/backend:$DOCKER_TAG -f Dockerfile .
                            docker push $DOCKER_REGISTRY/$DOCKER_USERNAME/backend:$DOCKER_TAG
                            docker logout
                        """
                    }
                }
            }
         }

         stage('Deploy to Server') {
             steps {
                 withCredentials([usernamePassword(
                     credentialsId: 'DOCKER_HUB',
                     usernameVariable: 'DOCKER_USERNAME',
                     passwordVariable: 'DOCKER_PASSWORD'
                 )]) {
                     sshagent(['ec2-ssh-key']) {
                         // 1.  .env 파일 로컬에서 생성 (Firebase JSON → Base64)
                         sh """
                                cat > /tmp/deploy.env <<-EOF
                                DOCKER_REGISTRY=${DOCKER_REGISTRY}
                                DOCKER_USERNAME=${DOCKER_USERNAME}
                                DOCKER_TAG=${DOCKER_TAG}
                                DOCKER_PASSWORD=${DOCKER_PASSWORD}
                                JWT_SECRET=${JWT_SECRET}
                                DB_USER_PASSWORD=${DB_USER_PASSWORD}
                                GMAIL_PASSWORD=${GMAIL_PASSWORD}
                                GOOGLE_CLIENT_ID=${GOOGLE_CLIENT_ID}
                                KAKAO_CLIENT_ID=${KAKAO_CLIENT_ID}
                                KAKAO_REDIRECT_URI=${KAKAO_REDIRECT_URI}
                                AWS_ACCESS_KEY=${AWS_ACCESS_KEY}
                                AWS_SECRET_KEY=${AWS_SECRET_KEY}
                                GMS_KEY=${GMS_KEY}
                                SPRING_PROFILES_ACTIVE=prod
                                FIREBASE_CREDENTIALS_PATH=file:/run/secrets/firebase.json
                                EOF
                            """

                         // 2. 디렉토리 생성
                         sh """
                             ssh -o StrictHostKeyChecking=no ${DEPLOY_USER}@${DEPLOY_HOST} "mkdir -p ${DEPLOY_DIR}"
                         """

                         // 3. 파일 복사
                         sh """
                             scp -o StrictHostKeyChecking=no /tmp/deploy.env ${DEPLOY_USER}@${DEPLOY_HOST}:${DEPLOY_DIR}/.env
                             scp -o StrictHostKeyChecking=no Infra/docker-compose.yml ${DEPLOY_USER}@${DEPLOY_HOST}:${DEPLOY_DIR}/docker-compose.yml
                         """

                         // 4. 배포 실행
                         sh """
                             ssh -o StrictHostKeyChecking=no ${DEPLOY_USER}@${DEPLOY_HOST} "
                                 cd ${DEPLOY_DIR}

                                 echo 'Pull latest image...'
                                 docker pull ${DOCKER_REGISTRY}/${DOCKER_USERNAME}/backend:${DOCKER_TAG}

                                 echo 'Stop old containers...'
                                 docker compose down || true

                                 echo 'Start new containers...'
                                 docker compose up -d

                                 echo 'Cleanup old images...'
                                 docker image prune -f --filter 'label=project=backend'
                             "
                         """

                         // 5. .env 파일 정리
                         sh "rm -f /tmp/deploy.env"
                     }
                 }
             }
         }

         stage('Verify Deployment') {
             steps {
                 sshagent(['ec2-ssh-key']) {
                     sh """
                         ssh $DEPLOY_USER@$DEPLOY_HOST "
                             echo "=== Running Containers ==="
                             docker ps
                         "
                     """
                 }
             }
         }
     }

     post {
         success {
             echo 'Deployment succeeded!'
         }
         failure {
             echo 'Deployment failed. Check logs for details.'
         }
     }
 }