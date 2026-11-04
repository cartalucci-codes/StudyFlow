// Express 4 does not catch errors thrown inside async route handlers, which would crash the
// whole Node process on a bad request. Wrapping every handler forwards errors to the central
// error handler in server.js instead, so one bad request can never take the API down.
module.exports = (fn) => (req, res, next) => Promise.resolve(fn(req, res, next)).catch(next);
