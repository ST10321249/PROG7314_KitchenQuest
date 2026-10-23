const mongoose = require('mongoose');
const PantryItem = require('../models/PantryItem');
const ApiError = require('../utils/ApiError');
const createOnce = require('../utils/createOnce');

function toDto(item) {
  return {
    id: item._id.toString(),
    clientId: item.clientId ?? null,
    ingredientName: item.ingredientName,
    quantity: item.quantity,
    unit: item.unit,
    category: item.category,
    expiryDate: item.expiryDate ? item.expiryDate.toISOString().slice(0, 10) : null,
    updatedAt: item.updatedAt ? item.updatedAt.toISOString() : null,
  };
}

// An id that is not a valid Mongo id can never match an item, so treat it as not found.
function requireValidId(id) {
  if (!mongoose.isValidObjectId(id)) {
    throw new ApiError(404, 'Pantry item not found');
  }
}

async function getPantryItems(req, res, next) {
  try {
    const items = await PantryItem.find({ userId: req.user.uid }).sort({ expiryDate: 1 });
    res.status(200).json(items.map(toDto));
  } catch (err) {
    next(err);
  }
}

async function addPantryItem(req, res, next) {
  try {
    const { doc, created } = await createOnce(PantryItem, req.user.uid, req.body);
    res.status(created ? 201 : 200).json(toDto(doc));
  } catch (err) {
    next(err);
  }
}

async function updatePantryItem(req, res, next) {
  try {
    requireValidId(req.params.id);

    const item = await PantryItem.findOneAndUpdate(
      { _id: req.params.id, userId: req.user.uid },
      { $set: req.body },
      { new: true, runValidators: true }
    );

    if (!item) {
      throw new ApiError(404, 'Pantry item not found');
    }

    res.status(200).json(toDto(item));
  } catch (err) {
    next(err);
  }
}

async function deletePantryItem(req, res, next) {
  try {
    requireValidId(req.params.id);

    const item = await PantryItem.findOneAndDelete({
      _id: req.params.id,
      userId: req.user.uid,
    });

    if (!item) {
      throw new ApiError(404, 'Pantry item not found');
    }

    res.status(204).send();
  } catch (err) {
    next(err);
  }
}

module.exports = { getPantryItems, addPantryItem, updatePantryItem, deletePantryItem, toDto };
