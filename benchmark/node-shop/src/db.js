const mysql = require('mysql2/promise');

const pool = mysql.createPool({
  host: process.env.DB_HOST || 'localhost',
  user: process.env.DB_USER || 'shop',
  password: process.env.DB_PASSWORD,
  database: 'shop',
  connectionLimit: 10,
});

module.exports = pool;
