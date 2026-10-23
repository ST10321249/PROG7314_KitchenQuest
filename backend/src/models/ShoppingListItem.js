const mongoose = require('mongoose');

const shoppingListItemSchema = new mongoose.Schema(
  {
    userId: { type: String, required: true, index: true },
    // Created by the app when an item is first added, so a replayed create can be recognised.
    clientId: { type: String, trim: true },
    ingredientName: { type: String, required: true, trim: true },
    quantity: { type: Number, default: null },
    unit: { type: String, default: '' },
    isPurchased: { type: Boolean, default: false },
  },
  { timestamps: true }
);

// Older items have no clientId, so only items that do are checked for duplicates.
shoppingListItemSchema.index(
  { userId: 1, clientId: 1 },
  { unique: true, partialFilterExpression: { clientId: { $type: 'string' } } }
);

module.exports = mongoose.model('ShoppingListItem', shoppingListItemSchema);
