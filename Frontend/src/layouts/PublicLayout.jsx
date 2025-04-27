//  Navbar/Footer for public pages

import { Outlet } from 'react-router-dom';

const PublicLayout = () => {
  return (
    <div className="public-layout">
      {/* Simple header/footer for public pages */}
      <Outlet />
    </div>
  );
};

export default PublicLayout;

