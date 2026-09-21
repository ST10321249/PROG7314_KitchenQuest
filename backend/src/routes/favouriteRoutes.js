const express = require('express');
const authMiddleware = require('../middleware/authMiddleware');
const favouriteController = require('../controllers/favouriteController');

const router = express.Router();

router.use(authMiddleware);

router.get('/', favouriteController.getFavourites);
router.post('/', favouriteController.addFavourite);
router.delete('/:recipeSourceId', favouriteController.removeFavourite);

module.exports = router;