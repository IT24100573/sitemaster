import { ShieldAlert } from 'lucide-react';
import { ErrorPage } from './ErrorPage';

export default function ForbiddenPage() {
  return (
    <ErrorPage
      code="403"
      title="You do not have access to this page"
      message="Your role does not include this area. If you need it, ask your administrator to update your permissions."
      icon={<ShieldAlert className="h-7 w-7" aria-hidden />}
    />
  );
}
