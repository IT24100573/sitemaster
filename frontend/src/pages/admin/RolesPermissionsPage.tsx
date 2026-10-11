import { useEffect, useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Lock, RotateCcw } from 'lucide-react';
import { rolesApi } from '../../api/rolesApi';
import { CURRENT_USER_KEY, useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { toApiError } from '../../lib/apiError';
import { humanize } from '../../lib/format';
import { Alert } from '../../components/ui/Alert';
import { Button } from '../../components/ui/Button';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { PageHeader } from '../../components/ui/PageHeader';
import { FullPageSpinner } from '../../components/ui/Spinner';
import type { PermissionCell } from '../../types/role';

const MATRIX_KEY = ['roles', 'matrix'] as const;

function sameSet(a: Set<number>, b: Set<number>): boolean {
  return a.size === b.size && [...a].every((id) => b.has(id));
}

/** Admin edits which actions each role may perform on each module. */
export default function RolesPermissionsPage() {
  const { hasPermission } = useAuth();
  const canEdit = hasPermission('ROLE:EDIT');
  const toast = useToast();
  const queryClient = useQueryClient();
  const matrix = useQuery({ queryKey: MATRIX_KEY, queryFn: rolesApi.matrix });
  const [roleId, setRoleId] = useState<number | null>(null);
  const [selected, setSelected] = useState<Set<number>>(new Set());
  const [confirming, setConfirming] = useState(false);

  const role = matrix.data?.roles.find((r) => r.id === roleId) ?? null;
  const saved = useMemo(() => new Set(role?.permissionIds ?? []), [role]);
  const locked = useMemo(() => new Set(role?.lockedPermissionIds ?? []), [role]);
  const dirty = role !== null && !sameSet(saved, selected);

  useEffect(() => {
    if (matrix.data && roleId === null && matrix.data.roles.length > 0) {
      setRoleId(matrix.data.roles[0].id);
    }
  }, [matrix.data, roleId]);

  useEffect(() => {
    setSelected(new Set(saved));
  }, [saved]);

  /** module -> action -> permission */
  const grid = useMemo(() => {
    const map = new Map<string, Map<string, PermissionCell>>();
    matrix.data?.permissions.forEach((p) => {
      if (!map.has(p.module)) {
        map.set(p.module, new Map());
      }
      map.get(p.module)?.set(p.action, p);
    });
    return map;
  }, [matrix.data]);

  const save = useMutation({
    mutationFn: () => rolesApi.updatePermissions(roleId as number, [...selected]),
    onSuccess: async (updated) => {
      toast.success(`Permissions for ${updated.displayName} saved. Signed-in users get them on their next action.`);
      setConfirming(false);
      await queryClient.invalidateQueries({ queryKey: MATRIX_KEY });
      await queryClient.invalidateQueries({ queryKey: CURRENT_USER_KEY });
    },
    onError: (error) => {
      toast.error(toApiError(error).message);
      setConfirming(false);
    },
  });

  const toggle = (id: number) => {
    if (!canEdit || locked.has(id)) {
      return;
    }
    setSelected((current) => {
      const next = new Set(current);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  };

  const toggleRow = (module: string) => {
    if (!canEdit) {
      return;
    }
    const ids = [...(grid.get(module)?.values() ?? [])].map((p) => p.id);
    const allOn = ids.every((id) => selected.has(id));
    setSelected((current) => {
      const next = new Set(current);
      ids.forEach((id) => {
        if (locked.has(id)) {
          return;
        }
        if (allOn) {
          next.delete(id);
        } else {
          next.add(id);
        }
      });
      return next;
    });
  };

  const changeRole = (id: number) => {
    if (dirty && !window.confirm('You have unsaved changes for this role. Discard them?')) {
      return;
    }
    setRoleId(id);
  };

  if (matrix.isLoading) {
    return <FullPageSpinner />;
  }
  if (matrix.isError || !matrix.data) {
    return <Alert tone="error">{toApiError(matrix.error).message}</Alert>;
  }

  return (
    <>
      <PageHeader
        title="Roles & permissions"
        description="Tick what each role may do in each module. Changes reach signed-in users on their next action."
      />

      <div className="mb-4 flex flex-wrap gap-2" role="tablist" aria-label="Roles">
        {matrix.data.roles.map((r) => (
          <button
            key={r.id}
            type="button"
            role="tab"
            aria-selected={r.id === roleId}
            onClick={() => changeRole(r.id)}
            className={`rounded-lg border px-3 py-2 text-sm font-medium transition-colors ${
              r.id === roleId
                ? 'border-primary bg-primary-light text-primary-dark'
                : 'border-line bg-white text-muted hover:text-ink'
            }`}
          >
            {r.displayName}
            <span className="ml-2 text-xs font-normal text-muted">{r.userCount} users</span>
          </button>
        ))}
      </div>

      {role && (
        <div className="card overflow-hidden">
          <div className="flex flex-col gap-3 border-b border-line p-4 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <h2 className="font-semibold text-ink">{role.displayName}</h2>
              {role.description && <p className="text-sm text-muted">{role.description}</p>}
            </div>
            {canEdit && (
              <div className="flex gap-2">
                <Button variant="secondary" size="sm" disabled={!dirty} onClick={() => setSelected(new Set(saved))}>
                  <RotateCcw className="h-4 w-4" aria-hidden />
                  Undo changes
                </Button>
                <Button size="sm" disabled={!dirty} onClick={() => setConfirming(true)}>
                  Save permissions
                </Button>
              </div>
            )}
          </div>

          <div className="overflow-x-auto">
            <table className="w-full min-w-[640px] text-sm">
              <thead className="bg-surface text-muted">
                <tr>
                  <th scope="col" className="px-4 py-3 text-left font-medium">
                    Module
                  </th>
                  {matrix.data.actions.map((action) => (
                    <th key={action} scope="col" className="px-4 py-3 text-center font-medium">
                      {humanize(action)}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {matrix.data.modules.map((module) => (
                  <tr key={module} className="border-t border-line">
                    <th scope="row" className="px-4 py-2.5 text-left font-medium text-ink">
                      {canEdit ? (
                        <button
                          type="button"
                          onClick={() => toggleRow(module)}
                          className="hover:text-primary"
                          title="Tick or clear the whole row"
                        >
                          {humanize(module)}
                        </button>
                      ) : (
                        humanize(module)
                      )}
                    </th>
                    {matrix.data.actions.map((action) => {
                      const cell = grid.get(module)?.get(action);
                      if (!cell) {
                        return <td key={action} />;
                      }
                      const isLocked = locked.has(cell.id);
                      return (
                        <td key={action} className="px-4 py-2.5 text-center">
                          <label className="inline-flex cursor-pointer items-center justify-center">
                            <input
                              type="checkbox"
                              checked={selected.has(cell.id)}
                              disabled={!canEdit || isLocked}
                              onChange={() => toggle(cell.id)}
                              className="h-4 w-4 rounded border-line accent-primary disabled:cursor-not-allowed"
                              aria-label={`${humanize(role.name)}: ${humanize(action)} ${humanize(module)}`}
                            />
                            {isLocked && <Lock className="ml-1 h-3 w-3 text-muted" aria-label="Required" />}
                          </label>
                        </td>
                      );
                    })}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <ConfirmDialog
        open={confirming}
        title={`Save permissions for ${role?.displayName ?? 'this role'}?`}
        message={`${selected.size} permissions will be granted to every ${role?.displayName ?? ''} user.`}
        confirmLabel="Save permissions"
        loading={save.isPending}
        onConfirm={() => save.mutate()}
        onCancel={() => setConfirming(false)}
      />
    </>
  );
}
