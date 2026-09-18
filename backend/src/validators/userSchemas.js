const { z } = require('zod');

const syncUserSchema = z.object({
  displayName: z.string().trim().optional(),
  dietaryPreferences: z.array(z.string().trim().min(1)).optional(),
  avoidedIngredients: z.array(z.string().trim().min(1)).optional(),
});

const updateProfileSchema = z
  .object({
    displayName: z.string().trim().min(1).optional(),
    dietaryPreferences: z.array(z.string().trim().min(1)).optional(),
    avoidedIngredients: z.array(z.string().trim().min(1)).optional(),
  })
  .refine((data) => Object.keys(data).length > 0, {
    message: 'Provide at least one field to update',
  });

module.exports = { syncUserSchema, updateProfileSchema };
