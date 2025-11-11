# LinkCare AI 배포 가이드

> **최종 수정**: 2025-01-11
> **버전**: v6
> **배포 환경**: Docker + AWS EC2

---

## 📋 목차

1. [Docker 패키징](#1-docker-패키징)
2. [AWS EC2 설정](#2-aws-ec2-설정)
3. [모델 파일 배포](#3-모델-파일-배포)
4. [컨테이너 실행](#4-컨테이너-실행)
5. [운영 관리](#5-운영-관리)
6. [백엔드 통합](#6-백엔드-통합)
7. [트러블슈팅](#7-트러블슈팅)

---

## 1. Docker 패키징

### 1.1 Dockerfile 확인

**파일**: `Dockerfile`

```dockerfile
# 예시 Dockerfile (프로젝트에 이미 존재)
FROM python:3.12-slim

WORKDIR /app

# 의존성 복사 및 설치
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 앱 코드 복사
COPY app/ ./app/

# 모델 디렉토리 생성 (마운트용)
RUN mkdir -p /app/app/models

# 포트 노출
EXPOSE 8000

# 서버 실행
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
```

---

### 1.2 로컬에서 이미지 빌드

**위치**: AI 디렉토리 (`C:\Users\SSAFY\Desktop\LinkCare\S13P31A307\AI`)

```bash
# Docker 이미지 빌드
docker build -t linkcare-ai:latest .

# 빌드 확인
docker images linkcare-ai

# 출력 예시:
# REPOSITORY      TAG       IMAGE ID       CREATED         SIZE
# linkcare-ai     latest    abc123def456   5 seconds ago   850MB
```

---

### 1.3 로컬 테스트

```bash
# 컨테이너 실행 (로컬 테스트)
docker run -d \
  --name linkcare-ai-test \
  -p 8000:8000 \
  -v $(pwd)/models:/app/app/models \
  linkcare-ai:latest

# 로그 확인
docker logs linkcare-ai-test

# API 테스트
curl http://localhost:8000/health

# 응답: {"status":"ok"}

# 테스트 완료 후 삭제
docker stop linkcare-ai-test
docker rm linkcare-ai-test
```

---

### 1.4 이미지 푸시 (Docker Hub)

```bash
# 1. Docker Hub 로그인
docker login
# Username: your-dockerhub-username
# Password: your-dockerhub-password

# 2. 이미지 태깅
docker tag linkcare-ai:latest your-dockerhub-username/linkcare-ai:latest
docker tag linkcare-ai:latest your-dockerhub-username/linkcare-ai:v6

# 3. 이미지 푸시
docker push your-dockerhub-username/linkcare-ai:latest
docker push your-dockerhub-username/linkcare-ai:v6

# 4. 푸시 확인
# https://hub.docker.com/r/your-dockerhub-username/linkcare-ai
```

---

### 1.5 이미지 푸시 (AWS ECR)

```bash
# 1. ECR 리포지토리 생성
aws ecr create-repository \
  --repository-name linkcare-ai \
  --region ap-northeast-2

# 2. ECR 로그인
aws ecr get-login-password --region ap-northeast-2 | \
  docker login --username AWS --password-stdin \
  <account-id>.dkr.ecr.ap-northeast-2.amazonaws.com

# 3. 이미지 태깅
docker tag linkcare-ai:latest \
  <account-id>.dkr.ecr.ap-northeast-2.amazonaws.com/linkcare-ai:latest

# 4. 이미지 푸시
docker push <account-id>.dkr.ecr.ap-northeast-2.amazonaws.com/linkcare-ai:latest
```

---

## 2. AWS EC2 설정

### 2.1 EC2 인스턴스 생성

**권장 스펙**:

| 항목 | 설정 |
|------|------|
| **Instance Type** | t3.medium (2 vCPU, 4GB RAM) 이상 |
| **OS** | Ubuntu 22.04 LTS |
| **Storage** | 20GB GP3 (General Purpose SSD) |
| **Security Group** | 아래 참조 |

**Security Group 설정**:

| Type | Protocol | Port | Source | Description |
|------|----------|------|--------|-------------|
| SSH | TCP | 22 | 본인 IP | SSH 접속 (관리용) |
| Custom TCP | TCP | 8000 | 백엔드 서버 IP | AI API (백엔드만 접근) |

**중요**: 8000 포트는 **백엔드 서버 IP만** 허용 (보안)

---

### 2.2 EC2 SSH 접속

```bash
# SSH 키 권한 설정 (최초 1회)
chmod 400 your-key.pem

# EC2 접속
ssh -i your-key.pem ubuntu@your-ec2-public-ip

# 접속 성공 시:
# Welcome to Ubuntu 22.04.X LTS ...
```

---

### 2.3 Docker 설치

```bash
# 패키지 업데이트
sudo apt update

# Docker 설치
sudo apt install -y docker.io

# Docker 서비스 시작
sudo systemctl start docker
sudo systemctl enable docker

# Docker 버전 확인
docker --version
# Docker version 24.0.X, build ...

# 현재 사용자를 docker 그룹에 추가 (sudo 없이 실행 가능)
sudo usermod -aG docker $USER

# 로그아웃 후 재접속 (그룹 변경 적용)
exit
```

**재접속 후 확인**:
```bash
# sudo 없이 docker 명령 실행 확인
docker ps
```

---

## 3. 모델 파일 배포

### 3.1 방법 1: SCP로 직접 업로드

**로컬에서 실행** (Windows PowerShell 또는 Git Bash):

```bash
# 모델 디렉토리 전체 업로드
scp -i your-key.pem -r \
  C:\Users\SSAFY\Desktop\LinkCare\S13P31A307\AI\models \
  ubuntu@your-ec2-public-ip:/home/ubuntu/

# 업로드 확인
ssh -i your-key.pem ubuntu@your-ec2-public-ip "ls -lh /home/ubuntu/models/"
```

---

### 3.2 방법 2: AWS S3 사용 (권장)

#### 로컬에서 S3에 업로드

```bash
# 1. S3 버킷 생성 (최초 1회)
aws s3 mb s3://linkcare-ai-models --region ap-northeast-2

# 2. 모델 파일 업로드
cd C:\Users\SSAFY\Desktop\LinkCare\S13P31A307\AI
aws s3 sync models/ s3://linkcare-ai-models/

# 3. 업로드 확인
aws s3 ls s3://linkcare-ai-models/

# 출력:
# 2025-01-11 12:00:00      45678 goal_steps_model.txt
# 2025-01-11 12:00:00      45678 goal_duration_model.txt
# 2025-01-11 12:00:00      45678 goal_kcal_model.txt
# 2025-01-11 12:00:00      45678 goal_distance_model.txt
```

#### EC2에서 S3에서 다운로드

```bash
# 1. AWS CLI 설치 (Ubuntu)
sudo apt install -y awscli

# 2. AWS 자격증명 설정
aws configure
# AWS Access Key ID: YOUR_ACCESS_KEY
# AWS Secret Access Key: YOUR_SECRET_KEY
# Default region name: ap-northeast-2
# Default output format: json

# 또는 EC2 인스턴스에 IAM Role 할당 (권장)
# - S3 읽기 권한 (AmazonS3ReadOnlyAccess)

# 3. 모델 다운로드
mkdir -p /home/ubuntu/models
aws s3 sync s3://linkcare-ai-models/ /home/ubuntu/models/

# 4. 파일 확인
ls -lh /home/ubuntu/models/

# 출력:
# -rw-r--r-- 1 ubuntu ubuntu 45K Jan 11 12:00 goal_steps_model.txt
# -rw-r--r-- 1 ubuntu ubuntu 45K Jan 11 12:00 goal_duration_model.txt
# -rw-r--r-- 1 ubuntu ubuntu 45K Jan 11 12:00 goal_kcal_model.txt
# -rw-r--r-- 1 ubuntu ubuntu 45K Jan 11 12:00 goal_distance_model.txt
```

---

## 4. 컨테이너 실행

### 4.1 Docker Hub 이미지 사용

```bash
# 1. 이미지 Pull
docker pull your-dockerhub-username/linkcare-ai:latest

# 2. 컨테이너 실행
docker run -d \
  --name linkcare-ai \
  --restart unless-stopped \
  -p 8000:8000 \
  -v /home/ubuntu/models:/app/app/models \
  your-dockerhub-username/linkcare-ai:latest

# 3. 실행 확인
docker ps
```

---

### 4.2 AWS ECR 이미지 사용

```bash
# 1. ECR 로그인
aws ecr get-login-password --region ap-northeast-2 | \
  docker login --username AWS --password-stdin \
  <account-id>.dkr.ecr.ap-northeast-2.amazonaws.com

# 2. 이미지 Pull
docker pull <account-id>.dkr.ecr.ap-northeast-2.amazonaws.com/linkcare-ai:latest

# 3. 컨테이너 실행
docker run -d \
  --name linkcare-ai \
  --restart unless-stopped \
  -p 8000:8000 \
  -v /home/ubuntu/models:/app/app/models \
  <account-id>.dkr.ecr.ap-northeast-2.amazonaws.com/linkcare-ai:latest
```

---

### 4.3 실행 옵션 설명

| 옵션 | 설명 |
|------|------|
| `-d` | 백그라운드 실행 (detached mode) |
| `--name linkcare-ai` | 컨테이너 이름 지정 |
| `--restart unless-stopped` | 재부팅 시 자동 시작 (수동 중지 제외) |
| `-p 8000:8000` | 포트 매핑 (호스트:컨테이너) |
| `-v /home/ubuntu/models:/app/app/models` | 모델 파일 마운트 |

---

### 4.4 실행 확인

```bash
# 1. 컨테이너 상태 확인
docker ps

# 출력:
# CONTAINER ID   IMAGE          STATUS         PORTS                    NAMES
# abc123def456   linkcare-ai    Up 10 seconds  0.0.0.0:8000->8000/tcp   linkcare-ai

# 2. 로그 확인
docker logs linkcare-ai

# 출력:
# INFO:     Uvicorn running on http://0.0.0.0:8000
# INFO:     Application startup complete.

# 3. 실시간 로그
docker logs -f linkcare-ai
```

---

### 4.5 API 테스트

```bash
# EC2 내부에서 테스트
curl http://localhost:8000/health
# 응답: {"status":"ok"}

# 예측 API 테스트
curl -X POST http://localhost:8000/predict/steps \
  -H "Content-Type: application/json" \
  -d '{
    "member_count": 4,
    "avg_age": 35.0,
    "avg_bmi": 23.5,
    "group_steps_mean_3w": 224000.0,
    "group_steps_var_3w": 5000.0,
    "group_duration_mean_3w": 1260.0,
    "member_steps_var": 1200.0
  }'

# 응답: {"predicted_growth_rate":1.027}
```

---

### 4.6 외부에서 테스트

**로컬 PC에서 테스트** (Security Group 설정 필요):

```bash
# 헬스 체크
curl http://your-ec2-public-ip:8000/health

# 예측 API 테스트
curl -X POST http://your-ec2-public-ip:8000/predict/steps \
  -H "Content-Type: application/json" \
  -d '{
    "member_count": 4,
    "avg_age": 35.0,
    "avg_bmi": 23.5,
    "group_steps_mean_3w": 224000.0,
    "group_steps_var_3w": 5000.0,
    "group_duration_mean_3w": 1260.0,
    "member_steps_var": 1200.0
  }'
```

---

## 5. 운영 관리

### 5.1 컨테이너 관리

```bash
# 컨테이너 중지
docker stop linkcare-ai

# 컨테이너 시작
docker start linkcare-ai

# 컨테이너 재시작
docker restart linkcare-ai

# 컨테이너 삭제
docker rm -f linkcare-ai

# 로그 보기 (최근 100줄)
docker logs --tail 100 linkcare-ai

# 로그 실시간 모니터링
docker logs -f linkcare-ai

# 컨테이너 내부 접속 (디버깅)
docker exec -it linkcare-ai /bin/bash
```

---

### 5.2 모델 업데이트

#### 방법 1: S3를 통한 업데이트 (권장)

```bash
# 1. 로컬에서 새 모델 S3에 업로드
aws s3 sync models/ s3://linkcare-ai-models/

# 2. EC2에서 다운로드
ssh -i your-key.pem ubuntu@your-ec2-public-ip
aws s3 sync s3://linkcare-ai-models/ /home/ubuntu/models/

# 3. 컨테이너 재시작 (모델 재로드)
docker restart linkcare-ai

# 4. 로그 확인
docker logs linkcare-ai
```

#### 방법 2: API를 통한 재로드

```bash
# 모델 파일만 업데이트한 경우 (컨테이너 재시작 불필요)
curl -X POST http://localhost:8000/reload_model

# 응답: {"status":"reloaded"}
```

---

### 5.3 이미지 업데이트

```bash
# 1. 로컬에서 새 이미지 빌드 및 푸시
docker build -t linkcare-ai:latest .
docker tag linkcare-ai:latest your-dockerhub-username/linkcare-ai:latest
docker push your-dockerhub-username/linkcare-ai:latest

# 2. EC2에서 Pull 및 재배포
ssh -i your-key.pem ubuntu@your-ec2-public-ip

# 이미지 Pull
docker pull your-dockerhub-username/linkcare-ai:latest

# 기존 컨테이너 중지 및 삭제
docker stop linkcare-ai
docker rm linkcare-ai

# 새 컨테이너 실행
docker run -d \
  --name linkcare-ai \
  --restart unless-stopped \
  -p 8000:8000 \
  -v /home/ubuntu/models:/app/app/models \
  your-dockerhub-username/linkcare-ai:latest

# 실행 확인
docker ps
docker logs linkcare-ai
```

---

### 5.4 디스크 공간 관리

```bash
# Docker 이미지/컨테이너 용량 확인
docker system df

# 사용하지 않는 이미지 삭제
docker image prune -a

# 중지된 컨테이너 삭제
docker container prune

# 모두 정리 (주의!)
docker system prune -a
```

---

## 6. 백엔드 통합

### 6.1 Spring Boot 설정

**application.yml** (백엔드):

```yaml
ai:
  server:
    # VPC 내부 Private IP 사용 (보안)
    url: http://your-ec2-private-ip:8000
    # 또는 Public IP (보안 그룹 설정 필요)
    # url: http://your-ec2-public-ip:8000
    timeout: 5000  # 5초
```

---

### 6.2 Kotlin 코드 예시

```kotlin
// RestTemplate 설정
@Configuration
class AiClientConfig {
    @Value("\${ai.server.url}")
    private lateinit var aiServerUrl: String

    @Bean
    fun restTemplate(): RestTemplate {
        val factory = SimpleClientHttpRequestFactory()
        factory.setConnectTimeout(5000)
        factory.setReadTimeout(5000)
        return RestTemplate(factory)
    }
}

// AI 서비스 클라이언트
@Service
class AiGoalService(
    @Value("\${ai.server.url}")
    private val aiServerUrl: String,
    private val restTemplate: RestTemplate
) {
    fun predictStepsGrowthRate(request: StepsRequest): Double {
        val url = "$aiServerUrl/predict/steps"
        val response = restTemplate.postForObject(
            url,
            request,
            GoalPredictResponse::class.java
        )
        return response?.predictedGrowthRate ?: 1.0
    }
}

// 요청/응답 DTO
data class StepsRequest(
    val memberCount: Int,
    val avgAge: Double,
    val avgBmi: Double,
    val groupStepsMean3w: Double,
    val groupStepsVar3w: Double,
    val groupDurationMean3w: Double,
    val memberStepsVar: Double
)

data class GoalPredictResponse(
    val predictedGrowthRate: Double
)
```

---

### 6.3 통합 테스트

```bash
# 백엔드에서 AI 서버 호출 테스트
curl -X POST http://your-backend-url/api/goals/predict \
  -H "Content-Type: application/json" \
  -d '{
    "groupId": 1
  }'

# 응답:
{
  "goalSteps": 230048,
  "goalKcal": 8602,
  "goalDuration": 1300,
  "goalDistance": 172.5,
  "predictedGrowthRates": {
    "steps": 1.027,
    "kcal": 1.024,
    "duration": 1.032,
    "distance": 1.027
  }
}
```

---

## 7. 트러블슈팅

### 7.1 컨테이너가 시작되지 않음

```bash
# 로그 확인
docker logs linkcare-ai

# 일반적인 원인:
# 1. 모델 파일 경로 문제
ls -lh /home/ubuntu/models/

# 2. 포트 충돌
sudo lsof -i :8000

# 3. 이미지 손상
docker pull your-dockerhub-username/linkcare-ai:latest --no-cache
```

---

### 7.2 모델 파일을 찾을 수 없음

```bash
# 마운트 확인
docker exec linkcare-ai ls -la /app/app/models/

# 출력 예시 (정상):
# -rw-r--r-- 1 root root 45678 Jan 11 12:00 goal_steps_model.txt
# -rw-r--r-- 1 root root 45678 Jan 11 12:00 goal_duration_model.txt
# -rw-r--r-- 1 root root 45678 Jan 11 12:00 goal_kcal_model.txt
# -rw-r--r-- 1 root root 45678 Jan 11 12:00 goal_distance_model.txt

# 파일이 없으면 S3에서 재다운로드
aws s3 sync s3://linkcare-ai-models/ /home/ubuntu/models/
docker restart linkcare-ai
```

---

### 7.3 메모리 부족

```bash
# 메모리 사용량 확인
docker stats linkcare-ai

# 출력:
# CONTAINER ID   NAME          CPU %     MEM USAGE / LIMIT     MEM %
# abc123def456   linkcare-ai   5.00%     800MiB / 4GiB         20.00%

# 메모리 사용량이 80% 이상이면 인스턴스 업그레이드
# t3.medium (4GB) → t3.large (8GB)
```

---

### 7.4 외부 접속 불가

```bash
# 1. Security Group 확인
# - 8000 포트가 허용되어 있는지 확인

# 2. 방화벽 확인
sudo ufw status

# 3. 컨테이너 포트 바인딩 확인
docker ps
# PORTS 컬럼에 0.0.0.0:8000->8000/tcp 있어야 함

# 4. 로컬에서 텔넷 테스트
telnet your-ec2-public-ip 8000
```

---

### 7.5 S3 접근 권한 오류

```bash
# 증상:
# fatal error: Unable to locate credentials

# 해결:
# 1. IAM Role 할당 (권장)
#    EC2 → Actions → Security → Modify IAM role
#    → AmazonS3ReadOnlyAccess 역할 추가

# 2. 또는 aws configure로 자격증명 설정
aws configure
```

---

### 7.6 Docker 디스크 용량 부족

```bash
# 디스크 사용량 확인
df -h

# Docker 데이터 정리
docker system prune -a --volumes

# 로그 파일 크기 제한 (docker-compose.yml)
logging:
  driver: "json-file"
  options:
    max-size: "10m"
    max-file: "3"
```

---

## 8. 모니터링 및 알림

### 8.1 헬스 체크 스크립트

**파일**: `/home/ubuntu/healthcheck.sh`

```bash
#!/bin/bash

RESPONSE=$(curl -s http://localhost:8000/health)

if [ "$RESPONSE" != '{"status":"ok"}' ]; then
  echo "Health check failed at $(date)"
  # Slack/Discord 알림 등 추가 가능
  docker restart linkcare-ai
fi
```

**실행 권한 부여**:
```bash
chmod +x /home/ubuntu/healthcheck.sh
```

**cron 등록** (5분마다 체크):
```bash
crontab -e

# 다음 줄 추가:
*/5 * * * * /home/ubuntu/healthcheck.sh >> /var/log/healthcheck.log 2>&1
```

---

### 8.2 CloudWatch Logs 연동

```bash
# CloudWatch Logs 에이전트 설치
sudo apt install -y amazon-cloudwatch-agent

# 컨테이너 로그를 CloudWatch로 전송
docker run -d \
  --name linkcare-ai \
  --restart unless-stopped \
  -p 8000:8000 \
  -v /home/ubuntu/models:/app/app/models \
  --log-driver=awslogs \
  --log-opt awslogs-region=ap-northeast-2 \
  --log-opt awslogs-group=/ecs/linkcare-ai \
  your-dockerhub-username/linkcare-ai:latest
```

---

## 9. 다음 단계

- ✅ Docker 이미지 빌드 및 푸시
- ✅ EC2 인스턴스 설정
- ✅ 모델 배포 및 컨테이너 실행
- ⏳ Spring Boot 백엔드 연동
- ⏳ HTTPS 적용 (Nginx + Let's Encrypt)
- ⏳ CI/CD 파이프라인 구축 (GitHub Actions)
- ⏳ 모니터링 및 알림 설정 (CloudWatch)

---

**작성일**: 2025-01-11
**최종 수정**: 2025-01-11
**버전**: v6.0
