const required = {
  mongodbUri: 'MONGODB_URI',
  firebaseServiceAccountPath: 'FIREBASE_SERVICE_ACCOUNT_PATH',
};

// Returns the names of required environment variables that are not set.
function missingEnvVars(env) {
  return Object.entries(required)
    .filter(([key]) => !env[key])
    .map(([, name]) => name);
}

module.exports = { missingEnvVars };
