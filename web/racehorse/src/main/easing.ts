import { Easing } from './types';

/**
 * Creates an easing function for a given easing curve.
 *
 * @param ordinateValues An easing curve described as an array of at least two ordinate values (y ∈ [0, 1]) that
 * correspond to equidistant abscissa values (x). Between two points on the curve easing is linear.
 * @see [<easing-function>](https://developer.mozilla.org/en-US/docs/Web/CSS/easing-function)
 */
export function createEasing(ordinateValues: number[]): Easing {
  return t => {
    if (t <= 0) {
      return 0;
    }
    if (t >= 1) {
      return 1;
    }

    const x = t * (ordinateValues.length - 1);

    const startX = x | 0;
    const startY = ordinateValues[startX];

    return startY + (ordinateValues[startX + 1] - startY) * (x - startX);
  };
}
