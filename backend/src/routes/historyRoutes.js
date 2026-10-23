const express = require('express');
const authMiddleware = require('../middleware/authMiddleware');
const historyController = require('../controllers/historyController');

const router = express.Router();

router.use(authMiddleware);

router.get('/', historyController.getHistory);
router.post('/', historyController.addHistoryEntry);

module.exports = router;