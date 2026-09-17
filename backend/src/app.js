const express = require('express');
const morgan = require('morgan');
const healthRoutes = require('./routes/healthRoutes');
const notFoundHandler = require('./middleware/notFoundHandler');
const errorHandler = require('./middleware/errorHandler');

const app = express();

app.use(morgan('dev'));
app.use(express.json());

app.use('/health', healthRoutes);

app.use(notFoundHandler);
app.use(errorHandler);

module.exports = app;
