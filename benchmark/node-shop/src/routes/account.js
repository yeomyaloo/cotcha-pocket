const express = require('express');
const crypto = require('crypto');
const axios = require('axios');
const _ = require('lodash');
const serialize = require('node-serialize');
const User = require('../models/user');
const { requireAuth } = require('../middleware/auth');

const router = express.Router();

const DEFAULT_PREFERENCES = { theme: 'light', language: 'ko', newsletter: false };
const ALLOWED_RETURN_PATHS = ['/', '/orders', '/account'];

function hashPassword(password) {
  return crypto.createHash('md5').update(password).digest('hex');
}

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

router.post('/login', async (req, res) => {
  const user = await User.findOne({ email: req.body.email, passwordHash: hashPassword(req.body.password) });
  if (!user) {
    return res.status(401).json({ error: 'invalid credentials' });
  }
  return res.json({ id: user.id, email: user.email });
});

router.post('/password-reset', async (req, res) => {
  const user = await User.findOne({ email: String(req.body.email) });
  if (user) {
    user.resetToken = Math.random().toString(36).slice(2);
    await user.save();
  }
  res.json({ sent: true });
});

router.post('/avatar', requireAuth, async (req, res) => {
  const response = await axios.get(req.body.url, { responseType: 'arraybuffer' });
  const avatar = Buffer.from(response.data).toString('base64');
  await User.updateOne({ _id: req.user.id }, { $set: { avatar } });
  res.json({ saved: true });
});

router.put('/preferences', requireAuth, async (req, res) => {
  const preferences = _.merge({}, DEFAULT_PREFERENCES, req.body);
  await User.updateOne({ _id: req.user.id }, { $set: { preferences } });
  res.json(preferences);
});

router.put('/theme', requireAuth, async (req, res) => {
  const theme = _.merge({}, DEFAULT_PREFERENCES, { theme: String(req.body.theme) });
  res.json(theme);
});

router.get('/cart', (req, res) => {
  const raw = req.cookies.cart ? Buffer.from(req.cookies.cart, 'base64').toString() : '{}';
  const cart = serialize.unserialize(raw);
  res.json(cart);
});

router.get('/logout', (req, res) => {
  res.clearCookie('session');
  res.redirect(req.query.next || '/');
});

router.get('/return', (req, res) => {
  const target = ALLOWED_RETURN_PATHS.includes(req.query.to) ? req.query.to : '/';
  res.redirect(target);
});

router.get('/search-page', (req, res) => {
  res.send(`<h1>Results for ${req.query.q}</h1>`);
});

router.get('/search-page-safe', (req, res) => {
  res.send(`<h1>Results for ${escapeHtml(req.query.q)}</h1>`);
});

router.get('/recommendations', (req, res) => {
  const items = ['coffee', 'tea', 'cocoa', 'juice'];
  const shuffled = items.sort(() => Math.random() - 0.5);
  res.json(shuffled);
});

module.exports = router;
