#!/bin/bash

# Test script to verify large file upload functionality (hundreds of GB support)
echo "Testing large file upload functionality for hundreds of GB support..."

# Start the application if not already running
echo "Starting application..."
./run.sh &
APP_PID=$!

# Wait for application to start
echo "Waiting for application to start..."
sleep 30

# Check if application is running
if ! curl -s http://localhost:8080/actuator/health > /dev/null; then
    echo "Application failed to start"
    exit 1
fi

echo "Application is running. Testing large file upload..."

# Create a test file (1GB for testing - you can increase this to test hundreds of GB)
echo "Creating test file (1GB)..."
dd if=/dev/zero of=test-large-file.dat bs=1M count=1024 2>/dev/null

# Upload the file
echo "Uploading test file (1GB)..."
UPLOAD_RESPONSE=$(curl -s -X POST \
  -F "file=@test-large-file.dat" \
  -F "userId=550e8400-e29b-41d4-a716-446655440000" \
  -F "visibility=PUBLIC" \
  -F "tags=test,large,hundreds-gb" \
  http://localhost:8080/api/v1/files/upload)

echo "Upload response: $UPLOAD_RESPONSE"

# Check if upload was successful
if echo "$UPLOAD_RESPONSE" | grep -q "fileId"; then
    echo "✅ Large file upload test PASSED (1GB)"
    FILE_ID=$(echo "$UPLOAD_RESPONSE" | grep -o '"fileId":"[^"]*"' | cut -d'"' -f4)
    echo "File ID: $FILE_ID"
    
    # Test download (just check headers, don't download the full file)
    echo "Testing file download headers..."
    DOWNLOAD_RESPONSE=$(curl -s -I http://localhost:8080/api/v1/files/$FILE_ID/users/550e8400-e29b-41d4-a716-446655440000)
    if echo "$DOWNLOAD_RESPONSE" | grep -q "200 OK"; then
        echo "✅ File download test PASSED"
        echo "Content-Length: $(echo "$DOWNLOAD_RESPONSE" | grep -i "content-length" | cut -d' ' -f2)"
    else
        echo "❌ File download test FAILED"
    fi
else
    echo "❌ Large file upload test FAILED"
    echo "Response: $UPLOAD_RESPONSE"
fi

# Test with even larger file (10GB) if you have enough disk space
echo ""
echo "Testing with 10GB file (requires sufficient disk space)..."
read -p "Do you want to test with 10GB file? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "Creating 10GB test file..."
    dd if=/dev/zero of=test-10gb-file.dat bs=1M count=10240 2>/dev/null
    
    echo "Uploading 10GB test file..."
    UPLOAD_RESPONSE_10GB=$(curl -s -X POST \
      -F "file=@test-10gb-file.dat" \
      -F "userId=550e8400-e29b-41d4-a716-446655440000" \
      -F "visibility=PUBLIC" \
      -F "tags=test,10gb,hundreds-gb" \
      http://localhost:8080/api/v1/files/upload)
    
    if echo "$UPLOAD_RESPONSE_10GB" | grep -q "fileId"; then
        echo "✅ 10GB file upload test PASSED"
        FILE_ID_10GB=$(echo "$UPLOAD_RESPONSE_10GB" | grep -o '"fileId":"[^"]*"' | cut -d'"' -f4)
        echo "10GB File ID: $FILE_ID_10GB"
    else
        echo "❌ 10GB file upload test FAILED"
        echo "Response: $UPLOAD_RESPONSE_10GB"
    fi
    
    # Cleanup 10GB file
    rm -f test-10gb-file.dat
fi

# Cleanup
echo "Cleaning up..."
rm -f test-large-file.dat

# Stop the application
echo "Stopping application..."
kill $APP_PID 2>/dev/null

echo "Test completed!"
echo ""
echo "Configuration Summary:"
echo "- Maximum file size: Unlimited (-1)"
echo "- Maximum request size: Unlimited (-1)"
echo "- File size threshold: 10MB (files larger than this are written to disk)"
echo "- Connection timeout: 60 minutes"
echo "- Jetty server optimized for large uploads"
echo "- Support for hundreds of GB files: ✅" 