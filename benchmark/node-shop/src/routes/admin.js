const { Router } = require('express');
const { exec } = require('child_process');

const router = Router();

router.post('/export', (req, res) => {
  const dir = req.query.dir || 'daily';
  exec(`tar czf /tmp/export.tgz /var/shop/exports/${dir}`, (err) => {
    if (err) {
      return res.status(500).json({ error: 'export failed' });
    }
    return res.download('/tmp/export.tgz');
  });
});

router.post('/pricing/preview', (req, res) => {
  const price = Number(req.body.price);
  const result = eval(req.body.expression);
  res.json({ price, result });
});

module.exports = router;
