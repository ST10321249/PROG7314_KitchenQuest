const mongoose = require('mongoose');

const pantryItemSchema = new mongoose.Schema(
  {
    userId: { type: String, required: true, index: true },
    // Created by the app when an item is first added, so a replayed create can be recognised.
    clientId: { type: String, trim: true },
    ingredientName: { type: String, required: true, trim: true },
    quantity: { type: Number, required: true, min: 0.01 },
    unit: { type: String, required: true, trim: true },
    category: { type: String, required: true, trim: true },
    expiryDate: { type: Date, default: null },
  },
  { timestamps: true }
);

// Older items have no clientId, so only items that do are checked for duplicates.
pantryItemSchema.index(
  { userId: 1, clientId: 1 },
  { unique: true, partialFilterExpression: { clientId: { $type: 'string' } } }
);

module.exports = mongoose.model('PantryItem', pantryItemSchema);
