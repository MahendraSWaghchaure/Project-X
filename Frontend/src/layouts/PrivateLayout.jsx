// For logged-in roles

import { Outlet, Navigate } from 'react-router-dom';

const PrivateLayout = () => {
  const isAuthenticated = localStorage.getItem('token'); // or context auth check

  return isAuthenticated ? (
    <div className="private-layout">
      {/* You can add a Sidebar/Navbar here */}
      <Outlet />
    </div>
  ) : (
    <Navigate to="/login" replace />
  );
};

export default PrivateLayout;
