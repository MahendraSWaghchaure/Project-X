import { useEffect } from 'react';
import { useLocation } from 'react-router-dom';

/** React Router doesn't reset scroll position on navigation - without this, clicking from the
 * bottom of one public page to another lands you mid-page instead of at the top. */
export function ScrollToTop() {
  const { pathname } = useLocation();
  useEffect(() => {
    window.scrollTo(0, 0);
  }, [pathname]);
  return null;
}
