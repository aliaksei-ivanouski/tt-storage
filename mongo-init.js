// MongoDB initialization script
db = db.getSiblingDB('tt_storage_db');

// Create user for the application
db.createUser({
  user: 'user',
  pwd: 'letmein',
  roles: [
    {
      role: 'readWrite',
      db: 'tt_storage_db'
    }
  ]
});

// Create collections
db.createCollection('files');
db.createCollection('tags');

print('MongoDB initialization completed successfully'); 