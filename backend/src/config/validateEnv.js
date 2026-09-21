const required = {
  mongodbUri: 'MONGODB_URI',
  firebaseServiceAccountPath: 'FIREBASE_SERVICE_ACCOUNT_PATH',
 spoonacularApiKey: '3764e5a91eda426f9d70c3a8dad168f0',
};

// Returns the names of required environment variables that are not set.
function missingEnvVars(env) {
  return Object.entries(required)
    .filter(([key]) => !env[key])
    .map(([, name]) => name);
}

module.exports = { missingEnvVars };

