const express = require('express');
const cookieParser = require('cookie-parser');
const productRoutes = require('./routes/products');
const accountRoutes = require('./routes/account');
const orderRoutes = require('./routes/orders');
const adminRoutes = require('./routes/admin');
const { requireAuth, requireAdmin } = require('./middleware/auth');

const app = express();

app.use(express.json());
app.use(cookieParser());

app.use('/api/products', productRoutes);
app.use('/api/account', accountRoutes);
app.use('/api/orders', requireAuth, orderRoutes);
app.use('/api/admin', requireAuth, requireAdmin, adminRoutes);

app.get('/health', (req, res) => {
  res.json({ status: 'ok' });
});

module.exports = app;
