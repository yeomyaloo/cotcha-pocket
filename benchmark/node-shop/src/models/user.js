const mongoose = require('mongoose');

const userSchema = new mongoose.Schema({
  email: { type: String, required: true, unique: true },
  passwordHash: String,
  resetToken: String,
  role: { type: String, default: 'customer' },
  preferences: Object,
  avatar: String,
});

module.exports = mongoose.model('User', userSchema);
