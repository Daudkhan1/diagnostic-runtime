# API Documentation

## Authentication

All API endpoints require JWT authentication. Include the token in the Authorization header:
```
Authorization: Bearer <token>
```

## Endpoints

### Annotation API

#### Create Annotation
```
POST /api/annotation
```
Creates a new annotation with associated disease entities.

**Request Body:**
```json
{
  "description": "string",
  "biologicalType": "string",
  "shape": "string",
  "annotationType": "string",
  "diseaseSpectrum": {
    "name": "string"
  },
  "subtype": {
    "name": "string"
  },
  "grading": {
    "name": "string"
  }
}
```

**Response:**
```json
{
  "id": "string",
  "description": "string",
  "biologicalType": "string",
  "shape": "string",
  "annotationType": "string",
  "diseaseSpectrum": {
    "id": "string",
    "name": "string"
  },
  "subtype": {
    "id": "string",
    "name": "string"
  },
  "grading": {
    "id": "string",
    "name": "string"
  }
}
```

#### Get Annotation
```
GET /api/annotation/{id}
```
Retrieves annotation details by ID.

### Disease API

#### Get Disease Spectrum by Organ
```
GET /api/disease-spectrum/organ/{organName}
```
Retrieves disease spectrums for a specific organ.

#### Get Grading by Organ and Disease Spectrum
```
GET /api/grading/organ/{organName}/disease-spectrum/{diseaseSpectrumName}
```
Retrieves gradings for a specific organ and disease spectrum.

### User API

#### Get All Users
```
GET /api/user
```
Retrieves all users with optional filtering.

**Query Parameters:**
- role: Filter by user role
- status: Filter by user status
- fullname: Filter by full name
- page: Page number
- size: Page size

### Task Processing API

#### Enqueue Task
```
POST /api/task
```
Enqueues a new AI processing task.

**Request Body:**
```json
{
  "slideId": "string",
  "slideImagePath": "string",
  "caseType": "string"
}
```

## Error Responses

### 400 Bad Request
```json
{
  "timestamp": "string",
  "status": 400,
  "error": "Bad Request",
  "message": "string",
  "path": "string"
}
```

### 401 Unauthorized
```json
{
  "timestamp": "string",
  "status": 401,
  "error": "Unauthorized",
  "message": "string",
  "path": "string"
}
```

### 404 Not Found
```json
{
  "timestamp": "string",
  "status": 404,
  "error": "Not Found",
  "message": "string",
  "path": "string"
}
```

## Rate Limiting

- 100 requests per minute per user
- 1000 requests per minute per IP

## Pagination

All list endpoints support pagination with the following parameters:
- page: Page number (0-based)
- size: Number of items per page
- sort: Sort field and direction (e.g., "createdAt,desc") 