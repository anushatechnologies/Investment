# AnushaTrade Infrastructure Migration Guide: AWS + Supabase

## Overview

This guide details the infrastructure migration of the AnushaTrade platform:

| Component | Old Environment | New Environment |
| :--- | :--- | :--- |
| **Cloud Provider & Region** | AWS (`ap-south-2` Hyderabad) | AWS (`ap-south-1` Mumbai) |
| **Compute / Host** | Old EC2 | New EC2 (`i-0a2e0f3ba74880332`, IP: `13.203.204.190`) |
| **Container Registry** | Old ECR | New ECR (`ap-south-1`, `anushabazaar-backend`) |
| **Database** | AWS RDS MySQL | Supabase PostgreSQL (Managed Postgres 15+) |
| **File / Object Storage** | AWS S3 | Supabase Storage (S3-compatible endpoint) |
| **Reverse Proxy** | Direct Port 8080 | Nginx on EC2 (`80/443` -> internal `8080`) |
| **CI/CD Pipeline** | GitHub Actions (`aws-cicd.yml`) | Updated GitHub Actions targeting `ap-south-1` |

---

## 1. New AWS EC2 Instance Details

- **Instance Name:** `AnushaTrade`
- **Instance ID:** `i-0a2e0f3ba74880332`
- **Region:** `ap-south-1`
- **Public IPv4:** `13.203.204.190`
- **Private IPv4:** `172.31.12.222`
- **Public DNS:** `ec2-13-203-204-190.ap-south-1.compute.amazonaws.com`
- **Instance Type:** `t3.micro`
- **Operating System:** Amazon Linux 2023
- **SSH User:** `ec2-user`
- **Key Pair:** `AnushaTrade`
- **VPC:** `vpc-03389b38eb5eb1f27`
- **Subnet:** `subnet-04d636fedd223d4fb`
- **IMDSv2:** Required

---

## 2. GitHub Secrets & Repository Variables

### GitHub Repository Variables
In GitHub Repository -> **Settings** -> **Secrets and variables** -> **Actions** -> **Variables**:

```text
AWS_REGION=ap-south-1
ECR_REPOSITORY=anushabazaar-backend
EC2_HOST=13.203.204.190
EC2_USER=ec2-user
APP_PORT=8080
SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.PostgreSQLDialect
APP_FILE_STORAGE_PROVIDER=s3
APP_FILE_STORAGE_MODE=s3
APP_FILE_STORAGE_S3_REGION=ap-south-1
```

### GitHub Repository Secrets
In GitHub Repository -> **Settings** -> **Secrets and variables** -> **Actions** -> **Secrets**:

| Secret Name | Description | Source |
| :--- | :--- | :--- |
| `AWS_GITHUB_ACTIONS_ROLE_ARN` | IAM OIDC Role ARN in the new AWS account to push to ECR | New AWS IAM Console |
| `EC2_SSH_PRIVATE_KEY` | Private key (`.pem`) for key pair `AnushaTrade` | Downloaded from AWS EC2 Console |
| `SPRING_DATASOURCE_URL` | Supabase PostgreSQL JDBC connection string | Supabase -> Settings -> Database -> Connection string (JDBC) |
| `SPRING_DATASOURCE_USERNAME` | Supabase DB username (typically `postgres` or `postgres.<project-ref>`) | Supabase Database Settings |
| `SPRING_DATASOURCE_PASSWORD` | Supabase DB password | Supabase Database Settings |
| `APP_FILE_STORAGE_S3_ENDPOINT` | Supabase S3 API URL: `https://<project-ref>.supabase.co/storage/v1/s3` | Supabase -> Project Settings -> Storage -> S3 Credentials |
| `APP_FILE_STORAGE_S3_ACCESS_KEY` | Supabase Storage S3 Access Key ID | Supabase Storage S3 Credentials |
| `APP_FILE_STORAGE_S3_SECRET_KEY` | Supabase Storage S3 Secret Key | Supabase Storage S3 Credentials |
| `APP_FILE_STORAGE_S3_BUCKET` | Supabase Storage Bucket name (e.g. `investments` or `anushabazaar`) | Supabase Storage Bucket |
| `APP_JWT_SECRET` | 64+ char secret key for signing JWT tokens | Existing GitHub Secret / production key |

---

## 3. EC2 Initialization & Docker Setup

Connect to the new EC2 instance:
```bash
ssh -i AnushaTrade.pem ec2-user@13.203.204.190
```

Install Docker and AWS CLI (Amazon Linux 2023):
```bash
sudo dnf update -y
sudo dnf install -y docker awscli
sudo systemctl enable docker
sudo systemctl start docker
sudo usermod -aG docker ec2-user
```

Log out and back in to activate group permissions:
```bash
exit
ssh -i AnushaTrade.pem ec2-user@13.203.204.190
docker ps
```

Ensure EC2 instance IAM role has `AmazonEC2ContainerRegistryReadOnly` policy attached so Docker can pull images from the new ECR repository in `ap-south-1`.

---

## 4. Nginx Reverse Proxy Configuration (Recommended)

To avoid exposing Spring Boot port `8080` directly:

1. Install Nginx:
```bash
sudo dnf install -y nginx
sudo systemctl enable nginx
sudo systemctl start nginx
```

2. Configure `/etc/nginx/conf.d/anushatrade.conf`:
```nginx
server {
    listen 80;
    server_name _;

    client_max_body_size 25M;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 60s;
        proxy_read_timeout 120s;
        proxy_send_timeout 120s;
    }
}
```

3. Test and reload Nginx:
```bash
sudo nginx -t
sudo systemctl reload nginx
```

4. Security Group Rule:
   - Allow Port 80 (HTTP) and Port 443 (HTTPS) from `0.0.0.0/0`.
   - Remove Port 8080 from public access.

---

## 5. Deployment Verification & Health Check

1. Health endpoint check:
```bash
curl -i http://localhost:8080/actuator/health
# or through Nginx:
curl -i http://13.203.204.190/actuator/health
```
Expected response:
```json
{"status":"UP"}
```

2. Container status & logs:
```bash
docker ps
docker logs investment-backend --tail 100
```

3. Database connection check:
Verify logs show successful HikariCP pool connection to Supabase PostgreSQL:
```text
HikariPool-1 - Added connection org.postgresql.jdbc.PgConnection@...
HikariPool-1 - Start completed.
```

4. Supabase Storage upload check:
Verify logs show successful initialization:
```text
Configuring S3 client with custom endpoint: https://<project-ref>.supabase.co/storage/v1/s3
AWS S3 Client successfully initialized for bucket '<bucket-name>'
```

---

## 6. Old AWS Safety & Rollback Procedures

1. **Old AWS Protection:**
   - The old RDS MySQL, S3 bucket, and EC2 instance in `ap-south-2` remain completely intact and active.
   - Do NOT delete or stop any old AWS resources until new environment testing is 100% complete and approved.

2. **Rollback Procedure:**
   - If issues arise in the new environment, point DNS records back to the old EC2 host.
   - The old environment continues serving traffic without downtime.
