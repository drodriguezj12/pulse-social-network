/**
 * Deterministic avatar colors: the alias always maps to the same hue, so a
 * user is recognizable across feed, header and profile without images.
 */
export function avatarStyle(alias: string): { [key: string]: string } {
  let hash = 0;
  for (let i = 0; i < alias.length; i++) {
    hash = (hash * 31 + alias.charCodeAt(i)) | 0;
  }
  const hue = Math.abs(hash) % 360;
  return {
    background: `oklch(0.32 0.09 ${hue})`,
    color: `oklch(0.88 0.06 ${hue})`,
  };
}

export function initialOf(alias: string): string {
  return alias.trim().charAt(0);
}
