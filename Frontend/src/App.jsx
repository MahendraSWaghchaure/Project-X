import { Routes, Route } from 'react-router-dom';
import PrivateRoute from './routes/PrivateRoute';
import AdminDashboard from './pages/schoolAdmin/AdminDashboard';
import DriverDashboard from './pages/driver/DriverDashboard';
import ParentDashboard from './pages/parents/ParentDashboard';
import Login from './pages/auth/Login';
import Home from './pages/public/Home';
// ... other imports

function App() {
  return (
    <Routes>
      {/* Public Routes */}
      <Route path="/" element={<Home />} />
      <Route path="/login" element={<Login />} />

      {/* Admin Protected Routes */}
      <Route element={<PrivateRoute allowedRoles={['ADMIN']} />}>
        <Route path="/admin/dashboard" element={<AdminDashboard />} />
        {/* other admin routes */}
      </Route>

      {/* Driver Protected Routes */}
      <Route element={<PrivateRoute allowedRoles={['DRIVER']} />}>
        <Route path="/driver/dashboard" element={<DriverDashboard />} />
        {/* other driver routes */}
      </Route>

      {/* Parent Protected Routes */}
      <Route element={<PrivateRoute allowedRoles={['PARENT']} />}>
        <Route path="/parent/dashboard" element={<ParentDashboard />} />
        {/* other parent routes */}
      </Route>

      {/* Unauthorized Page */}
      <Route path="/unauthorized" element={<div>Unauthorized Access</div>} />
    </Routes>
  );
}

export default App;
