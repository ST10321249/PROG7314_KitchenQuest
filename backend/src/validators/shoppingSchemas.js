const { z } = require('zod');

const createShoppingItemSchema = z.object({
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

module.exports = { createShoppingItemSchema, updateShoppingItemSchema };