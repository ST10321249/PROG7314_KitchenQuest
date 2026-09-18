const { z } = require('zod');

const syncUserSchema = z.object({
  displayName: z.string().trim().optional(),
  dietaryPreferences: z.array(z.string().trim().min(1)).optional(),
  avoidedIngredients: z.array(z.string().trim().min(1)).optional(),
});

module.exports = { syncUserSchema };
