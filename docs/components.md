# Component Documentation

## Core Components

### 1. Annotation System

#### AnnotationController
- Handles HTTP requests for annotation operations
- Manages annotation creation, retrieval, and updates
- Integrates with disease classification system

#### AnnotationService
- Implements business logic for annotations
- Manages annotation state transitions
- Coordinates with AI processing system

#### AnnotationRepository
- MongoDB repository for annotation persistence
- Custom queries for annotation retrieval
- Transaction management

### 2. Disease Classification

#### DiseaseSpectrumService
- Manages disease spectrum classification
- Organ-specific disease mapping
- Validation and business rules

#### GradingService
- Handles disease grading
- Manages grading criteria
- Validates grading rules

#### SubtypeService
- Manages disease subtypes
- Handles subtype classification
- Validates subtype rules

### 3. AI Processing

#### TaskProcessorService
- Manages AI task processing
- EC2 instance management
- Task queue handling
- Error recovery and retry logic

#### EC2Service
- AWS EC2 instance operations
- Instance state management
- Resource optimization

### 4. User Management

#### UserController
- User management endpoints
- Authentication and authorization
- User profile management

#### UserService
- User business logic
- Role management
- Security operations

## Data Models

### Annotation
```java
public class Annotation {
    private String id;
    private String description;
    private String biologicalType;
    private String shape;
    private String annotationType;
    private DiseaseSpectrum diseaseSpectrum;
    private Subtype subtype;
    private Grading grading;
    private User annotator;
    private Date createdAt;
    private Date updatedAt;
}
```

### DiseaseSpectrum
```java
public class DiseaseSpectrum {
    private String id;
    private String name;
    private Organ organ;
    private List<Grading> gradings;
}
```

### Grading
```java
public class Grading {
    private String id;
    private String name;
    private DiseaseSpectrum diseaseSpectrum;
    private String description;
}
```

## Configuration

### Application Properties
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

### Docker Configuration
```yaml
version: '3'
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_DATA_MONGODB_URI=mongodb://mongodb:27017/diagnostic
    depends_on:
      - mongodb
  mongodb:
    image: mongo:latest
    ports:
      - "27017:27017"
```

## Testing

### Unit Tests
- Controller tests
- Service tests
- Repository tests
- Integration tests

### Test Utilities
- Test data generators
- Mock services
- Test configurations

## Deployment

### Requirements
- Java 17
- MongoDB
- Docker
- AWS Account

### Steps
1. Build the application
2. Configure environment variables
3. Start MongoDB
4. Deploy the application
5. Configure AWS resources

## Monitoring

### Logging
- Application logs
- Error tracking
- Performance metrics

### Health Checks
- Application health
- Database connectivity
- AWS service status 