import { MapPinOff } from 'lucide-react';
import { ErrorPage } from './ErrorPage';

export default function NotFoundPage({ embedded = false }: { embedded?: boolean }) {
  return (
    <ErrorPage
      code="404"
      title="Page not found"
      message="The address may be mistyped, or the page has moved. Check the link or head back."
      icon={<MapPinOff className="h-7 w-7" aria-hidden />}
      embedded={embedded}
    />
  );
}
