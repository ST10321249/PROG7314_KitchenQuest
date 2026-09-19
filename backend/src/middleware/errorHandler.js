function errorHandler(err, req, res, next) {
  if (res.headersSent) {
    return next(err);
  }

  const statusCode = err.statusCode || 500;

  if (statusCode >= 500) {
    console.error(err);
    return res.status(statusCode).json({ error: 'Internal server error' });
  }

  res.status(statusCode).json({ error: err.message });
}

module.exports = errorHandler;
