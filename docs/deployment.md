# Deployment Guide

## Prerequisites

### System Requirements
- Java 17 JDK
- MongoDB 4.4+
- Docker 20.10+
- AWS CLI configured
- Git

### AWS Requirements
- EC2 instance for AI processing
- IAM roles with appropriate permissions
- Security groups configured
- VPC setup

## Local Development Setup

### 1. Clone the Repository
```bash
git clone <repository-url>
cd DiagnosticRuntime-master
```

### 2. Configure Environment
Create `application-local.yml`:
```yaml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/diagnostic
  security:
    jwt:
      secret: your-secret-key
      expiration: 86400000

aws:
  ec2:
    instance-id: your-instance-id
    region: your-region
```

### 3. Start MongoDB
```bash
docker-compose up -d mongodb
```

### 4. Build and Run
```bash
./mvnw clean install
./mvnw spring-boot:run -Dspring.profiles.active=local
```

## Docker Deployment

### 1. Build Docker Image
```bash
docker build -t diagnostic-runtime .
```

### 2. Run with Docker Compose
```bash
docker-compose up -d
```

### 3. Verify Deployment
```bash
docker ps
curl http://localhost:8080/actuator/health
```

## AWS Deployment

### 1. EC2 Setup
1. Launch EC2 instance
2. Configure security groups
3. Install Docker
4. Configure AWS CLI

### 2. Application Deployment
1. Build application
2. Create Docker image
3. Push to ECR
4. Deploy to EC2

### 3. MongoDB Setup
1. Launch MongoDB Atlas cluster
2. Configure network access
3. Create database user
4. Update connection string

## Configuration Management

### Environment Variables
```bash
export SPRING_DATA_MONGODB_URI=mongodb://your-uri
export AWS_ACCESS_KEY_ID=your-key
export AWS_SECRET_ACCESS_KEY=your-secret
export AWS_REGION=your-region
```

### Application Properties
- Database configuration
- AWS credentials
- JWT settings
- Logging configuration

## Monitoring and Maintenance

### Logging
```bash
# View application logs
docker logs diagnostic-runtime

# View MongoDB logs
docker logs mongodb
```

### Health Checks
```bash
# Application health
curl http://localhost:8080/actuator/health

# Database connectivity
curl http://localhost:8080/actuator/health/db

# AWS service status
curl http://localhost:8080/actuator/health/aws
```

### Backup and Recovery

#### MongoDB Backup
```bash
# Create backup
mongodump --uri="mongodb://your-uri" --out=/backup

# Restore backup
mongorestore --uri="mongodb://your-uri" /backup
```

#### Application Data Backup
```bash
# Backup Docker volumes
docker run --rm -v diagnostic-runtime-data:/data -v /backup:/backup ubuntu tar czf /backup/data.tar.gz /data

# Restore Docker volumes
docker run --rm -v diagnostic-runtime-data:/data -v /backup:/backup ubuntu tar xzf /backup/data.tar.gz -C /data
```

## Troubleshooting

### Common Issues

1. **MongoDB Connection Issues**
   - Check MongoDB service status
   - Verify connection string
   - Check network connectivity

2. **AWS EC2 Issues**
   - Verify IAM permissions
   - Check security group settings
   - Monitor instance status

3. **Application Startup Issues**
   - Check application logs
   - Verify environment variables
   - Check database connectivity

### Log Files
- Application logs: `/var/log/diagnostic-runtime/application.log`
- MongoDB logs: `/var/log/mongodb/mongod.log`
- Docker logs: `docker logs <container-id>`

## Security Considerations

### Network Security
- Configure firewalls
- Enable SSL/TLS
- Restrict network access

### Data Security
- Encrypt sensitive data
- Regular backups
- Access control

### AWS Security
- IAM best practices
- Security group rules
- VPC configuration 