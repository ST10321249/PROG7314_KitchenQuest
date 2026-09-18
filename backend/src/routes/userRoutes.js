const express = require('express');
const authMiddleware = require('../middleware/authMiddleware');
const validate = require('../middleware/validate');
const { syncUserSchema } = require('../validators/userSchemas');
const userController = require('../controllers/userController');

const router = express.Router();

router.post('/sync', authMiddleware, validate(syncUserSchema), userController.syncUser);

module.exports = router;
