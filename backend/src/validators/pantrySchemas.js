const { z } = require('zod');
const { clientIdField } = require('./commonSchemas');

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
  .nullable()
  .optional();

// The app checks the expiry date when the user types it. A change made while the phone was
// offline can reach the server days later, after that date has passed, so it must not be
// refused for that. Only a create sent without a clientId, which is how the API is called
// directly rather than by the app, is checked here.
const rejectPastExpiryWithoutClientId = {
  check: (data) =>
    Boolean(data.clientId) || !data.expiryDate || new Date(data.expiryDate) >= startOfToday(),
  options: { message: 'Expiry date cannot be in the past', path: ['expiryDate'] },
};

// Kept separate from the checked version because a schema with a check cannot be extended.
const createPantryItemBaseSchema = z.object({
  clientId: clientIdField.optional(),
  ingredientName: z.string().trim().min(1, 'Ingredient name is required'),
  quantity: z.number().positive('Quantity must be greater than zero'),
  unit: z.string().trim().min(1, 'Unit is required'),
  category: z.string().trim().min(1, 'Category is required'),
  expiryDate: expiryDateField,
});

const createPantryItemSchema = createPantryItemBaseSchema.refine(
  rejectPastExpiryWithoutClientId.check,
  rejectPastExpiryWithoutClientId.options
);

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

module.exports = { createPantryItemSchema, createPantryItemBaseSchema, updatePantryItemSchema };
