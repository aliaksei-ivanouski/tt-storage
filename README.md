# TT-Storage

A robust file storage service built with Spring Boot that provides secure file upload, download, and management capabilities with support for file tagging and visibility controls.

## 🚀 Project Description

TT-Storage is a RESTful API service that allows users to:

- **Upload files** with metadata including filename, user ID, visibility settings, and tags
- **Download files** through secure direct links
- **Manage file metadata** including renaming and deletion
- **Search and filter files** by tags and visibility
- **List files** with pagination support for both public and user-specific files
- **Tag management** with search capabilities

### Key Features

- **Multi-format file support** with automatic content type detection using Apache Tika
- **File visibility controls** (PUBLIC/PRIVATE) for access management
- **Tag-based organization** with up to 5 tags per file
- **Pagination and sorting** for efficient file listing
- **Comprehensive error handling** with detailed error responses
- **Swagger/OpenAPI documentation** for easy API exploration
- **Docker support** for easy deployment
- **MongoDB integration** for metadata storage
- **MinIO integration** for object storage
- **Large file support** with ability to handle files up to hundreds of GB

### Large File Support

TT-Storage is configured to handle extremely large file uploads efficiently:

- **Maximum file size**: Unlimited (supports hundreds of GB)
- **Maximum request size**: Unlimited (supports hundreds of GB)
- **File size threshold**: 10MB (files larger than this are written to disk)
- **Connection timeout**: 60 minutes for very large uploads
- **Optimized multipart handling** with proper error responses
- **Streaming file processing** to minimize memory usage
- **Disk-based temporary storage** for large files

**Testing Large File Uploads:**
```bash
# Use the provided test script
./test-large-file.sh

# Or manually test with curl
curl -X POST \
  -F "file=@your-large-file.dat" \
  -F "userId=550e8400-e29b-41d4-a716-446655440000" \
  -F "visibility=PUBLIC" \
  -F "tags=large,test" \
  http://localhost:8080/api/v1/files/upload
```

**Configuration Details:**
- Spring Boot multipart settings configured for unlimited file sizes
- Jetty server optimized for very large uploads
- Proper error handling for multipart parsing failures
- Memory-efficient file processing with disk-based temporary storage
- Streaming upload service for handling hundreds of GB files
- Extended connection timeouts for large file transfers

### Technology Stack

- **Backend**: Spring Boot 3.3.3 with Java 17
- **Database**: MongoDB for metadata storage
- **Object Storage**: MinIO for file storage
- **Documentation**: Swagger/OpenAPI 3.0
- **Containerization**: Docker & Docker Compose
- **Build Tool**: Maven
- **Content Detection**: Apache Tika 2.9.2

## 🛠️ How to Run

### Prerequisites

- Java 17 or higher (for local development)
- Maven 3.9+ (for local development)
- Docker and Docker Compose (for containerized deployment)

### Option 1: Quick Start with Docker (Recommended)

The easiest way to run TT-Storage is using the provided `run.sh` script:

1. **Clone the repository**
   ```bash
   git clone git@github.com:aliaksei-ivanouski/tt-storage.git
   cd tt-storage
   ```

2. **Make the script executable and run it**
   ```bash
   chmod +x run.sh
   ./run.sh
   ```

The script will:
- Check if Docker and Docker Compose are available
- Create a `.env` file with default values if it doesn't exist
- Clean up any existing containers
- Build and start all services (TT-Storage backend, MongoDB, MinIO)
- Wait for all services to be healthy
- Display service status and access URLs

**Default Services:**
- TT-Storage API: http://localhost:8080
- Swagger UI: http://localhost:8080/docs
- MongoDB: localhost:27017
- MinIO Console: http://localhost:9000

**Default MinIO Credentials:**
- Access Key: `minio_user`
- Secret Key: `minio_letmein`

### Option 2: Manual Docker Compose

1. **Clone the repository**
   ```bash
   git clone git@github.com:aliaksei-ivanouski/tt-storage.git
   cd tt-storage
   ```

2. **Set up environment variables**
   Create a `.env` file in the root directory with the following variables:
   ```env
   TT_STORAGE_SERVICE_PORT=8080
   MONGODB_HOST=localhost
   MONGODB_PORT=27017
   MINIO_HOST=localhost
   MINIO_PORT=9000
   ```

3. **Build and run with Docker Compose**
   ```bash
   docker-compose up --build
   ```

### Option 3: Local Development

1. **Clone the repository**
   ```bash
   git clone git@github.com:aliaksei-ivanouski/tt-storage.git
   cd tt-storage
   ```

2. **Set up environment variables**
   Create a `.env` file in the root directory with the following variables:
   ```env
   TT_STORAGE_SERVICE_PORT=8080
   MONGODB_HOST=localhost
   MONGODB_PORT=27017
   MINIO_HOST=localhost
   MINIO_PORT=9000
   ```

3. **Start dependencies with Docker Compose**
   ```bash
   docker-compose up -d tt-mongodb tt-minio
   ```

4. **Run the application**
   ```bash
   # Using Maven wrapper
   ./mvnw spring-boot:run
   
   # Or using Maven directly
   mvn spring-boot:run
   ```

### Option 4: Standalone JAR

1. **Build the application**
   ```bash
   ./mvnw clean package
   ```

2. **Run the JAR file**
   ```bash
   java -jar target/tt-storage-0.0.1-SNAPSHOT.jar
   ```

## 📚 Documentation

### API Documentation

Once the application is running, you can access the interactive API documentation at:

- **Swagger UI**: http://localhost:8080/docs
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

### API Endpoints

The service provides the following main endpoints:

#### File Operations (`/api/v1/files`)
- `POST /upload` - Upload a new file
- `GET /{fileId}/users/{userId}` - Download a file
- `GET /public` - List public files with pagination
- `GET /users/{userId}` - List user's files with pagination
- `PUT /{fileId}/rename` - Rename a file
- `DELETE /{fileId}/users/{userId}` - Delete a file

#### Tag Operations (`/api/v1/tags`)
- `GET /` - List all tags with search and pagination

### Postman Collection

A complete Postman collection with example requests is available in the `postman/` directory. Import `postman/postman_collection.json` into Postman to test all API endpoints.

### Configuration

The application configuration is located in `src/main/resources/application.yml` and includes:

- **Server settings**: Port configuration
- **MongoDB connection**: Database URI and credentials
- **MinIO settings**: Object storage configuration
- **File upload limits**: Maximum file and request sizes
- **Swagger configuration**: API documentation settings

## 🏗️ Architecture

The application follows a layered architecture:

- **Web Layer**: REST controllers handling HTTP requests
- **Service Layer**: Business logic implementation
- **Repository Layer**: Data access with MongoDB
- **Storage Layer**: File storage with MinIO
- **Error Handling**: Centralized exception handling

### Key Components

- `FileController` - Handles file-related HTTP requests
- `TagController` - Manages tag operations
- `FileService` - Implements file business logic
- `StorageService` - Manages MinIO file operations
- `FileRepository` - MongoDB data access for file metadata

## 🧪 Testing

Run the test suite using:

```bash
./mvnw test
```

The project includes:
- Unit tests for service layer
- Integration tests with Testcontainers
- MongoDB and MinIO test containers

## 🔧 Development

### Project Structure

```
src/
├── main/
│   ├── java/com/aivanouski/ttstorage/
│   │   ├── file/          # File management
│   │   ├── tag/           # Tag management
│   │   ├── storage/       # Storage operations
│   │   ├── web/           # Web layer
│   │   ├── error/         # Error handling
│   │   ├── validation/    # Validation logic
│   │   └── global/        # Global configurations
│   └── resources/
│       ├── application.yml
│       └── logback.xml
└── test/                  # Test files
```

### Building

```bash
# Clean and compile
./mvnw clean compile

# Run tests
./mvnw test

# Package application
./mvnw package

# Install to local repository
./mvnw install
```

## 🚀 Deployment

### Production Considerations

1. **Environment Variables**: Configure production environment variables
2. **Security**: Use HTTPS in production
3. **Monitoring**: Add health checks and monitoring
4. **Scaling**: Consider horizontal scaling for high load
5. **Backup**: Implement regular backups for MongoDB and MinIO

### Docker Production Build

```bash
# Build production image
docker build -t tt-storage:latest .

# Run with production environment
docker run -p 8080:8080 --env-file .env.prod tt-storage:latest
```

### Useful Commands

```bash
# Start all services
./run.sh

# Stop all services
docker-compose down

# View logs
docker-compose logs -f

# View logs for specific service
docker-compose logs -f tt-storage-backend

# Restart services
docker-compose restart

# Clean up everything (including volumes)
docker-compose down --volumes --remove-orphans
```

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## 📞 Support

For questions or issues, please create an issue in the project repository.

## Docker Configuration

The application is configured to handle large file uploads efficiently in Docker:

- **Memory Limit**: 1GB per container
- **Temporary File Storage**: Uses a volume-mounted directory `./_temp-uploads` for temporary file storage during uploads
- **Jetty Server**: Optimized for large file uploads with extended timeouts
- **Streaming Upload**: Files are processed in chunks to minimize memory usage

### Temporary File Storage

For large file uploads, the application uses a volume-mounted directory outside the container:
- Host path: `./_temp-uploads`
- Container path: `/app/temp-uploads`
- This ensures sufficient disk space for temporary files during upload processing
