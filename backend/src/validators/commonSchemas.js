const { z } = require('zod');

// The app generates a UUID, but any URL-safe id of a sensible length is accepted.
const clientIdField = z
  .string()
  .trim()
  .regex(/^[A-Za-z0-9_-]{8,64}$/, 'clientId must be 8-64 letters, numbers, - or _');

const objectIdField = z.string().regex(/^[a-fA-F0-9]{24}$/, 'Invalid id');

module.exports = { clientIdField, objectIdField };
