const express = require('express');
const authMiddleware = require('../middleware/authMiddleware');
const validate = require('../middleware/validate');
const { testNotificationSchema } = require('../validators/deviceSchemas');
const notificationController = require('../controllers/notificationController');

const router = express.Router();

router.use(authMiddleware);

router.post('/test', validate(testNotificationSchema), notificationController.sendTestNotification);

module.exports = router;
