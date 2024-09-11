import { Animation } from './types';
import { runAnimation } from './runAnimation';

/**
 * Options of the {@link scrollToElement} function.
 */
export interface ScrollToElementOptions {
  /**
   * The animation that the scroll should use. By default, no animation is applied and an element is centered instantly.
   */
  animation?: Partial<Animation>;

  /**
   * Top scroll padding.
   *
   * @default 0
   */
  paddingTop?: number;

  /**
   * Bottom scroll padding.
   *
   * @default 0
   */
  paddingBottom?: number;

  /**
   * The signal that cancels the animation.
   */
  signal?: AbortSignal;
}

/**
 * Scrolls the window so the element is vertically centered.
 *
 * @param element The element to scroll to.
 * @param options Scroll options.
 */
export function scrollToElement(element: Element, options: ScrollToElementOptions = {}): void {
  const { animation = {}, paddingTop = 0, paddingBottom = 0 } = options;

  let startY = -1;
  let endY = -1;

  const cancel = () => {
    window.addEventListener('touchstart', cancel, true);

    startY = endY = -1;
  };

  runAnimation(
    animation,
    {
      onStart() {
        window.addEventListener('touchstart', cancel, true);

        const { top, height } = element.getBoundingClientRect();

        startY = window.scrollY;
        endY = Math.max(0, startY + paddingTop + top - (window.innerHeight - paddingTop - paddingBottom - height) / 2);
      },

      onProgress(_animation, fraction) {
        if (startY !== -1) {
          window.scrollTo(0, startY + (endY - startY) * fraction);
        }
      },

      onEnd: cancel,

      onAbort: cancel,
    },
    options.signal
  );
}
