const mongoose = require('mongoose');

const shoppingListItemSchema = new mongoose.Schema(
  {
    userId: { type: String, required: true, index: true },
    ingredientName: { type: String, required: true, trim: true },
    quantity: { type: Number, default: null },
    unit: { type: String, default: '' },
    isPurchased: { type: Boolean, default: false },
  },
  { timestamps: true }
);

module.exports = mongoose.model('ShoppingListItem', shoppingListItemSchema);