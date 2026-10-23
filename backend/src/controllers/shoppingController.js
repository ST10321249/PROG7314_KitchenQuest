const mongoose = require('mongoose');
const ShoppingListItem = require('../models/ShoppingListItem');
const PantryItem = require('../models/PantryItem');
const ApiError = require('../utils/ApiError');
const createOnce = require('../utils/createOnce');
const { toDto: pantryToDto } = require('./pantryController');

function toDto(item) {
  return {
    id: item._id.toString(),
    clientId: item.clientId ?? null,
    ingredientName: item.ingredientName,
    quantity: item.quantity,
    unit: item.unit,
    isPurchased: item.isPurchased,
    updatedAt: item.updatedAt ? item.updatedAt.toISOString() : null,
  };
}

// An id that is not a valid Mongo id can never match an item, so treat it as not found.
function requireValidId(id) {
  if (!mongoose.isValidObjectId(id)) {
    throw new ApiError(404, 'Shopping item not found');
  }
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
    const { doc, created } = await createOnce(ShoppingListItem, req.user.uid, req.body);
    res.status(created ? 201 : 200).json(toDto(doc));
  } catch (err) {
    next(err);
  }
}

async function updateShoppingItem(req, res, next) {
  try {
    requireValidId(req.params.id);

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
    requireValidId(req.params.id);

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

// Moves a bought item into the pantry. The pantry item is created first and the shopping
// item removed second, and both steps are safe to repeat, so a request that is resent
// after a timeout finishes the job instead of duplicating or losing the item.
async function transferToPantry(req, res, next) {
  try {
    const { shoppingItemId, shoppingClientId, pantryItem } = req.body;
    const userId = req.user.uid;

    const { doc, created } = await createOnce(PantryItem, userId, pantryItem);

    const shoppingFilter = shoppingItemId
      ? { _id: shoppingItemId, userId }
      : { clientId: shoppingClientId, userId };
    await ShoppingListItem.deleteOne(shoppingFilter);

    res.status(created ? 201 : 200).json(pantryToDto(doc));
  } catch (err) {
    next(err);
  }
}

module.exports = {
  getShoppingItems,
  addShoppingItem,
  updateShoppingItem,
  deleteShoppingItem,
  transferToPantry,
};
