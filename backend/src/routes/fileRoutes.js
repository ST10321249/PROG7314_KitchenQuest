const express = require('express');
const authMiddleware = require('../middleware/authMiddleware');
const fileController = require('../controllers/fileController');
const { allowedTypes, maxFileBytes } = require('../config/files');

const router = express.Router();

// Signing in is checked before the photo is read, so an anonymous request cannot make the
// server accept a large upload.
router.use(authMiddleware);

// The photo is sent as the raw request body with its image content type.
router.post(
  '/',
  express.raw({ type: allowedTypes, limit: maxFileBytes }),
  fileController.uploadFile
);
router.get('/:id', fileController.downloadFile);
router.delete('/:id', fileController.deleteFile);

module.exports = router;
