// Creates a document, but if the same user already created one with this clientId
// the existing document is returned instead. This lets the app safely resend a
// create request that timed out without ending up with a duplicate.
async function createOnce(Model, userId, data) {
  const { clientId } = data;

  if (!clientId) {
    return { doc: await Model.create({ ...data, userId }), created: true };
  }

  const existing = await Model.findOne({ userId, clientId });
  if (existing) {
    return { doc: existing, created: false };
  }

  try {
    return { doc: await Model.create({ ...data, userId }), created: true };
  } catch (err) {
    // Two identical requests can arrive together; the unique index rejects the second.
    if (err && err.code === 11000) {
      const winner = await Model.findOne({ userId, clientId });
      if (winner) {
        return { doc: winner, created: false };
      }
    }
    throw err;
  }
}

module.exports = createOnce;
