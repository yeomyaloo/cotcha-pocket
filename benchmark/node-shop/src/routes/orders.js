const express = require('express');
const axios = require('axios');
const db = require('../db');

const router = express.Router();

router.get('/', async (req, res) => {
  const [rows] = await db.query('SELECT id, total, status FROM orders WHERE user_id = ?', [req.user.id]);
  res.json(rows);
});

router.get('/:id/payment-status', async (req, res) => {
  const [rows] = await db.query('SELECT payment_id FROM orders WHERE id = ? AND user_id = ?', [req.params.id, req.user.id]);
  if (rows.length === 0) {
    return res.status(404).json({ error: 'not found' });
  }
  const response = await axios.get(`https://payments.example.com/v1/status/${encodeURIComponent(rows[0].payment_id)}`);
  return res.json(response.data);
});

module.exports = router;
