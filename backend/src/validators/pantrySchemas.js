const { z } = require('zod');

const startOfToday = () => {
  const now = new Date();
  now.setHours(0, 0, 0, 0);
  return now;
};

const expiryDateField = z
  .string()
  .trim()
  .refine((value) => !Number.isNaN(Date.parse(value)), {
    message: 'Invalid date',
  })
  .refine((value) => new Date(value) >= startOfToday(), {
    message: 'Expiry date cannot be in the past',
  })
  .nullable()
  .optional();

const createPantryItemSchema = z.object({
  ingredientName: z.string().trim().min(1, 'Ingredient name is required'),
  quantity: z.number().positive('Quantity must be greater than zero'),
  unit: z.string().trim().min(1, 'Unit is required'),
  category: z.string().trim().min(1, 'Category is required'),
  expiryDate: expiryDateField,
});

const updatePantryItemSchema = z
  .object({
    ingredientName: z.string().trim().min(1).optional(),
    quantity: z.number().positive('Quantity must be greater than zero').optional(),
    unit: z.string().trim().min(1).optional(),
    category: z.string().trim().min(1).optional(),
    expiryDate: expiryDateField,
  })
  .refine((data) => Object.keys(data).length > 0, {
    message: 'Provide at least one field to update',
  });

module.exports = { createPantryItemSchema, updatePantryItemSchema };