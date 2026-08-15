import { Route, Routes } from 'react-router-dom';
import { ProtectedRoute } from '@/components/layout/ProtectedRoute';
import { AppShell } from '@/components/layout/AppShell';
import { Toaster } from '@/components/ui/Toaster';

import { HomePage } from '@/features/public/HomePage';
import { NotFoundPage } from '@/features/public/NotFoundPage';
import { UnauthorizedPage } from '@/features/public/UnauthorizedPage';
import { DriverRegisterPage } from '@/features/public/DriverRegisterPage';
import { LoginPage } from '@/features/auth/LoginPage';
import { AcceptInvitePage } from '@/features/auth/AcceptInvitePage';

import { PlatformAdminDashboardPage } from '@/features/platform-admin/DashboardPage';
import { SchoolsPage } from '@/features/platform-admin/SchoolsPage';
import { PlatformAdminGrievancesPage } from '@/features/platform-admin/GrievancesPage';
import { PlatformAdminPaymentsPage } from '@/features/platform-admin/PaymentsPage';

import { SchoolAdminDashboardPage } from '@/features/school-admin/DashboardPage';
import { VehiclesPage } from '@/features/school-admin/VehiclesPage';
import { DriversPage } from '@/features/school-admin/DriversPage';
import { RoutesPage } from '@/features/school-admin/RoutesPage';
import { ChildrenPage as SchoolAdminChildrenPage } from '@/features/school-admin/ChildrenPage';
import { TripsPage } from '@/features/school-admin/TripsPage';
import { GrievancesPage as SchoolAdminGrievancesPage } from '@/features/school-admin/GrievancesPage';
import { PaymentsPage as SchoolAdminPaymentsPage } from '@/features/school-admin/PaymentsPage';
import { InsurancePage as SchoolAdminInsurancePage } from '@/features/school-admin/InsurancePage';

import { DriverDashboardPage } from '@/features/driver/DashboardPage';
import { DriverTripsPage } from '@/features/driver/DriverTripsPage';
import { DriverGrievancesPage } from '@/features/driver/GrievancesPage';
import { DriverPayrollPage } from '@/features/driver/PayrollPage';

import { ParentDashboardPage } from '@/features/parent/DashboardPage';
import { TrackBusPage } from '@/features/parent/TrackBusPage';
import { ParentChildrenPage } from '@/features/parent/ChildrenPage';
import { NotificationsPage } from '@/features/parent/NotificationsPage';
import { CompleteProfilePage } from '@/features/parent/CompleteProfilePage';
import { ParentProfilePage } from '@/features/parent/ProfilePage';
import { ParentGrievancesPage } from '@/features/parent/GrievancesPage';
import { ParentPaymentsPage } from '@/features/parent/PaymentsPage';

export default function App() {
  return (
    <>
      <Routes>
        {/* Public */}
        <Route path="/" element={<HomePage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/accept-invite/:token" element={<AcceptInvitePage />} />
        <Route path="/driver/register" element={<DriverRegisterPage />} />
        <Route path="/unauthorized" element={<UnauthorizedPage />} />

        {/* Parent completes their profile right after accepting an invite, before the normal shell/nav applies */}
        <Route element={<ProtectedRoute allowedRoles={['PARENT']} />}>
          <Route path="/parent/complete-profile" element={<CompleteProfilePage />} />
        </Route>

        <Route element={<ProtectedRoute allowedRoles={['PLATFORM_ADMIN']} />}>
          <Route element={<AppShell />}>
            <Route path="/platform-admin/dashboard" element={<PlatformAdminDashboardPage />} />
            <Route path="/platform-admin/schools" element={<SchoolsPage />} />
            <Route path="/platform-admin/grievances" element={<PlatformAdminGrievancesPage />} />
            <Route path="/platform-admin/payments" element={<PlatformAdminPaymentsPage />} />
          </Route>
        </Route>

        <Route element={<ProtectedRoute allowedRoles={['SCHOOL_ADMIN', 'PLATFORM_ADMIN']} />}>
          <Route element={<AppShell />}>
            <Route path="/school-admin/dashboard" element={<SchoolAdminDashboardPage />} />
            <Route path="/school-admin/vehicles" element={<VehiclesPage />} />
            <Route path="/school-admin/drivers" element={<DriversPage />} />
            <Route path="/school-admin/routes" element={<RoutesPage />} />
            <Route path="/school-admin/children" element={<SchoolAdminChildrenPage />} />
            <Route path="/school-admin/trips" element={<TripsPage />} />
            <Route path="/school-admin/grievances" element={<SchoolAdminGrievancesPage />} />
            <Route path="/school-admin/payments" element={<SchoolAdminPaymentsPage />} />
            <Route path="/school-admin/insurance" element={<SchoolAdminInsurancePage />} />
          </Route>
        </Route>

        <Route element={<ProtectedRoute allowedRoles={['DRIVER']} />}>
          <Route element={<AppShell />}>
            <Route path="/driver/dashboard" element={<DriverDashboardPage />} />
            <Route path="/driver/trips" element={<DriverTripsPage />} />
            <Route path="/driver/grievances" element={<DriverGrievancesPage />} />
            <Route path="/driver/payroll" element={<DriverPayrollPage />} />
          </Route>
        </Route>

        <Route element={<ProtectedRoute allowedRoles={['PARENT']} />}>
          <Route element={<AppShell />}>
            <Route path="/parent/dashboard" element={<ParentDashboardPage />} />
            <Route path="/parent/track" element={<TrackBusPage />} />
            <Route path="/parent/children" element={<ParentChildrenPage />} />
            <Route path="/parent/notifications" element={<NotificationsPage />} />
            <Route path="/parent/profile" element={<ParentProfilePage />} />
            <Route path="/parent/grievances" element={<ParentGrievancesPage />} />
            <Route path="/parent/payments" element={<ParentPaymentsPage />} />
          </Route>
        </Route>

        <Route path="*" element={<NotFoundPage />} />
      </Routes>
      <Toaster />
    </>
  );
}
