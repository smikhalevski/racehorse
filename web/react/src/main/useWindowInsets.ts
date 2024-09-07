import { Rect } from 'racehorse';
import { useEffect, useState } from 'react';
import { useDeviceManager } from './managers';

/**
 * Returns the current window insets and re-renders the component if device orientation changes.
 *
 * @param typeMask Bit mask of {@link InsetType}s to query the insets for. By default, display cutout, navigation and
 * status bars are included.
 */
export function useWindowInsets(typeMask?: number): Rect {
  const manager = useDeviceManager();

  const [windowInsets, setWindowInsets] = useState(() => manager.getWindowInsets(typeMask));

  useEffect(() => {
    const listener = () => setWindowInsets(manager.getWindowInsets(typeMask));

    window.addEventListener('orientationchange', listener);

    return () => window.removeEventListener('orientationchange', listener);
  }, [manager]);

  return windowInsets;
}
