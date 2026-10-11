import { useEffect } from 'react';
import { MapContainer, Marker, TileLayer, useMap, useMapEvents } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import { pinIcon } from '../../lib/mapIcons';

/** Centre of Sri Lanka, used before a location is chosen. */
const DEFAULT_CENTER: [number, number] = [7.8731, 80.7718];

interface LocationPickerProps {
  latitude: number | null;
  longitude: number | null;
  onPick: (latitude: number, longitude: number) => void;
}

function ClickHandler({ onPick }: { onPick: LocationPickerProps['onPick'] }) {
  useMapEvents({
    click(event) {
      onPick(Number(event.latlng.lat.toFixed(6)), Number(event.latlng.lng.toFixed(6)));
    },
  });
  return null;
}

function Recenter({ position }: { position: [number, number] | null }) {
  const map = useMap();
  useEffect(() => {
    if (position) {
      map.setView(position, Math.max(map.getZoom(), 12));
    }
  }, [position, map]);
  return null;
}

/** Click the map to set a project's coordinates (OpenStreetMap tiles, no API key). */
export function LocationPicker({ latitude, longitude, onPick }: LocationPickerProps) {
  const position: [number, number] | null =
    latitude !== null && longitude !== null && !Number.isNaN(latitude) && !Number.isNaN(longitude)
      ? [latitude, longitude]
      : null;

  return (
    <div className="h-64 overflow-hidden rounded-lg border border-line">
      <MapContainer center={position ?? DEFAULT_CENTER} zoom={position ? 12 : 7} className="h-full w-full">
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        <ClickHandler onPick={onPick} />
        <Recenter position={position} />
        {position && <Marker position={position} icon={pinIcon('#F97316')} />}
      </MapContainer>
    </div>
  );
}
