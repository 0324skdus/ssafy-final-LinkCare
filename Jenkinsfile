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

        AWS_ACCESS_KEY = credentials('AWS_ACCESS_KEY')
        AWS_SECRET_KEY = credentials('AWS_SECRET_KEY')
    }

  stages {
         stage('Check Backend Changes') {
               steps {
                   script {
                       def backendChanged = sh(
                           script: "git diff --name-only HEAD~1 HEAD | grep '^backend/' || true",
                           returnStdout: true
                       ).trim()
                       if (!backendChanged) {
                           echo "No backend changes. Skipping pipeline."
                           currentBuild.result = 'SUCCESS'
                           return
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
                         sh """
                             ssh -o StrictHostKeyChecking=no $DEPLOY_USER@$DEPLOY_HOST "
                                mkdir -p $DEPLOY_DIR
                             "

                             scp -o StrictHostKeyChecking=no Infra/docker-compose.yml $DEPLOY_USER@$DEPLOY_HOST:$DEPLOY_DIR/docker-compose.yml

                             ssh -o StrictHostKeyChecking=no $DEPLOY_USER@$DEPLOY_HOST "
                                 cd $DEPLOY_DIR

                                 echo "Pull latest image..."
                                 docker pull $DOCKER_REGISTRY/$DOCKER_USERNAME/backend:$DOCKER_TAG

                                 echo "Stop old containers..."
                                 docker-compose down || true

                                 echo "Start new containers..."

                                 export DOCKER_REGISTRY=$DOCKER_REGISTRY
                                 export DOCKER_USERNAME=$DOCKER_USERNAME
                                 export DOCKER_PASSWORD=$DOCKER_PASSWORD
                                 export DB_USER_PASSWORD=$DB_USER_PASSWORD
                                 export GMAIL_PASSWORD=$GMAIL_PASSWORD
                                 export GOOGLE_CLIENT_ID=$GOOGLE_CLIENT_ID
                                 export KAKAO_CLIENT_ID=$KAKAO_CLIENT_ID
                                 export AWS_ACCESS_KEY=$AWS_ACCESS_KEY
                                 export AWS_SECRET_KEY=$AWS_SECRET_KEY

                                 docker-compose up -d --build

                                 echo "Cleanup old images..."
                                 docker image prune -f --filter "label=project=backend"
                             "
                         """
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