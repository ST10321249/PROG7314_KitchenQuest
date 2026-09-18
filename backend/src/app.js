const express = require('express');
const morgan = require('morgan');
const healthRoutes = require('./routes/healthRoutes');
const userRoutes = require('./routes/userRoutes');
const notFoundHandler = require('./middleware/notFoundHandler');
const errorHandler = require('./middleware/errorHandler');

const app = express();

app.use(morgan('dev', { skip: () => process.env.NODE_ENV === 'test' }));
app.use(express.json());

app.use('/health', healthRoutes);
app.use('/api/users', userRoutes);

app.use(notFoundHandler);
app.use(errorHandler);

module.exports = app;
