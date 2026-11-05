const mongoose = require('mongoose');
const { bucketName } = require('../config/files');

// Photos are kept in MongoDB GridFS using the connection the rest of the API already uses.
// The file data lives in its own collections, never inside a user or history document.
// Every file records the Firebase uid of its owner in its metadata.

function bucket() {
  return new mongoose.mongo.GridFSBucket(mongoose.connection.db, { bucketName });
}

function filesCollection() {
  return mongoose.connection.db.collection(`${bucketName}.files`);
}

function toObjectId(id) {
  return new mongoose.Types.ObjectId(id);
}

function describe(file) {
  return {
    id: file._id.toString(),
    contentType: file.metadata.contentType,
    size: file.length,
    createdAt: file.uploadDate.toISOString(),
  };
}

async function save({ ownerUid, buffer, contentType }) {
  const stream = bucket().openUploadStream(`photo-${Date.now()}`, {
    metadata: { ownerUid, contentType },
  });

  await new Promise((resolve, reject) => {
    stream.on('error', reject);
    stream.on('finish', resolve);
    stream.end(buffer);
  });

  const file = await filesCollection().findOne({ _id: stream.id });
  return describe(file);
}

// Returns the file's details, but only when it belongs to the given user.
async function findOwned(id, ownerUid) {
  if (!mongoose.isValidObjectId(id)) {
    return null;
  }

  const file = await filesCollection().findOne({
    _id: toObjectId(id),
    'metadata.ownerUid': ownerUid,
  });
  return file ? describe(file) : null;
}

function openDownloadStream(id) {
  return bucket().openDownloadStream(toObjectId(id));
}

async function remove(id) {
  await bucket().delete(toObjectId(id));
}

async function countForUser(ownerUid) {
  return filesCollection().countDocuments({ 'metadata.ownerUid': ownerUid });
}

module.exports = { save, findOwned, openDownloadStream, remove, countForUser };
