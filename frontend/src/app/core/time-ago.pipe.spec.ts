import { TimeAgoPipe } from './time-ago.pipe';

describe('TimeAgoPipe', () => {
  const pipe = new TimeAgoPipe();

  function isoSecondsAgo(seconds: number): string {
    return new Date(Date.now() - seconds * 1000).toISOString();
  }

  it('returns "ahora" for very recent dates', () => {
    expect(pipe.transform(isoSecondsAgo(10))).toBe('ahora');
  });

  it('returns minutes under an hour', () => {
    expect(pipe.transform(isoSecondsAgo(5 * 60))).toBe('hace 5 min');
  });

  it('returns hours under a day', () => {
    expect(pipe.transform(isoSecondsAgo(3 * 3600))).toBe('hace 3 h');
  });

  it('returns singular day and plural days', () => {
    expect(pipe.transform(isoSecondsAgo(86400 + 3600))).toBe('hace 1 día');
    expect(pipe.transform(isoSecondsAgo(3 * 86400))).toBe('hace 3 días');
  });
});
