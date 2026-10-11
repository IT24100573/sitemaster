import { Component, type ErrorInfo, type ReactNode } from 'react';
import { RotateCcw } from 'lucide-react';

interface State {
  hasError: boolean;
}

/** Last line of defence: a rendering error shows a recovery screen, not a blank page. */
export class ErrorBoundary extends Component<{ children: ReactNode }, State> {
  state: State = { hasError: false };

  static getDerivedStateFromError(): State {
    return { hasError: true };
  }

  componentDidCatch(error: Error, info: ErrorInfo): void {
    console.error('Unhandled UI error', error, info.componentStack);
  }

  render() {
    if (!this.state.hasError) {
      return this.props.children;
    }
    return (
      <div className="flex min-h-screen items-center justify-center bg-surface p-6">
        <div className="card max-w-md p-8 text-center">
          <h1 className="text-xl font-semibold text-ink">This page stopped working</h1>
          <p className="mt-2 text-sm text-muted">
            An unexpected error occurred while showing this page. Reload to try again.
          </p>
          <button
            type="button"
            onClick={() => window.location.reload()}
            className="mt-6 inline-flex h-11 items-center gap-2 rounded-lg bg-primary px-4 text-sm font-medium text-white hover:bg-primary-dark"
          >
            <RotateCcw className="h-4 w-4" aria-hidden />
            Reload page
          </button>
        </div>
      </div>
    );
  }
}
