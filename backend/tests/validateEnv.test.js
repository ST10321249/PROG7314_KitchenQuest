const { missingEnvVars } = require('../src/config/validateEnv');

describe('missingEnvVars', () => {
  it('reports nothing when everything is set', () => {
    const env = { mongodbUri: 'mongodb://x', firebaseServiceAccountPath: './key.json' };

    expect(missingEnvVars(env)).toEqual([]);
  });

  it('names each missing variable', () => {
    expect(missingEnvVars({})).toEqual(['MONGODB_URI', 'FIREBASE_SERVICE_ACCOUNT_PATH']);
    expect(missingEnvVars({ mongodbUri: 'mongodb://x' })).toEqual([
      'FIREBASE_SERVICE_ACCOUNT_PATH',
    ]);
  });

  it('treats an empty value as missing', () => {
    expect(missingEnvVars({ mongodbUri: '', firebaseServiceAccountPath: './key.json' })).toEqual([
      'MONGODB_URI',
    ]);
  });
});
