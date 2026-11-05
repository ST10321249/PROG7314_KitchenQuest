const { z } = require('zod');

// FCM tokens are long strings with no spaces.
const tokenField = z
  .string()
  .trim()
  .min(20, 'token is too short')
  .max(4096, 'token is too long')
  .regex(/^\S+$/, 'token must not contain spaces');

const registerDeviceSchema = z.object({
  token: tokenField,
  platform: z.enum(['android']).optional(),
});

const testNotificationSchema = z.object({
  // Send to one registered device instead of all of them.
  token: tokenField.optional(),
});

module.exports = { registerDeviceSchema, testNotificationSchema, tokenField };
