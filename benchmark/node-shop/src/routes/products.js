const express = require('express');
const path = require('path');
const crypto = require('crypto');
const db = require('../db');

const router = express.Router();

const IMAGE_DIR = path.join(__dirname, '..', '..', 'uploads', 'images');
const THUMB_DIR = path.join(__dirname, '..', '..', 'uploads', 'thumbs');
const SORT_COLUMNS = ['name', 'price', 'created_at'];

router.get('/', async (req, res) => {
  const column = SORT_COLUMNS.includes(req.query.sort) ? req.query.sort : 'name';
  const [rows] = await db.query(`SELECT id, name, price FROM products ORDER BY ${column}`);
  const etag = crypto.createHash('sha1').update(JSON.stringify(rows)).digest('hex');
  res.set('ETag', etag);
  res.json(rows);
});

router.get('/search', async (req, res) => {
  const keyword = req.query.q || '';
  const [rows] = await db.query(`SELECT id, name, price FROM products WHERE name LIKE '%${keyword}%'`);
  res.json(rows);
});

router.get('/:id', async (req, res) => {
  const [rows] = await db.query('SELECT id, name, price, description FROM products WHERE id = ?', [req.params.id]);
  if (rows.length === 0) {
    return res.status(404).json({ error: 'not found' });
  }
  return res.json(rows[0]);
});

router.get('/images/:name', (req, res) => {
  res.sendFile(path.join(IMAGE_DIR, req.params.name));
});

router.get('/thumbs/:name', (req, res) => {
  res.sendFile(path.basename(req.params.name), { root: THUMB_DIR });
});

module.exports = router;
