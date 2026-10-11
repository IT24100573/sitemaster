import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { materialsApi } from '../../api/materialsApi';
import { toApiError } from '../../lib/apiError';
import { formatDateTime } from '../../lib/format';
import { Alert } from '../../components/ui/Alert';
import { Card } from '../../components/ui/Card';
import { PageHeader } from '../../components/ui/PageHeader';
import { ProjectSelect } from '../../components/ui/ProjectSelect';
import { Spinner } from '../../components/ui/Spinner';

/** Materials on hand at a site: deliveries add stock, approved daily reports use it. */
export default function SiteStockPage() {
  const [projectId, setProjectId] = useState('');
  const stock = useQuery({
    queryKey: ['stock', projectId],
    queryFn: () => materialsApi.stock(Number(projectId)),
    enabled: projectId !== '',
  });

  return (
    <>
      <PageHeader title="Site stock" description="Delivered materials add to stock; materials used in approved daily reports are taken off." />
      <div className="mb-4 max-w-sm">
        <ProjectSelect aria-label="Project" placeholder="Choose a project" value={projectId} onChange={(e) => setProjectId(e.target.value)} />
      </div>
      {projectId === '' ? (
        <p className="card p-8 text-center text-sm text-muted">Choose a project to see its stock.</p>
      ) : stock.isLoading ? (
        <Spinner />
      ) : stock.isError ? (
        <Alert tone="error">{toApiError(stock.error).message}</Alert>
      ) : (
        <Card title="Materials on site">
          {stock.data && stock.data.length > 0 ? (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[480px] text-sm">
                <thead className="text-left text-muted">
                  <tr className="border-b border-line">
                    <th scope="col" className="py-2 pr-4 font-medium">Material</th>
                    <th scope="col" className="py-2 pr-4 font-medium">Category</th>
                    <th scope="col" className="py-2 pr-4 text-right font-medium">Quantity</th>
                    <th scope="col" className="py-2 font-medium">Last change</th>
                  </tr>
                </thead>
                <tbody>
                  {stock.data.map((line) => (
                    <tr key={line.materialId} className="border-b border-line last:border-0">
                      <td className="py-2.5 pr-4 font-medium text-ink">{line.materialName}</td>
                      <td className="py-2.5 pr-4 text-muted">{line.category}</td>
                      <td className={`py-2.5 pr-4 text-right ${Number(line.quantity) === 0 ? 'text-red-600' : 'text-ink'}`}>
                        {Number(line.quantity).toLocaleString('en-LK', { maximumFractionDigits: 3 })} {line.unit}
                      </td>
                      <td className="py-2.5 text-muted">{formatDateTime(line.updatedAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <p className="text-sm text-muted">Nothing has been delivered to this site yet.</p>
          )}
        </Card>
      )}
    </>
  );
}
