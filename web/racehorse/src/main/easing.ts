import { Easing } from './types';

/**
 * Creates an easing function for a given easing curve.
 *
 * @param ys An easing curve described as an array of at least two ordinate values (y ∈ [0, 1]) that correspond to
 * an equidistant abscissa values (x). Between two points on the curve easing is linear.
 * @see [<easing-function>](https://developer.mozilla.org/en-US/docs/Web/CSS/easing-function)
 */
export function createEasing(ys: number[]): Easing {
  return t => {
    if (t <= 0) {
      return 0;
    }
    if (t >= 1) {
      return 1;
    }

    const x = t * (ys.length - 1);

    const startX = x | 0;
    const startY = ys[startX];

    return startY + (ys[startX + 1] - startY) * (x - startX);
  };
}
