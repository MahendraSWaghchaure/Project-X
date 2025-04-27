import StudentDashboard from '../pages/students/StudentDashboard';
import BusTracking from '../pages/students/BusTracking';

export const studentRoutes = [
  { path: '/student/dashboard', element: <StudentDashboard /> },
  { path: '/student/bus-tracking', element: <BusTracking /> },
];
