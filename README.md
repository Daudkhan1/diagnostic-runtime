# Diagnostic Runtime Application

## Overview
The Diagnostic Runtime Application is a comprehensive medical diagnostic system that provides tools for medical image annotation, disease classification, and diagnostic reporting. The application supports various medical specialties and provides AI-assisted diagnostic capabilities.

## Features
- Medical Image Annotation
- Disease Classification and Grading
- AI-Assisted Diagnostics
- Patient Case Management
- User Authentication and Authorization
- Report Generation
- Dashboard Analytics

## Technology Stack
- Java 17
- Spring Boot
- MongoDB
- Docker
- AWS EC2 (for AI processing)
- RESTful APIs

## Project Structure
```
src/
├── main/
│   ├── java/
│   │   └── app/
│   │       └── api/
│   │           └── diagnosticruntime/
│   │               ├── annotation/     # Image annotation services
│   │               ├── auth/          # Authentication services
│   │               ├── changelog/     # Database migrations
│   │               ├── common/        # Shared utilities
│   │               ├── config/        # Application configuration
│   │               ├── dashboard/     # Analytics and reporting
│   │               ├── disease/       # Disease classification
│   │               ├── organ/         # Organ-specific services
│   │               ├── patient/       # Patient management
│   │               ├── report/        # Report generation
│   │               ├── slides/        # Slide management
│   │               └── userdetails/   # User management
└── test/           # Test suites
```

## Getting Started

### Prerequisites
- Java 17
- MongoDB
- Docker
- AWS Account (for AI processing)

### Installation
1. Clone the repository
2. Configure application properties
3. Build the project
4. Run the application

### Configuration
See [Configuration Guide](docs/configuration.md) for detailed setup instructions.

## Documentation
- [API Documentation](docs/api.md)
- [Architecture Overview](docs/architecture.md)
- [Component Documentation](docs/components.md)
- [Deployment Guide](docs/deployment.md)

## Contributing
Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code of conduct and the process for submitting pull requests.

## License
This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details. 