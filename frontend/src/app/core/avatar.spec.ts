import { avatarStyle, initialOf } from './avatar';

describe('avatar utils', () => {
  it('produces a deterministic style for the same alias', () => {
    expect(avatarStyle('marilo')).toEqual(avatarStyle('marilo'));
  });

  it('produces different hues for different aliases', () => {
    expect(avatarStyle('marilo')).not.toEqual(avatarStyle('cgomez'));
  });

  it('uses OKLCH colors', () => {
    const style = avatarStyle('valen');
    expect(style['background']).toContain('oklch');
    expect(style['color']).toContain('oklch');
  });

  it('extracts the first character as initial', () => {
    expect(initialOf('marilo')).toBe('m');
    expect(initialOf('  valen')).toBe('v');
  });
});
