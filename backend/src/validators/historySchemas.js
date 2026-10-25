const { z } = require('zod');
const { objectIdField } = require('./commonSchemas');

// Use null to remove the photo from a history entry.
const updateHistorySchema = z
  .object({
    photoFileId: objectIdField.nullable(),
  })
  .refine((data) => data.photoFileId !== undefined, {
    message: 'Provide photoFileId',
  });

module.exports = { updateHistorySchema };
