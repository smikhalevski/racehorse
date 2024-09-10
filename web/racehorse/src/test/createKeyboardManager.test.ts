import { createEasingFunction } from '../main/createKeyboardManager';

describe('createEasingFunction', () => {
  test('interpolates values', () => {
    const easing = createEasingFunction([0, 1]);

    expect(easing(-1)).toBe(0);
    expect(easing(0)).toBe(0);
    expect(easing(0.5)).toBe(0.5);
    expect(easing(1)).toBe(1);
    expect(easing(2)).toBe(1);
  });
});
