const ShoppingListItem = require('../models/ShoppingListItem');
const ApiError = require('../utils/ApiError');

function toDto(item) {
  return {
    id: item._id.toString(),
    ingredientName: item.ingredientName,
    quantity: item.quantity,
    unit: item.unit,
    isPurchased: item.isPurchased,
  };
}

async function getShoppingItems(req, res, next) {
  try {
    const items = await ShoppingListItem.find({ userId: req.user.uid }).sort({ createdAt: 1 });
    res.status(200).json(items.map(toDto));
  } catch (err) {
    next(err);
  }
}

async function addShoppingItem(req, res, next) {
  try {
    const item = await ShoppingListItem.create({ ...req.body, userId: req.user.uid });
    res.status(201).json(toDto(item));
  } catch (err) {
    next(err);
  }
}

async function updateShoppingItem(req, res, next) {
  try {
    const item = await ShoppingListItem.findOneAndUpdate(
      { _id: req.params.id, userId: req.user.uid },
      { $set: req.body },
      { new: true, runValidators: true }
    );

    if (!item) throw new ApiError(404, 'Shopping item not found');
    res.status(200).json(toDto(item));
  } catch (err) {
    next(err);
  }
}

async function deleteShoppingItem(req, res, next) {
  try {
    const item = await ShoppingListItem.findOneAndDelete({
      _id: req.params.id,
      userId: req.user.uid,
    });

    if (!item) throw new ApiError(404, 'Shopping item not found');
    res.status(204).send();
  } catch (err) {
    next(err);
  }
}

module.exports = { getShoppingItems, addShoppingItem, updateShoppingItem, deleteShoppingItem };