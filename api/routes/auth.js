const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { OAuth2Client } = require('google-auth-library');
const { User } = require('../models');

const router = express.Router();
const googleClient = new OAuth2Client(process.env.GOOGLE_CLIENT_ID);
const JWT_SECRET = process.env.JWT_SECRET || 'dev-secret-change-me';
const EMAIL_RE = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;

function issueToken(user) {
  return jwt.sign({ userId: user.userId, email: user.email }, JWT_SECRET, { expiresIn: '7d' });
}

function publicUser(user) {
  const { passwordHash, ...safe } = user.toJSON();
  return safe;
}

const normaliseEmail = (value) => String(value || '').trim().toLowerCase();

// POST /api/auth/register
router.post('/register', async (req, res) => {
  try {
    const name = String(req.body.name || '').trim();
    const email = normaliseEmail(req.body.email);
    const password = String(req.body.password || '');

    if (!name || name.length > 100) return res.status(400).json({ error: 'A name (max 100 characters) is required' });
    if (!EMAIL_RE.test(email) || email.length > 150) return res.status(400).json({ error: 'A valid email is required' });
    if (password.length < 8) return res.status(400).json({ error: 'Password must be at least 8 characters' });

    const existing = await User.findOne({ where: { email } });
    if (existing) return res.status(409).json({ error: 'Email already registered' });

    const passwordHash = await bcrypt.hash(password, 10); // never store the plain text password
    const user = await User.create({ name, email, passwordHash });

    res.status(201).json({ token: issueToken(user), user: publicUser(user) });
  } catch (err) {
    console.error('Registration failed:', err);
    res.status(500).json({ error: 'Registration failed' });
  }
});

// POST /api/auth/login
router.post('/login', async (req, res) => {
  try {
    const email = normaliseEmail(req.body.email);
    const password = String(req.body.password || '');
    const user = await User.findOne({ where: { email } });
    if (!user || !user.passwordHash) {
      return res.status(401).json({ error: 'Invalid email or password' });
    }
    const valid = await bcrypt.compare(password, user.passwordHash);
    if (!valid) return res.status(401).json({ error: 'Invalid email or password' });

    res.json({ token: issueToken(user), user: publicUser(user) });
  } catch (err) {
    console.error('Login failed:', err);
    res.status(500).json({ error: 'Login failed' });
  }
});

// POST /api/auth/google  { idToken } -> verify the Google ID token server-side, then sign the
// student in (creating the account on first use) and return our own JWT.
router.post('/google', async (req, res) => {
  try {
    const { idToken } = req.body;
    if (!idToken) return res.status(400).json({ error: 'idToken is required' });

    const ticket = await googleClient.verifyIdToken({
      idToken,
      audience: process.env.GOOGLE_CLIENT_ID,
    });
    const payload = ticket.getPayload();
    const email = normaliseEmail(payload.email);

    let user = await User.findOne({ where: { email } });
    if (!user) {
      user = await User.create({ name: (payload.name || email).slice(0, 100), email });
    }
    res.json({ token: issueToken(user), user: publicUser(user) });
  } catch (err) {
    console.error('Google sign-in failed:', err.message);
    res.status(401).json({ error: 'Google sign-in failed' });
  }
});

module.exports = router;
