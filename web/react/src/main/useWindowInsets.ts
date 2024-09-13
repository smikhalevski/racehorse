import { Rect } from 'racehorse';
import { useEffect, useMemo, useState } from 'react';
import { useDeviceManager } from './managers';

/**
 * Returns the current window insets and re-renders the component if device orientation changes.
 *
 * @param typeMask Bit mask of {@link InsetType}s to query the insets for. By default, display cutout, navigation and
 * status bars are included.
 */
export function useWindowInsets(typeMask?: number): Rect {
  const manager = useDeviceManager();
  const [orientation, setOrientation] = useState(window.orientation);

  useEffect(() => {
    const orientationListener = () => setOrientation(window.orientation);

    window.addEventListener('orientationchange', orientationListener);

    return () => window.removeEventListener('orientationchange', orientationListener);
  }, []);

  return useMemo(() => manager.getWindowInsets(typeMask), [manager, typeMask, orientation]);
}
