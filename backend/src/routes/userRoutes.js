const express = require('express');
const authMiddleware = require('../middleware/authMiddleware');
const validate = require('../middleware/validate');
const { syncUserSchema, updateProfileSchema } = require('../validators/userSchemas');
const userController = require('../controllers/userController');

const router = express.Router();

router.post('/sync', authMiddleware, validate(syncUserSchema), userController.syncUser);
router.get('/me', authMiddleware, userController.getMe);
router.put('/me', authMiddleware, validate(updateProfileSchema), userController.updateMe);

module.exports = router;
