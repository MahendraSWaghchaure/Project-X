import DriverDashboard from '../pages/driver/DriverDashboard';
import AssignedRoute from '../pages/driver/AssignedRoute';
import Attendance from '../pages/driver/Attendance';

export const driverRoutes = [
  { path: '/driver/dashboard', element: <DriverDashboard /> },
  { path: '/driver/assigned-route', element: <AssignedRoute /> },
  { path: '/driver/attendance', element: <Attendance /> },
];


