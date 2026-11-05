const detectImageType = require('../src/utils/imageType');

const filler = Buffer.alloc(20, 7);

describe('detectImageType', () => {
  it('recognises a JPEG', () => {
    const bytes = Buffer.concat([Buffer.from([0xff, 0xd8, 0xff, 0xe1]), filler]);

    expect(detectImageType(bytes)).toBe('image/jpeg');
  });

  it('recognises a PNG', () => {
    const bytes = Buffer.concat([
      Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
      filler,
    ]);

    expect(detectImageType(bytes)).toBe('image/png');
  });

  it('recognises a WebP', () => {
    const bytes = Buffer.concat([
      Buffer.from('RIFF'),
      Buffer.from([1, 2, 3, 4]),
      Buffer.from('WEBP'),
      filler,
    ]);

    expect(detectImageType(bytes)).toBe('image/webp');
  });

  it('does not mistake other RIFF files, such as audio, for WebP', () => {
    const bytes = Buffer.concat([
      Buffer.from('RIFF'),
      Buffer.from([1, 2, 3, 4]),
      Buffer.from('WAVE'),
      filler,
    ]);

    expect(detectImageType(bytes)).toBeNull();
  });

  it.each([
    ['text', Buffer.from('this is just some text, not a photo')],
    ['a PDF', Buffer.from('%PDF-1.7 and then some more bytes')],
    ['a GIF', Buffer.from('GIF89a and then some more bytes here')],
    ['something too short', Buffer.from([0xff, 0xd8, 0xff])],
    ['an empty buffer', Buffer.alloc(0)],
  ])('rejects %s', (name, bytes) => {
    expect(detectImageType(bytes)).toBeNull();
  });

  it('rejects anything that is not a buffer', () => {
    expect(detectImageType('image')).toBeNull();
    expect(detectImageType(undefined)).toBeNull();
  });
});
