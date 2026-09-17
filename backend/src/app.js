const express = require('express');
const morgan = require('morgan');
const healthRoutes = require('./routes/healthRoutes');

const app = express();

app.use(morgan('dev'));
app.use(express.json());

app.use('/health', healthRoutes);

module.exports = app;
