const { missingEnvVars } = require('../src/config/validateEnv');

describe('missingEnvVars', () => {
  const completeEnv = {
    mongodbUri: 'mongodb://x',
    firebaseServiceAccountPath: './key.json',
    spoonacularApiKey: 'test-key',
  };

  it('reports nothing when everything is set', () => {
    expect(missingEnvVars(completeEnv)).toEqual([]);
  });

  it('names each missing variable', () => {
    expect(missingEnvVars({})).toEqual([
      'MONGODB_URI',
      'FIREBASE_SERVICE_ACCOUNT_PATH',
      'SPOONACULAR_API_KEY',
    ]);

    expect(
      missingEnvVars({
        mongodbUri: 'mongodb://x',
        firebaseServiceAccountPath: './key.json',
      })
    ).toEqual(['SPOONACULAR_API_KEY']);
  });

  it('treats an empty value as missing', () => {
    expect(
      missingEnvVars({
        ...completeEnv,
        mongodbUri: '',
      })
    ).toEqual(['MONGODB_URI']);

    expect(
      missingEnvVars({
        ...completeEnv,
        spoonacularApiKey: '',
      })
    ).toEqual(['SPOONACULAR_API_KEY']);
  });
});