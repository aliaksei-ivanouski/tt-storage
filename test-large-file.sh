#!/bin/bash

# Test script to verify large file upload functionality (2GB and 10GB support)
set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# Function to cleanup on exit
cleanup() {
    echo "Cleaning up..."
    # Clean up any test files that might exist
    rm -f test-large-file*.dat test-10gb-file*.dat test-2gb-file*.dat test-*gb-file*.dat
    if [ ! -z "$APP_PID" ]; then
        echo "Stopping application..."
        kill $APP_PID 2>/dev/null || true
        docker-compose down 2>/dev/null || true
    fi
    echo "Test completed!"
}

# Function to check required containers and volumes
check_requirements() {
    echo "Checking required containers and volumes..."
    
    # Check if Docker is running
    if ! docker info > /dev/null 2>&1; then
        echo -e "${RED}❌ Docker is not running. Please start Docker and try again.${NC}"
        exit 1
    fi
    
    # Check if Docker Compose is available
    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        echo -e "${RED}❌ Docker Compose is not available. Please install Docker Compose and try again.${NC}"
        exit 1
    fi
    
    # Check if required directories exist
    if [ ! -d "_temp-uploads" ]; then
        echo -e "${YELLOW}⚠️  _temp-uploads directory not found. Creating it...${NC}"
        mkdir -p _temp-uploads
    fi
    
    if [ ! -d "_minio-data" ]; then
        echo -e "${YELLOW}⚠️  _minio-data directory not found. Creating it...${NC}"
        mkdir -p _minio-data
    fi
    
    # Check if .env file exists
    if [ ! -f ".env" ]; then
        echo -e "${YELLOW}⚠️  .env file not found. Creating it with default values...${NC}"
        cat > .env << EOF
# TT-Storage Environment Variables
TT_STORAGE_SERVICE_PORT=8080
MONGODB_HOST=localhost
MONGODB_PORT=27017
MINIO_HOST=localhost
MINIO_PORT=9000
EOF
    fi
    
    echo -e "${GREEN}✅ Requirements check passed${NC}"
}

# Set up cleanup trap
trap cleanup EXIT

echo "Testing large file upload functionality for 2GB and 10GB support..."

# Check requirements before starting
check_requirements

# Check if application is already running
echo "Checking if TT-Storage is already running..."
if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo -e "${GREEN}✅ TT-Storage is already running${NC}"
    SERVICES_RUNNING=true
else
    echo -e "${YELLOW}TT-Storage is not running. Starting application...${NC}"
    SERVICES_RUNNING=false
    
    # Start the application with skip options for faster startup
    echo "Starting application (this may take a few minutes)..."
    ./run.sh --skip-tests --skip-health-checks &
    APP_PID=$!
    
    # Wait for application to start with proper health check
    echo "Waiting for application to start..."
    timeout=180
    counter=0
    while [ $counter -lt $timeout ]; do
        if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
            echo -e "${GREEN}✅ TT-Storage is ready${NC}"
            break
        fi
        sleep 5
        counter=$((counter + 5))
        echo -n "."
    done
    
    if [ $counter -eq $timeout ]; then
        echo -e "${RED}❌ Application failed to start within $timeout seconds${NC}"
        echo "Checking service logs..."
        docker-compose logs --tail=20
        exit 1
    fi
    
    echo ""
fi

echo "Application is running. Testing large file upload..."

# Run diagnostics before testing
echo -e "${CYAN}🔍 Running diagnostics before testing...${NC}"

# Check available disk space
AVAILABLE_SPACE=$(df . | awk 'NR==2 {print $4}')
REQUIRED_SPACE=10485760  # 10GB in KB
echo -e "  ${BOLD}Available disk space:${NC} ${BLUE}$(($AVAILABLE_SPACE / 1024))MB${NC}"
echo -e "  ${BOLD}Required space:${NC} ${BLUE}10GB${NC}"

if [ "$AVAILABLE_SPACE" -lt "$REQUIRED_SPACE" ]; then
    echo -e "${RED}❌ Insufficient disk space for 10GB test${NC}"
    exit 1
fi

# Check container memory usage
echo -e "  ${BOLD}Container memory usage:${NC}"
docker stats --no-stream

# Test 2GB file first
echo ""
echo -e "${CYAN}🔍 Testing with 2GB file...${NC}"
TEST_FILENAME_2GB="test-2gb-file-$(date +%s)-$(openssl rand -hex 8).dat"
dd if=/dev/urandom of="$TEST_FILENAME_2GB" bs=1M count=2048 2>/dev/null
echo -e "${GREEN}✅ Created 2GB test file: $TEST_FILENAME_2GB${NC}"

echo "Uploading 2GB test file..."
if command -v gtimeout >/dev/null 2>&1; then
    UPLOAD_RESPONSE_2GB=$(gtimeout 600 curl -s -X POST \
      -F "file=@$TEST_FILENAME_2GB" \
      -F "userId=550e8400-e29b-41d4-a716-446655440000" \
      -F "visibility=PUBLIC" \
      -F "tags=test,2gb" \
      http://localhost:8080/api/v1/files/upload)
elif command -v timeout >/dev/null 2>&1; then
    UPLOAD_RESPONSE_2GB=$(timeout 600 curl -s -X POST \
      -F "file=@$TEST_FILENAME_2GB" \
      -F "userId=550e8400-e29b-41d4-a716-446655440000" \
      -F "visibility=PUBLIC" \
      -F "tags=test,2gb" \
      http://localhost:8080/api/v1/files/upload)
else
    UPLOAD_RESPONSE_2GB=$(curl --max-time 600 -s -X POST \
      -F "file=@$TEST_FILENAME_2GB" \
      -F "userId=550e8400-e29b-41d4-a716-446655440000" \
      -F "visibility=PUBLIC" \
      -F "tags=test,2gb" \
      http://localhost:8080/api/v1/files/upload)
fi

echo "Upload response: $UPLOAD_RESPONSE_2GB"

if echo "$UPLOAD_RESPONSE_2GB" | grep -q '"id"'; then
    echo ""
    echo -e "${BOLD}${GREEN}================================${NC}"
    echo -e "${BOLD}${GREEN}  ✅ 2GB UPLOAD SUCCESSFUL${NC}"
    echo -e "${BOLD}${GREEN}================================${NC}"
    
    FILE_ID_2GB=$(echo "$UPLOAD_RESPONSE_2GB" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
    FILENAME_2GB=$(echo "$UPLOAD_RESPONSE_2GB" | grep -o '"filename":"[^"]*"' | cut -d'"' -f4)
    SIZE_2GB=$(echo "$UPLOAD_RESPONSE_2GB" | grep -o '"size":[0-9]*' | cut -d':' -f2)
    DOWNLOAD_LINK_2GB=$(echo "$UPLOAD_RESPONSE_2GB" | grep -o '"downloadLink":"[^"]*"' | cut -d'"' -f4)
    
    echo -e "${CYAN}📁 File Details:${NC}"
    echo -e "  ${BOLD}File ID:${NC} ${BLUE}$FILE_ID_2GB${NC}"
    echo -e "  ${BOLD}Filename:${NC} ${BLUE}$FILENAME_2GB${NC}"
    echo -e "  ${BOLD}Size:${NC} ${BLUE}$SIZE_2GB bytes ($((${SIZE_2GB:-0} / 1024 / 1024)) MB)${NC}"
    echo -e "  ${BOLD}Download Link:${NC} ${BLUE}$DOWNLOAD_LINK_2GB${NC}"
    echo ""
    
    # Test download (just check headers, don't download the full file)
    echo -e "${CYAN}🔍 Testing download functionality...${NC}"
    DOWNLOAD_RESPONSE=$(curl -s -I http://localhost:8080/api/v1/files/$FILE_ID_2GB/users/550e8400-e29b-41d4-a716-446655440000)
    if echo "$DOWNLOAD_RESPONSE" | grep -q "200 OK"; then
        echo -e "${GREEN}✅ File download test PASSED${NC}"
    else
        echo -e "${RED}❌ File download test FAILED${NC}"
        echo "Response: $DOWNLOAD_RESPONSE"
    fi
    echo ""
    
    rm -f "$TEST_FILENAME_2GB"
    echo -e "${GREEN}✅ Proceeding with 10GB test...${NC}"
else
    echo -e "${RED}❌ 2GB file upload test FAILED${NC}"
    echo "Error Details:"
    echo "  Status: $(echo "$UPLOAD_RESPONSE_2GB" | grep -o '"status":[0-9]*' | cut -d':' -f2)"
    echo "  Error: $(echo "$UPLOAD_RESPONSE_2GB" | grep -o '"error":"[^"]*"' | cut -d'"' -f4)"
    echo "  Message: $(echo "$UPLOAD_RESPONSE_2GB" | grep -o '"message":"[^"]*"' | cut -d'"' -f4)"
    echo "  Code: $(echo "$UPLOAD_RESPONSE_2GB" | grep -o '"code":"[^"]*"' | cut -d'"' -f4)"
    echo "Full Response: $UPLOAD_RESPONSE_2GB"
    rm -f "$TEST_FILENAME_2GB"
    echo -e "${RED}❌ Skipping 10GB test due to 2GB failure${NC}"
    exit 1
fi

# Test 10GB file
echo -e "${CYAN}🔍 Testing with 10GB file...${NC}"
echo "Creating 10GB test file..."
TEST_FILENAME_10GB="test-10gb-file-$(date +%s)-$(openssl rand -hex 8).dat"
dd if=/dev/urandom of="$TEST_FILENAME_10GB" bs=1M count=10240 2>/dev/null
echo -e "${GREEN}✅ Created 10GB test file: $TEST_FILENAME_10GB${NC}"

echo "Uploading 10GB test file..."
if command -v gtimeout >/dev/null 2>&1; then
    UPLOAD_RESPONSE_10GB=$(gtimeout 600 curl -s -X POST \
      -F "file=@$TEST_FILENAME_10GB" \
      -F "userId=550e8400-e29b-41d4-a716-446655440000" \
      -F "visibility=PUBLIC" \
      -F "tags=test,10gb" \
      http://localhost:8080/api/v1/files/upload)
elif command -v timeout >/dev/null 2>&1; then
    UPLOAD_RESPONSE_10GB=$(timeout 600 curl -s -X POST \
      -F "file=@$TEST_FILENAME_10GB" \
      -F "userId=550e8400-e29b-41d4-a716-446655440000" \
      -F "visibility=PUBLIC" \
      -F "tags=test,10gb" \
      http://localhost:8080/api/v1/files/upload)
else
    UPLOAD_RESPONSE_10GB=$(curl --max-time 600 -s -X POST \
      -F "file=@$TEST_FILENAME_10GB" \
      -F "userId=550e8400-e29b-41d4-a716-446655440000" \
      -F "visibility=PUBLIC" \
      -F "tags=test,10gb" \
      http://localhost:8080/api/v1/files/upload)
fi

echo "Upload response: $UPLOAD_RESPONSE_10GB"

if echo "$UPLOAD_RESPONSE_10GB" | grep -q '"id"'; then
    echo ""
    echo -e "${BOLD}${GREEN}================================${NC}"
    echo -e "${BOLD}${GREEN}  ✅ 10GB UPLOAD SUCCESSFUL${NC}"
    echo -e "${BOLD}${GREEN}================================${NC}"
    
    FILE_ID_10GB=$(echo "$UPLOAD_RESPONSE_10GB" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
    FILENAME_10GB=$(echo "$UPLOAD_RESPONSE_10GB" | grep -o '"filename":"[^"]*"' | cut -d'"' -f4)
    SIZE_10GB=$(echo "$UPLOAD_RESPONSE_10GB" | grep -o '"size":[0-9]*' | cut -d':' -f2)
    DOWNLOAD_LINK_10GB=$(echo "$UPLOAD_RESPONSE_10GB" | grep -o '"downloadLink":"[^"]*"' | cut -d'"' -f4)
    
    echo -e "${CYAN}📁 File Details:${NC}"
    echo -e "  ${BOLD}File ID:${NC} ${BLUE}$FILE_ID_10GB${NC}"
    echo -e "  ${BOLD}Filename:${NC} ${BLUE}$FILENAME_10GB${NC}"
    echo -e "  ${BOLD}Size:${NC} ${BLUE}$SIZE_10GB bytes ($((${SIZE_10GB:-0} / 1024 / 1024)) MB)${NC}"
    echo -e "  ${BOLD}Download Link:${NC} ${BLUE}$DOWNLOAD_LINK_10GB${NC}"
    echo ""
    
    # Test download (just check headers, don't download the full file)
    echo -e "${CYAN}🔍 Testing download functionality...${NC}"
    DOWNLOAD_RESPONSE_10GB=$(curl -s -I http://localhost:8080/api/v1/files/$FILE_ID_10GB/users/550e8400-e29b-41d4-a716-446655440000)
    if echo "$DOWNLOAD_RESPONSE_10GB" | grep -q "200 OK"; then
        echo -e "${GREEN}✅ File download test PASSED${NC}"
    else
        echo -e "${RED}❌ File download test FAILED${NC}"
        echo "Response: $DOWNLOAD_RESPONSE_10GB"
    fi
    echo ""
    
    rm -f "$TEST_FILENAME_10GB"
else
    echo -e "${RED}❌ 10GB file upload test FAILED${NC}"
    echo "Error Details:"
    echo "  Status: $(echo "$UPLOAD_RESPONSE_10GB" | grep -o '"status":[0-9]*' | cut -d':' -f2)"
    echo "  Error: $(echo "$UPLOAD_RESPONSE_10GB" | grep -o '"error":"[^"]*"' | cut -d'"' -f4)"
    echo "  Message: $(echo "$UPLOAD_RESPONSE_10GB" | grep -o '"message":"[^"]*"' | cut -d'"' -f4)"
    echo "  Code: $(echo "$UPLOAD_RESPONSE_10GB" | grep -o '"code":"[^"]*"' | cut -d'"' -f4)"
    echo "Full Response: $UPLOAD_RESPONSE_10GB"
    rm -f "$TEST_FILENAME_10GB"
    exit 1
fi

echo ""
echo -e "${BOLD}${GREEN}================================${NC}"
echo -e "${BOLD}${GREEN}  ✅ ALL TESTS PASSED${NC}"
echo -e "${BOLD}${GREEN}================================${NC}"
echo -e "${GREEN}✅ 2GB file upload: SUCCESS${NC}"
echo -e "${GREEN}✅ 10GB file upload: SUCCESS${NC}"
echo ""

echo "Configuration Summary:"
echo "- Maximum file size: Unlimited (-1)"
echo "- Maximum request size: Unlimited (-1)"
echo "- File size threshold: 10MB (files larger than this are written to disk)"
echo "- Connection timeout: 60 minutes"
echo "- Jetty server optimized for large uploads"
echo "- Support for 2GB and 10GB files: ✅" 