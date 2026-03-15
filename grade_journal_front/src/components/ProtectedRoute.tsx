import { Navigate } from 'react-router-dom';
import { useAuthStore } from '../store/auth';
import type { Role } from '../types/auth';

type ProtectedRouteProps = {
  children: React.ReactNode;
  allowedRoles?: Role[];
};

export function ProtectedRoute({ children, allowedRoles }: ProtectedRouteProps) {
  const role = useAuthStore((state) => state.role);

  if (!role) {
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles && !allowedRoles.includes(role)) {
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
}