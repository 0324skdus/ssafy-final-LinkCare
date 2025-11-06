# EC2 배포 가이드

LinkCare AI 서버를 AWS EC2에 배포하는 전체 과정입니다.

## 사전 준비

### 1. Docker 이미지 빌드 (로컬)

```bash
# AI 디렉토리에서 실행
cd C:\Users\SSAFY\Desktop\LinkCare\S13P31A307\AI

# Docker 이미지 빌드
docker build -t linkcare-ai:latest .

# 이미지 확인
docker images linkcare-ai
```

### 2. Docker Hub에 이미지 푸시

```bash
# Docker Hub 로그인
docker login

# 이미지 태그 (your-dockerhub-username을 실제 계정명으로 변경)
docker tag linkcare-ai:latest your-dockerhub-username/linkcare-ai:latest

# 푸시
docker push your-dockerhub-username/linkcare-ai:latest
```

**대안: AWS ECR 사용**
```bash
# ECR 로그인 (region과 account-id를 실제 값으로 변경)
aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.ap-northeast-2.amazonaws.com

# ECR 리포지토리 생성
aws ecr create-repository --repository-name linkcare-ai --region ap-northeast-2

# 이미지 태그 및 푸시
docker tag linkcare-ai:latest <account-id>.dkr.ecr.ap-northeast-2.amazonaws.com/linkcare-ai:latest
docker push <account-id>.dkr.ecr.ap-northeast-2.amazonaws.com/linkcare-ai:latest
```

---

## EC2 인스턴스 설정

### 1. EC2 인스턴스 생성

**권장 스펙**:
- **Instance Type**: t3.medium (2 vCPU, 4GB RAM) 이상
- **OS**: Ubuntu 22.04 LTS
- **Storage**: 20GB GP3
- **Security Group**:
  - SSH (22) - 본인 IP만 허용
  - HTTP (8000) - Spring Boot 백엔드 IP 허용

### 2. EC2에 SSH 접속

```bash
ssh -i your-key.pem ubuntu@your-ec2-public-ip
```

### 3. Docker 설치

```bash
# 패키지 업데이트
sudo apt update

# Docker 설치
sudo apt install -y docker.io

# Docker 서비스 시작
sudo systemctl start docker
sudo systemctl enable docker

# 현재 사용자를 docker 그룹에 추가 (sudo 없이 실행 가능)
sudo usermod -aG docker $USER

# 로그아웃 후 재접속 (그룹 변경 적용)
exit
```

재접속 후:
```bash
# Docker 설치 확인
docker --version
```

---

## 모델 파일 배포

### 방법 1: SCP로 직접 업로드

**로컬에서 실행** (Windows PowerShell):
```bash
scp -i your-key.pem -r C:\Users\SSAFY\Desktop\LinkCare\S13P31A307\AI\models ubuntu@your-ec2-public-ip:/home/ubuntu/
```

### 방법 2: AWS S3 사용 (권장)

**로컬에서 S3에 업로드**:
```bash
# S3 버킷 생성 (최초 1회)
aws s3 mb s3://linkcare-ai-models --region ap-northeast-2

# 모델 파일 업로드
aws s3 sync models/ s3://linkcare-ai-models/
```

**EC2에서 S3에서 다운로드**:
```bash
# AWS CLI 설치
sudo apt install -y awscli

# EC2에 IAM Role 할당 필요 (S3 읽기 권한)
# 또는 aws configure로 액세스 키 설정

# 모델 다운로드
mkdir -p /home/ubuntu/models
aws s3 sync s3://linkcare-ai-models/ /home/ubuntu/models/

# 파일 확인
ls -lh /home/ubuntu/models/
```

---

## 컨테이너 실행

### 1. Docker 이미지 Pull

**Docker Hub 사용 시**:
```bash
docker pull your-dockerhub-username/linkcare-ai:latest
```

**AWS ECR 사용 시**:
```bash
# ECR 로그인
aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.ap-northeast-2.amazonaws.com

# 이미지 Pull
docker pull <account-id>.dkr.ecr.ap-northeast-2.amazonaws.com/linkcare-ai:latest
```

### 2. 컨테이너 실행

**Docker Hub 이미지**:
```bash
docker run -d \
  --name linkcare-ai \
  --restart unless-stopped \
  -p 8000:8000 \
  -v /home/ubuntu/models:/app/app/models \
  your-dockerhub-username/linkcare-ai:latest
```

**AWS ECR 이미지**:
```bash
docker run -d \
  --name linkcare-ai \
  --restart unless-stopped \
  -p 8000:8000 \
  -v /home/ubuntu/models:/app/app/models \
  <account-id>.dkr.ecr.ap-northeast-2.amazonaws.com/linkcare-ai:latest
```

**옵션 설명**:
- `-d`: 백그라운드 실행
- `--name linkcare-ai`: 컨테이너 이름 지정
- `--restart unless-stopped`: 재부팅 시 자동 시작
- `-p 8000:8000`: 포트 매핑
- `-v /home/ubuntu/models:/app/app/models`: 모델 파일 마운트

### 3. 실행 확인

```bash
# 컨테이너 상태 확인
docker ps

# 로그 확인
docker logs linkcare-ai

# 실시간 로그
docker logs -f linkcare-ai
```

---

## API 테스트

### 1. 헬스 체크

```bash
curl http://localhost:8000/health
# 응답: {"status":"ok"}
```

### 2. 예측 API 테스트

```bash
# 걸음수 예측
curl -X POST http://localhost:8000/predict/steps \
  -H "Content-Type: application/json" \
  -d '{
    "member_count": 4,
    "avg_age": 35.0,
    "avg_bmi": 23.5,
    "group_steps_mean_3w": 8000.0,
    "group_steps_var_3w": 500000.0,
    "group_duration_mean_3w": 45.0,
    "member_steps_var": 1500.0
  }'
# 응답: {"predicted_growth_rate": 1.03}

# 칼로리 예측
curl -X POST http://localhost:8000/predict/kcal \
  -H "Content-Type: application/json" \
  -d '{
    "member_count": 4,
    "avg_age": 35.0,
    "avg_bmi": 23.5,
    "group_kcal_mean_3w": 2500.0,
    "group_kcal_var_3w": 10000.0,
    "group_duration_mean_3w": 45.0,
    "member_steps_var": 1500.0
  }'
```

### 3. 외부에서 테스트

로컬 PC에서:
```bash
curl http://your-ec2-public-ip:8000/health
```

---

## 운영 관리

### 컨테이너 관리

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
```

### 모델 업데이트

```bash
# 1. 로컬에서 S3에 새 모델 업로드
aws s3 sync models/ s3://linkcare-ai-models/

# 2. EC2에서 다운로드
aws s3 sync s3://linkcare-ai-models/ /home/ubuntu/models/

# 3. 컨테이너 재시작 (모델 재로드)
docker restart linkcare-ai
```

또는 API로 모델 재로드:
```bash
curl -X POST http://localhost:8000/reload_model
# 응답: {"status":"reloaded"}
```

### 이미지 업데이트

```bash
# 1. 로컬에서 새 이미지 빌드 및 푸시
docker build -t linkcare-ai:latest .
docker tag linkcare-ai:latest your-dockerhub-username/linkcare-ai:latest
docker push your-dockerhub-username/linkcare-ai:latest

# 2. EC2에서 Pull 및 재시작
docker pull your-dockerhub-username/linkcare-ai:latest
docker stop linkcare-ai
docker rm linkcare-ai
docker run -d \
  --name linkcare-ai \
  --restart unless-stopped \
  -p 8000:8000 \
  -v /home/ubuntu/models:/app/app/models \
  your-dockerhub-username/linkcare-ai:latest
```

---

## Spring Boot 백엔드 연동

백엔드에서 AI 서버 호출:

```kotlin
// application.yml
ai:
  server:
    url: http://your-ec2-private-ip:8000

// RestTemplate 또는 WebClient 사용
val response = restTemplate.postForObject(
    "${aiServerUrl}/predict/steps",
    stepsRequest,
    GoalPredictResponse::class.java
)
```

**보안 고려사항**:
- EC2 Security Group에서 백엔드 서버 IP만 허용
- VPC 내부 통신 사용 (Private IP)
- HTTPS 적용 시 Nginx 리버스 프록시 사용

---

## 트러블슈팅

### 컨테이너가 시작되지 않음

```bash
# 로그 확인
docker logs linkcare-ai

# 일반적인 원인:
# 1. 모델 파일 경로 문제
ls -lh /home/ubuntu/models/

# 2. 포트 충돌
sudo lsof -i :8000
```

### 모델 파일을 찾을 수 없음

```bash
# 마운트 확인
docker exec linkcare-ai ls -la /app/app/models/

# 출력 예시:
# -rw-r--r-- 1 root root 12345 Jan 01 00:00 goal_steps_model.txt
# -rw-r--r-- 1 root root 12345 Jan 01 00:00 goal_kcal_model.txt
# -rw-r--r-- 1 root root 12345 Jan 01 00:00 goal_duration_model.txt
# -rw-r--r-- 1 root root 12345 Jan 01 00:00 goal_distance_model.txt
```

### 메모리 부족

```bash
# 메모리 사용량 확인
docker stats linkcare-ai

# 대용량 모델의 경우 인스턴스 업그레이드 필요 (t3.large 이상)
```

---

## 비용 최적화

### 1. EC2 Instance Scheduler
- 개발 환경: 업무 시간만 실행 (9AM-6PM)
- AWS EventBridge + Lambda로 자동 시작/중지

### 2. Spot Instance
- 비용 70% 절감
- 단, 중단 가능성 있음 (프로덕션에는 비권장)

### 3. Auto Scaling
- 트래픽에 따라 인스턴스 자동 증감
- ALB + Target Group 구성 필요

---

## 모니터링

### CloudWatch Logs 연동

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

### 헬스 체크 스크립트

```bash
# /home/ubuntu/healthcheck.sh 생성
#!/bin/bash
RESPONSE=$(curl -s http://localhost:8000/health)
if [ "$RESPONSE" != '{"status":"ok"}' ]; then
  echo "Health check failed"
  # Slack 알림 등
  exit 1
fi
```

cron 등록:
```bash
# 5분마다 헬스 체크
*/5 * * * * /home/ubuntu/healthcheck.sh
```

---

## 다음 단계

1. ✅ Docker 이미지 빌드 및 푸시
2. ✅ EC2 인스턴스 설정
3. ✅ 모델 배포 및 컨테이너 실행
4. ⏳ Spring Boot 백엔드 연동
5. ⏳ HTTPS 적용 (Nginx + Let's Encrypt)
6. ⏳ CI/CD 파이프라인 구축 (GitHub Actions)
7. ⏳ 모니터링 및 알림 설정 (CloudWatch)

---

## 문의

배포 중 문제가 발생하면 다음을 확인하세요:
1. Docker 로그: `docker logs linkcare-ai`
2. EC2 Security Group 설정
3. 모델 파일 마운트 확인
4. 네트워크 연결 상태
