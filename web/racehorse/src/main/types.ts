export type Unsubscribe = () => void;

/**
 * An easing function that converts `t` ∈ [0, 1] to `y` ∈ [0, 1].
 */
export type Easing = (t: number) => number;

/**
 * The animation spec.
 */
export interface Animation {
  /**
   * An animation duration in milliseconds.
   */
  duration: number;

  /**
   * An easing function applied to this animation.
   */
  easing: Easing;

  /**
   * A timestamp when an animation has started.
   */
  startTime: number;
}

/**
 * A value animation from {@link startValue} to {@link endValue}.
 */
export interface TweenAnimation extends Animation {
  /**
   * A value from which an animation starts.
   */
  startValue: number;

  /**
   * A value at which an animation ends.
   */
  endValue: number;
}

/**
 * Handles animation state changes.
 *
 * @template T The handled animation.
 */
export interface AnimationHandler<T extends Partial<Animation>> {
  /**
   * Triggered right before the animation is started.
   *
   * @param animation The animation that has started.
   */
  onStart(animation: T): void;

  /**
   * Triggered when an animated value is changed.
   *
   * @param animation The pending animation.
   * @param fraction The result of {@link Animation.easing} applied to {@link percent}.
   * @param percent The completed animation percentage [0, 1].
   */
  onProgress(animation: T, fraction: number, percent: number): void;

  /**
   * Triggered right after the animation has ended. Not called if animation was prematurely aborted.
   *
   * @param animation The animation that has ended.
   */
  onEnd(animation: T): void;

  /**
   * Triggered if the animation was prematurely aborted.
   *
   * @param animation The animation that was aborted.
   */
  onAbort(animation: T): void;
}
