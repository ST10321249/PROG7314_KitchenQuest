const express = require('express');
const authMiddleware = require('../middleware/authMiddleware');
const validate = require('../middleware/validate');
const { createPantryItemSchema, updatePantryItemSchema } = require('../validators/pantrySchemas');
const pantryController = require('../controllers/pantryController');

const router = express.Router();

router.use(authMiddleware);

router.get('/', pantryController.getPantryItems);
router.post('/', validate(createPantryItemSchema), pantryController.addPantryItem);
router.patch('/:id', validate(updatePantryItemSchema), pantryController.updatePantryItem);
router.delete('/:id', pantryController.deletePantryItem);

module.exports = router;