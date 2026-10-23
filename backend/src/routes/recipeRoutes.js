const express = require('express');
const authMiddleware = require('../middleware/authMiddleware');
const recipeController = require('../controllers/recipeController');

const router = express.Router();

router.use(authMiddleware);

// /search and /recommendations must be registered before /:id —
// otherwise Express would treat "recommendations" as an :id value.
router.get('/search', recipeController.searchRecipes);
router.get('/recommendations', recipeController.getRecommendations);
router.get('/:id', recipeController.getRecipeById);

module.exports = router;