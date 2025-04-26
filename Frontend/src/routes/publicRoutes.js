import Home from '../pages/public/Home';
import About from '../pages/public/About';
import Contact from '../pages/public/Contact';
import TermsAndConditions from '../pages/public/TermsAndConditions';
import FAQ from '../pages/public/FAQ';

export const publicRoutes = [
  { path: '/', element: <Home /> },
  { path: '/about', element: <About /> },
  { path: '/contact', element: <Contact /> },
  { path: '/terms', element: <TermsAndConditions /> },
  { path: '/faq', element: <FAQ /> },
];
