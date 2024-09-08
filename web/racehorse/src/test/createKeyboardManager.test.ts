import { createEasing } from '../main/createKeyboardManager';

describe('createEasing', () => {
  test('interpolates values', () => {
    const easing = createEasing([0, 1]);

    expect(easing(-1)).toBe(0);
    expect(easing(0)).toBe(0);
    expect(easing(0.5)).toBe(0.5);
    expect(easing(1)).toBe(1);
    expect(easing(2)).toBe(1);
  });
});
