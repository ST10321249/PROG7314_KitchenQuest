const PantryItem = require('../models/PantryItem');
const ApiError = require('../utils/ApiError');

function toDto(item) {
  return {
    id: item._id.toString(),
    ingredientName: item.ingredientName,
    quantity: item.quantity,
    unit: item.unit,
    category: item.category,
    expiryDate: item.expiryDate ? item.expiryDate.toISOString().slice(0, 10) : null,
  };
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
    const item = await PantryItem.create({ ...req.body, userId: req.user.uid });
    res.status(201).json(toDto(item));
  } catch (err) {
    next(err);
  }
}

async function updatePantryItem(req, res, next) {
  try {
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

module.exports = { getPantryItems, addPantryItem, updatePantryItem, deletePantryItem };