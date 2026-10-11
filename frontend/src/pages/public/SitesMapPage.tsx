import { useQuery } from '@tanstack/react-query';
import { MapContainer, Marker, Popup, TileLayer } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import { publicApi } from '../../api/publicApi';
import { humanize } from '../../lib/format';
import { pinIcon, STATUS_PIN_COLORS } from '../../lib/mapIcons';
import { Alert } from '../../components/ui/Alert';
import { ProgressBar } from '../../components/ui/ProgressBar';
import { Spinner } from '../../components/ui/Spinner';
import { toApiError } from '../../lib/apiError';

const SRI_LANKA: [number, number] = [7.8731, 80.7718];

/** Every active, delayed and completed project on an OpenStreetMap map. */
export default function SitesMapPage() {
  const markers = useQuery({ queryKey: ['public', 'map'], queryFn: publicApi.mapMarkers });

  return (
    <section className="mx-auto max-w-6xl px-4 py-12 sm:px-6">
      <h1 className="text-3xl font-bold tracking-tight text-ink">Our sites</h1>
      <p className="mt-2 text-muted">Select a pin to see the project and how far along it is.</p>

      <ul className="mt-4 flex flex-wrap gap-4 text-sm text-muted" aria-label="Map legend">
        {Object.entries(STATUS_PIN_COLORS).map(([status, color]) => (
          <li key={status} className="flex items-center gap-2">
            <span className="h-3 w-3 rounded-full" style={{ background: color }} aria-hidden />
            {humanize(status)}
          </li>
        ))}
      </ul>

      {markers.isError && (
        <div className="mt-4">
          <Alert tone="error">{toApiError(markers.error).message}</Alert>
        </div>
      )}

      <div className="relative mt-6 h-[65vh] min-h-[400px] overflow-hidden rounded-card border border-line">
        {markers.isLoading && (
          <div className="absolute inset-0 z-[500] flex items-center justify-center bg-white/60">
            <Spinner className="h-6 w-6" />
          </div>
        )}
        <MapContainer center={SRI_LANKA} zoom={7} className="h-full w-full">
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />
          {markers.data?.map((m) => (
            <Marker key={m.id} position={[Number(m.latitude), Number(m.longitude)]} icon={pinIcon(STATUS_PIN_COLORS[m.status] ?? '#F97316')}>
              <Popup>
                <div className="min-w-[180px] space-y-1.5">
                  <p className="font-semibold text-ink">{m.name}</p>
                  <p className="text-xs text-muted">{m.location}</p>
                  <p className="text-xs">Status: {humanize(m.status)}</p>
                  <ProgressBar value={m.progress} label={`${m.name} progress`} />
                </div>
              </Popup>
            </Marker>
          ))}
        </MapContainer>
      </div>
    </section>
  );
}
