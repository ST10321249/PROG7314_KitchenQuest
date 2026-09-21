const express = require('express');
const authMiddleware = require('../middleware/authMiddleware');
const recipeNoteController = require('../controllers/recipeNoteController');

const router = express.Router();

router.use(authMiddleware);

router.get('/:recipeId', recipeNoteController.getNote);
router.post('/', recipeNoteController.saveNote);

module.exports = router;