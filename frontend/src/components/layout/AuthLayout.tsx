import { Outlet } from 'react-router-dom';
import { ClipboardCheck, HardHat, Truck } from 'lucide-react';
import { Brand } from './Brand';

const HIGHLIGHTS = [
  { icon: ClipboardCheck, text: 'Daily reports and approvals move from site to office the same day.' },
  { icon: Truck, text: 'Material requests follow through to purchase order, payment and delivery.' },
  { icon: HardHat, text: 'Crews check in on site; verified attendance flows straight into payroll.' },
];

/** Split screen for sign-in pages: brand panel on large screens, form on the right. */
export function AuthLayout() {
  return (
    <div className="flex min-h-screen bg-surface">
      <aside className="relative hidden w-[44%] max-w-xl flex-col justify-between overflow-hidden bg-ink p-12 text-white lg:flex">
        <div className="hazard-band absolute inset-x-0 top-0 h-3" aria-hidden />
        <Brand inverted />
        <div>
          <h1 className="max-w-md text-4xl font-bold leading-tight tracking-tight">
            Every site, every crew, every rupee in one place.
          </h1>
          <ul className="mt-10 space-y-5">
            {HIGHLIGHTS.map(({ icon: Icon, text }) => (
              <li key={text} className="flex items-start gap-3 text-sm leading-6 text-white/80">
                <span className="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-md bg-primary/15 text-primary">
                  <Icon className="h-4 w-4" aria-hidden />
                </span>
                {text}
              </li>
            ))}
          </ul>
        </div>
        <div className="hazard-band absolute inset-x-0 bottom-0 h-3" aria-hidden />
      </aside>

      <main className="flex flex-1 items-center justify-center px-4 py-10 sm:px-6">
        <div className="w-full max-w-md">
          <div className="mb-8 lg:hidden">
            <Brand />
          </div>
          <Outlet />
        </div>
      </main>
    </div>
  );
}
