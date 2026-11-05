// Limits for uploaded photos. They keep the free MongoDB database from filling up:
// 50 photos of at most 3 MB each is at most about 150 MB for one user.
module.exports = {
  allowedTypes: ['image/jpeg', 'image/png', 'image/webp'],
  maxFileBytes: 3 * 1024 * 1024,
  maxFilesPerUser: 50,
  bucketName: 'photos',
};
