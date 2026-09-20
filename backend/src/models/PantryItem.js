const mongoose = require('mongoose');

const pantryItemSchema = new mongoose.Schema(
  {
    userId: { type: String, required: true, index: true },
    ingredientName: { type: String, required: true, trim: true },
    quantity: { type: Number, required: true, min: 0.01 },
    unit: { type: String, required: true, trim: true },
    category: { type: String, required: true, trim: true },
    expiryDate: { type: Date, default: null },
  },
  { timestamps: true }
);

module.exports = mongoose.model('PantryItem', pantryItemSchema);