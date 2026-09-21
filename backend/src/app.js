const express = require('express');
const morgan = require('morgan');
const healthRoutes = require('./routes/healthRoutes');
const userRoutes = require('./routes/userRoutes');
const notFoundHandler = require('./middleware/notFoundHandler');
const errorHandler = require('./middleware/errorHandler');
const pantryRoutes = require('./routes/pantryRoutes');
const shoppingRoutes = require('./routes/shoppingRoutes');
const recipeRoutes = require('./routes/recipeRoutes');
const favouriteRoutes = require('./routes/favouriteRoutes');

const app = express();

app.use(morgan('dev', { skip: () => process.env.NODE_ENV === 'test' }));
app.use(express.json());

app.use('/health', healthRoutes);
app.use('/api/users', userRoutes);
app.use('/api/pantry', pantryRoutes);
app.use('/api/shopping-list', shoppingRoutes);
app.use('/api/recipes', recipeRoutes);
app.use('/api/favourites', favouriteRoutes);

app.use(notFoundHandler);
app.use(errorHandler);

module.exports = app;
