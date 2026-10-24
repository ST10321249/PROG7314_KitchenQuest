const express = require('express');
const authMiddleware = require('../middleware/authMiddleware');
const validate = require('../middleware/validate');
const { registerDeviceSchema } = require('../validators/deviceSchemas');
const deviceController = require('../controllers/deviceController');

const router = express.Router();

router.use(authMiddleware);

router.post('/', validate(registerDeviceSchema), deviceController.registerDevice);
router.delete('/:token', deviceController.unregisterDevice);

module.exports = router;
