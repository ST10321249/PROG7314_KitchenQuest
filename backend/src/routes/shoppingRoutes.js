const express = require('express');
const authMiddleware = require('../middleware/authMiddleware');
const validate = require('../middleware/validate');
const { createShoppingItemSchema, updateShoppingItemSchema } = require('../validators/shoppingSchemas');
const shoppingController = require('../controllers/shoppingController');

const router = express.Router();

router.use(authMiddleware);

router.get('/', shoppingController.getShoppingItems);
router.post('/', validate(createShoppingItemSchema), shoppingController.addShoppingItem);
router.patch('/:id', validate(updateShoppingItemSchema), shoppingController.updateShoppingItem);
router.delete('/:id', shoppingController.deleteShoppingItem);

module.exports = router;