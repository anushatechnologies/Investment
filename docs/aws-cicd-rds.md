# AWS CI/CD With ECR, EC2 Docker, and RDS

This project deploys like this:

- GitHub Actions runs tests.
- GitHub Actions builds the Spring Boot jar.
- GitHub Actions builds a Docker image.
- GitHub Actions pushes the image to Amazon ECR.
- GitHub Actions SSHs into EC2.
- EC2 pulls the latest ECR image and runs it with Docker.
- Runtime secrets are injected from GitHub Actions secrets into an `.env` file on EC2.
- RDS MySQL is used as the production database.

No ECS is required.

## 1. Create RDS MySQL

Use the same AWS region you selected in the console:

```text
ap-south-2
Asia Pacific (Hyderabad)
```

Recommended cheap setup:

```text
Engine: MySQL
Template: Dev/Test or Sandbox
Deployment: Single-AZ DB instance
Instance: db.t3.micro or db.t4g.micro
Storage: General Purpose SSD gp3
Allocated storage: 20 GiB
DB identifier: investment-db
Master username: admin
Initial database name: anushabazaar
```

Do not choose Multi-AZ cluster, `db.m6gd.large`, `io2`, 400 GiB, or 3000 IOPS unless you intentionally want a large monthly bill.

Final JDBC URL:

```text
jdbc:mysql://<rds-endpoint>:3306/anushabazaar?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata
```

## 2. Create ECR Repository

AWS Console -> ECR -> Create repository:

```text
anushabazaar-backend
```

## 3. Create EC2 Instance

Recommended for testing:

```text
AMI: Amazon Linux 2023
Instance type: t3.micro
Key pair: create/download a .pem key
Security group inbound:
  SSH 22 from your IP
  HTTP 80 from 0.0.0.0/0
  Custom TCP 8080 from 0.0.0.0/0 if you expose app directly
```

For production, use a load balancer and keep app port private.

## 4. Install Docker And AWS CLI On EC2

SSH into EC2:

```bash
ssh -i your-key.pem ec2-user@<ec2-public-ip>
```

Run:

```bash
sudo dnf update -y
sudo dnf install -y docker awscli
sudo systemctl enable docker
sudo systemctl start docker
sudo usermod -aG docker ec2-user
exit
```

SSH again so the Docker group permission is active:

```bash
ssh -i your-key.pem ec2-user@<ec2-public-ip>
docker ps
aws --version
```

## 5. Give EC2 Permission To Pull From ECR

Create an IAM role for EC2:

```text
anushabazaar-ec2-ecr-role
```

Attach policy:

```text
AmazonEC2ContainerRegistryReadOnly
```

Attach this role to the EC2 instance:

```text
EC2 -> Instance -> Actions -> Security -> Modify IAM role
```

## 6. Create GitHub OIDC Role For Pushing To ECR

Create IAM Identity Provider:

```text
Provider URL: https://token.actions.githubusercontent.com
Audience: sts.amazonaws.com
```

Create IAM role:

```text
github-actions-anushabazaar-ecr-deploy
```

Trust policy:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::406223548776:oidc-provider/token.actions.githubusercontent.com"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "token.actions.githubusercontent.com:aud": "sts.amazonaws.com"
        },
        "StringLike": {
          "token.actions.githubusercontent.com:sub": "repo:anushatechnologies/Investment:ref:refs/heads/main"
        }
      }
    }
  ]
}
```

Attach policy for ECR push:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": "ecr:GetAuthorizationToken",
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "ecr:BatchCheckLayerAvailability",
        "ecr:CompleteLayerUpload",
        "ecr:DescribeRepositories",
        "ecr:InitiateLayerUpload",
        "ecr:PutImage",
        "ecr:UploadLayerPart"
      ],
      "Resource": "arn:aws:ecr:ap-south-2:406223548776:repository/anushabazaar-backend"
    }
  ]
}
```

Copy the role ARN.

## 7. Add GitHub Repository Variables

GitHub repo -> Settings -> Secrets and variables -> Actions -> Variables:

```text
AWS_REGION=ap-south-2
ECR_REPOSITORY=anushabazaar-backend
EC2_HOST=<ec2-public-ip-or-dns>
EC2_USER=ec2-user
APP_PORT=8080
SPRING_DATASOURCE_URL=jdbc:mysql://<rds-endpoint>:3306/anushabazaar?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata
SPRING_DATASOURCE_USERNAME=admin
```

## 8. Add GitHub Repository Secrets

GitHub repo -> Settings -> Secrets and variables -> Actions -> Secrets:

```text
AWS_GITHUB_ACTIONS_ROLE_ARN=arn:aws:iam::406223548776:role/github-actions-anushabazaar-ecr-deploy
EC2_SSH_PRIVATE_KEY=<full private key content from your EC2 .pem file>
SPRING_DATASOURCE_PASSWORD=<your RDS master password>
APP_JWT_SECRET=<64+ character JWT secret>
```

Important: `EC2_SSH_PRIVATE_KEY` must include the full key:

```text
-----BEGIN RSA PRIVATE KEY-----
...
-----END RSA PRIVATE KEY-----
```

or:

```text
-----BEGIN OPENSSH PRIVATE KEY-----
...
-----END OPENSSH PRIVATE KEY-----
```

## 9. RDS Security Group

If EC2 connects to RDS privately:

```text
RDS security group inbound:
Type: MySQL/Aurora
Port: 3306
Source: EC2 security group
```

If testing from your laptop:

```text
Source: your IP only
```

Never leave MySQL open to:

```text
0.0.0.0/0
```

## 10. Deploy

After all variables/secrets are added:

```bash
git push origin main
```

Or run manually:

```text
GitHub -> Investment -> Actions -> AWS EC2 CI/CD -> Run workflow
```

The workflow will:

1. Test the backend.
2. Package the jar.
3. Build Docker image.
4. Push image to ECR.
5. SSH into EC2.
6. Write backend secrets into `~/investment-backend/.env`.
7. Pull the new image from ECR.
8. Restart the Docker container.

## 11. Verify

Open:

```text
http://<ec2-public-ip>:8080/actuator/health
http://<ec2-public-ip>:8080/swagger-ui.html
```

Expected:

```json
{"status":"UP"}
```

## Notes

- Local container uploads are stored in Docker volume `investment_uploads`.
- For production KYC/receipt files, S3 is better than local EC2 Docker volume.
- If port `8080` is not open, either open it in the EC2 security group or map `APP_PORT=80`.
