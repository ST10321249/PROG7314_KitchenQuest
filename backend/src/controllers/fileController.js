const CookingHistory = require('../models/CookingHistory');
const fileStorage = require('../services/fileStorage');
const detectImageType = require('../utils/imageType');
const ApiError = require('../utils/ApiError');
const { allowedTypes, maxFilesPerUser } = require('../config/files');

function notFound() {
  // The same answer for a file that does not exist and one that belongs to someone else,
  // so nobody can find out which file ids exist.
  return new ApiError(404, 'Photo not found');
}

async function uploadFile(req, res, next) {
  try {
    const body = req.body;
    const claimedType = (req.headers['content-type'] || '').split(';')[0].trim().toLowerCase();

    if (!Buffer.isBuffer(body)) {
      // An empty upload is not read at all, so it arrives here without a body.
      if (allowedTypes.includes(claimedType)) {
        throw new ApiError(400, 'The photo is empty');
      }
      throw new ApiError(415, `Send the photo as ${allowedTypes.join(', ')}`);
    }

    if (body.length === 0) {
      throw new ApiError(400, 'The photo is empty');
    }

    // Trust the file itself rather than the content type the app claimed.
    const detected = detectImageType(body);
    if (!detected) {
      throw new ApiError(400, 'The file is not a valid JPEG, PNG or WebP image');
    }
    if (detected !== claimedType) {
      throw new ApiError(400, 'The file does not match its content type');
    }

    const ownerUid = req.user.uid;
    if ((await fileStorage.countForUser(ownerUid)) >= maxFilesPerUser) {
      throw new ApiError(409, `You can store up to ${maxFilesPerUser} photos. Delete one to add another`);
    }

    const file = await fileStorage.save({ ownerUid, buffer: body, contentType: detected });
    res.status(201).json(file);
  } catch (err) {
    next(err);
  }
}

async function downloadFile(req, res, next) {
  try {
    const file = await fileStorage.findOwned(req.params.id, req.user.uid);
    if (!file) {
      throw notFound();
    }

    // A stored photo never changes, so its id is a safe version tag.
    const etag = `"${file.id}"`;
    res.set({
      'Content-Type': file.contentType,
      'Content-Length': String(file.size),
      'Cache-Control': 'private, max-age=86400',
      'X-Content-Type-Options': 'nosniff',
      ETag: etag,
    });

    if (req.headers['if-none-match'] === etag) {
      return res.status(304).end();
    }

    const stream = fileStorage.openDownloadStream(file.id);
    stream.on('error', (err) => {
      if (res.headersSent) {
        res.destroy(err);
      } else {
        next(err);
      }
    });
    stream.pipe(res);
  } catch (err) {
    next(err);
  }
}

async function deleteFile(req, res, next) {
  try {
    const file = await fileStorage.findOwned(req.params.id, req.user.uid);
    if (!file) {
      throw notFound();
    }

    await fileStorage.remove(file.id);
    // Any cooking history entry that showed this photo no longer has one.
    await CookingHistory.updateMany(
      { userId: req.user.uid, photoFileId: file.id },
      { $set: { photoFileId: null } }
    );

    res.status(204).send();
  } catch (err) {
    next(err);
  }
}

module.exports = { uploadFile, downloadFile, deleteFile };
