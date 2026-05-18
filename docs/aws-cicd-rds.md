# AWS CI/CD, ECR, ECS, and RDS Setup

This project deploys as a Dockerized Spring Boot app:

- GitHub Actions builds and tests the app.
- Docker image is pushed to Amazon ECR.
- Amazon ECS Fargate runs the container.
- Amazon RDS MySQL stores application data.
- GitHub Actions uses AWS OIDC, so no long-lived AWS keys are stored in GitHub.

## 1. Create RDS MySQL

1. Open AWS Console -> RDS -> Create database.
2. Engine: MySQL.
3. Template: Free tier for testing, Production for real use.
4. DB identifier: `anushabazaar-db`.
5. Master username: `admin`.
6. Master password: create a strong password.
7. Database name: `anushabazaar`.
8. Public access: `No` for production.
9. Security group: allow inbound MySQL `3306` only from the ECS service security group.

Final JDBC URL format:

```text
jdbc:mysql://<rds-endpoint>:3306/anushabazaar?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata
```

## 2. Store Secrets In AWS Secrets Manager

Create two plaintext secrets:

```text
anushabazaar/prod/db-password
anushabazaar/prod/jwt-secret
```

The JWT secret should be at least 64 characters.

Copy both secret ARNs. They will be used in GitHub repository secrets.

## 3. Create ECR Repository

Open AWS Console -> ECR -> Create repository:

```text
anushabazaar-backend
```

## 4. Create CloudWatch Log Group

Create a log group:

```text
/ecs/anushabazaar-backend
```

## 5. Create ECS Cluster And Service

1. Open ECS -> Clusters -> Create cluster.
2. Cluster name: `anushabazaar-cluster`.
3. Infrastructure: AWS Fargate.
4. Create an Application Load Balancer with listener `HTTP:80`.
5. Target group health check path:

```text
/actuator/health
```

6. Container port: `8080`.
7. Service name: `anushabazaar-backend-service`.

The first service creation may need a temporary task definition. After GitHub Actions runs once, it will register the real task definition.

## 6. Create IAM Roles

### ECS Task Execution Role

Create or use:

```text
ecsTaskExecutionRole
```

Attach:

```text
AmazonECSTaskExecutionRolePolicy
```

Add permissions to read Secrets Manager secrets:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "secretsmanager:GetSecretValue"
      ],
      "Resource": [
        "arn:aws:secretsmanager:<region>:<account-id>:secret:anushabazaar/prod/*"
      ]
    }
  ]
}
```

### ECS Task Role

Create:

```text
anushabazaar-backend-task-role
```

For now it can have no extra permissions.

## 7. Create GitHub OIDC Role In AWS

Create an IAM Identity Provider:

```text
Provider URL: https://token.actions.githubusercontent.com
Audience: sts.amazonaws.com
```

Create IAM role:

```text
github-actions-anushabazaar-deploy
```

Trust policy:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::<account-id>:oidc-provider/token.actions.githubusercontent.com"
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

Attach this deploy policy, replacing account and region values:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ecr:GetAuthorizationToken"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "ecr:BatchCheckLayerAvailability",
        "ecr:CompleteLayerUpload",
        "ecr:CreateRepository",
        "ecr:DescribeRepositories",
        "ecr:InitiateLayerUpload",
        "ecr:PutImage",
        "ecr:UploadLayerPart"
      ],
      "Resource": "arn:aws:ecr:<region>:<account-id>:repository/anushabazaar-backend"
    },
    {
      "Effect": "Allow",
      "Action": [
        "ecs:DescribeServices",
        "ecs:DescribeTaskDefinition",
        "ecs:RegisterTaskDefinition",
        "ecs:UpdateService"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "iam:PassRole"
      ],
      "Resource": [
        "arn:aws:iam::<account-id>:role/ecsTaskExecutionRole",
        "arn:aws:iam::<account-id>:role/anushabazaar-backend-task-role"
      ]
    }
  ]
}
```

## 8. Add GitHub Repository Variables

GitHub repo -> Settings -> Secrets and variables -> Actions -> Variables:

```text
AWS_REGION=ap-south-1
ECR_REPOSITORY=anushabazaar-backend
ECS_CLUSTER=anushabazaar-cluster
ECS_SERVICE=anushabazaar-backend-service
ECS_TASK_FAMILY=anushabazaar-backend
ECS_CONTAINER_NAME=anushabazaar-backend
ECS_EXECUTION_ROLE_ARN=arn:aws:iam::<account-id>:role/ecsTaskExecutionRole
ECS_TASK_ROLE_ARN=arn:aws:iam::<account-id>:role/anushabazaar-backend-task-role
SPRING_DATASOURCE_URL=jdbc:mysql://<rds-endpoint>:3306/anushabazaar?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata
SPRING_DATASOURCE_USERNAME=admin
```

## 9. Add GitHub Repository Secrets

GitHub repo -> Settings -> Secrets and variables -> Actions -> Secrets:

```text
AWS_GITHUB_ACTIONS_ROLE_ARN=arn:aws:iam::<account-id>:role/github-actions-anushabazaar-deploy
SPRING_DATASOURCE_PASSWORD_SECRET_ARN=arn:aws:secretsmanager:<region>:<account-id>:secret:anushabazaar/prod/db-password-xxxx
APP_JWT_SECRET_ARN=arn:aws:secretsmanager:<region>:<account-id>:secret:anushabazaar/prod/jwt-secret-xxxx
```

## 10. Deploy

Push to `main`:

```powershell
git add .
git commit -m "Add AWS CI/CD pipeline"
git push
```

Then open:

```text
GitHub -> Investment -> Actions -> AWS CI/CD
```

The workflow will:

1. Run tests.
2. Package the Spring Boot jar.
3. Build Docker image.
4. Push image to ECR.
5. Register a new ECS task definition.
6. Update the ECS service.

## 11. Verify

Use the load balancer DNS:

```text
http://<load-balancer-dns>/actuator/health
http://<load-balancer-dns>/swagger-ui.html
```

Expected health response:

```json
{"status":"UP"}
```

## Notes

- Do not commit production database passwords.
- The local defaults in `application.yml` are only for development.
- For production file uploads, local container storage is temporary. Use S3 later if uploaded KYC/receipt files must persist across deployments.
