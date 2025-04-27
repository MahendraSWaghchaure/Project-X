// import ParentDashboard from '../pages/parents/ParentDashboard'; // create this page

// export const parentRoutes = [
//   { path: '/parent/dashboard', element: <ParentDashboard /> },
// ];


import ParentDashboard from '../pages/parents/ParentDashboard';
import TrackBus from '../pages/parents/TrackBus';
import StudentAttendance from '../pages/parents/StudentAttendance';
import Notifications from '../pages/parents/Notifications';

export const parentRoutes = [
  { path: '/parent/dashboard', element: <ParentDashboard /> },
  { path: '/parent/track-bus', element: <TrackBus /> },
  { path: '/parent/attendance', element: <StudentAttendance /> },
  { path: '/parent/notifications', element: <Notifications /> },
];
