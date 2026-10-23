const { z } = require('zod');
const { clientIdField, objectIdField } = require('./commonSchemas');
const { createPantryItemSchema } = require('./pantrySchemas');

const createShoppingItemSchema = z.object({
  clientId: clientIdField.optional(),
  ingredientName: z.string().trim().min(1, 'Item name is required'),
  quantity: z.number().positive().nullable().optional(),
  unit: z.string().trim().optional(),
});

const updateShoppingItemSchema = z
  .object({
    ingredientName: z.string().trim().min(1).optional(),
    quantity: z.number().positive().nullable().optional(),
    unit: z.string().trim().optional(),
    isPurchased: z.boolean().optional(),
  })
  .refine((data) => Object.keys(data).length > 0, {
    message: 'Provide at least one field to update',
  });

// Buying an item moves it to the pantry. The pantry item must carry a clientId so the
// request can be repeated without adding the item twice.
const transferToPantrySchema = z
  .object({
    shoppingItemId: objectIdField.optional(),
    shoppingClientId: clientIdField.optional(),
    pantryItem: createPantryItemSchema.extend({ clientId: clientIdField }),
  })
  .refine((data) => data.shoppingItemId || data.shoppingClientId, {
    message: 'Provide shoppingItemId or shoppingClientId',
  });

module.exports = { createShoppingItemSchema, updateShoppingItemSchema, transferToPantrySchema };