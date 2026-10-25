const express = require('express');
const authMiddleware = require('../middleware/authMiddleware');
const validate = require('../middleware/validate');
const { updateHistorySchema } = require('../validators/historySchemas');
const historyController = require('../controllers/historyController');

const router = express.Router();

router.use(authMiddleware);

router.get('/', historyController.getHistory);
router.post('/', historyController.addHistoryEntry);
router.patch('/:id', validate(updateHistorySchema), historyController.updateHistoryEntry);

module.exports = router;
