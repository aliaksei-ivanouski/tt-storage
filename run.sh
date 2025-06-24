#!/bin/bash

# TT-Storage Docker Compose Runner
# This script runs the entire TT-Storage application using Docker Compose

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if Docker is running
check_docker() {
    if ! docker info > /dev/null 2>&1; then
        print_error "Docker is not running. Please start Docker and try again."
        exit 1
    fi
    print_success "Docker is running"
}

# Function to check if Docker Compose is available
check_docker_compose() {
    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        print_error "Docker Compose is not installed. Please install Docker Compose and try again."
        exit 1
    fi
    print_success "Docker Compose is available"
}

# Function to create .env file if it doesn't exist
create_env_file() {
    if [ ! -f .env ]; then
        print_status "Creating .env file with default values..."
        cat > .env << EOF
# TT-Storage Environment Variables
TT_STORAGE_SERVICE_PORT=8080
MONGODB_HOST=localhost
MONGODB_PORT=27017
MINIO_HOST=localhost
MINIO_PORT=9000
EOF
        print_success "Created .env file with default values"
    else
        print_status "Using existing .env file"
    fi
}

# Function to stop and clean up existing containers
cleanup_existing() {
    print_status "Checking for existing containers..."
    if docker-compose ps | grep -q "tt_storage\|tt_mongodb\|tt_minio"; then
        print_warning "Found existing containers. Stopping and removing them..."
        docker-compose down --volumes --remove-orphans
        print_success "Cleaned up existing containers"
    else
        print_status "No existing containers found"
    fi
}

# Function to build and start services
start_services() {
    print_status "Building and starting TT-Storage services..."
    
    # Create necessary directories
    print_status "Creating necessary directories..."
    mkdir -p _temp-uploads
    mkdir -p _minio-data
    print_success "Directories created"
    
    # Build and start all services
    docker-compose up --build -d
    
    print_success "Services started successfully"
}

# Function to wait for services to be healthy
wait_for_services() {
    print_status "Waiting for services to be healthy..."
    
    # Wait for MongoDB
    print_status "Waiting for MongoDB to be ready..."
    timeout=60
    counter=0
    while [ $counter -lt $timeout ]; do
        if docker-compose exec -T tt-mongodb mongosh --eval "db.runCommand('ping').ok" --quiet > /dev/null 2>&1; then
            print_success "MongoDB is ready"
            break
        fi
        sleep 2
        counter=$((counter + 2))
        echo -n "."
    done
    
    if [ $counter -eq $timeout ]; then
        print_error "MongoDB failed to start within $timeout seconds"
        exit 1
    fi
    
    # Wait for MinIO
    print_status "Waiting for MinIO to be ready..."
    counter=0
    while [ $counter -lt $timeout ]; do
        if docker-compose exec -T tt-minio curl -f http://localhost:9000/minio/health/live > /dev/null 2>&1; then
            print_success "MinIO is ready"
            break
        fi
        sleep 2
        counter=$((counter + 2))
        echo -n "."
    done
    
    if [ $counter -eq $timeout ]; then
        print_error "MinIO failed to start within $timeout seconds"
        exit 1
    fi
    
    # Wait for TT-Storage backend
    print_status "Waiting for TT-Storage backend to be ready..."
    counter=0
    while [ $counter -lt $timeout ]; do
        if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1 || curl -f http://localhost:8080/docs > /dev/null 2>&1; then
            print_success "TT-Storage backend is ready"
            break
        fi
        sleep 2
        counter=$((counter + 2))
        echo -n "."
    done
    
    if [ $counter -eq $timeout ]; then
        print_error "TT-Storage backend failed to start within $timeout seconds"
        exit 1
    fi
    
    echo ""
}

# Function to run tests
run_tests() {
    print_status "Running integration tests..."
    
    # Check if Maven is available on host
    if ! command -v ./mvnw &> /dev/null && ! command -v mvn &> /dev/null; then
        print_warning "Maven not found on host. Skipping tests."
        print_warning "To run tests manually, ensure Maven is installed and run: ./mvnw test"
        return 0
    fi
    
    # Run tests on host machine
    if ./mvnw test; then
        print_success "All tests passed!"
    else
        print_error "Some tests failed. Check the logs above for details."
        print_warning "You can continue using the application even if tests fail."
    fi
}

# Function to display service status
show_status() {
    print_status "Service Status:"
    docker-compose ps
    
    echo ""
    print_success "TT-Storage is now running!"
    echo ""
    echo -e "${GREEN}Available Services:${NC}"
    echo -e "  • TT-Storage API: ${BLUE}http://localhost:8080${NC}"
    echo -e "  • Swagger UI: ${BLUE}http://localhost:8080/docs${NC}"
    echo -e "  • OpenAPI JSON: ${BLUE}http://localhost:8080/v3/api-docs${NC}"
    echo -e "  • MongoDB: ${BLUE}localhost:27017${NC}"
    echo -e "  • MinIO Console: ${BLUE}http://localhost:9000${NC}"
    echo ""
    echo -e "${YELLOW}Default Credentials:${NC}"
    echo -e "  • MinIO Access Key: ${BLUE}minio_user${NC}"
    echo -e "  • MinIO Secret Key: ${BLUE}minio_letmein${NC}"
    echo ""
    echo -e "${YELLOW}To stop the services, run:${NC} ${BLUE}docker-compose down${NC}"
    echo -e "${YELLOW}To view logs, run:${NC} ${BLUE}docker-compose logs -f${NC}"
}

# Function to handle script interruption
cleanup_on_exit() {
    print_warning "Received interrupt signal. Cleaning up..."
    docker-compose down --volumes --remove-orphans
    print_success "Cleanup completed"
    exit 0
}

# Set up signal handlers
trap cleanup_on_exit INT TERM

# Main execution
main() {
    echo -e "${BLUE}================================${NC}"
    echo -e "${BLUE}  TT-Storage Docker Runner${NC}"
    echo -e "${BLUE}================================${NC}"
    echo ""
    
    # Check prerequisites
    check_docker
    check_docker_compose
    
    # Create environment file
    create_env_file
    
    # Clean up existing containers
    cleanup_existing
    
    # Run tests
    run_tests
    
    # Start services
    start_services
    
    # Wait for services to be healthy
    wait_for_services
    
    # Show final status
    show_status
}

# Run main function
main "$@" 