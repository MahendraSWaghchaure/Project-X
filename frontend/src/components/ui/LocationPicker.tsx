import { MapContainer, Marker, TileLayer, useMapEvents } from 'react-leaflet';
import { homeIcon } from '@/lib/leafletIcons';

interface LocationPickerProps {
  latitude?: number | null;
  longitude?: number | null;
  onChange: (point: { latitude: number; longitude: number }) => void;
  label?: string;
  height?: number;
}

const INDIA_CENTER: [number, number] = [20.5937, 78.9629];

function ClickHandler({ onPick }: { onPick: (lat: number, lng: number) => void }) {
  useMapEvents({
    click(e) {
      onPick(e.latlng.lat, e.latlng.lng);
    },
  });
  return null;
}

/** Click-to-drop-a-pin location input, used anywhere a lat/lng pair used to be typed by hand. */
export function LocationPicker({ latitude, longitude, onChange, label = 'Location', height = 280 }: LocationPickerProps) {
  const hasValue = latitude != null && longitude != null;
  const center: [number, number] = hasValue ? [latitude, longitude] : INDIA_CENTER;

  return (
    <div className="flex flex-col gap-1 sm:col-span-2">
      <label className="text-sm font-medium text-slate-700">{label}</label>
      <div className="overflow-hidden rounded-md border border-slate-300" style={{ height }}>
        <MapContainer center={center} zoom={hasValue ? 15 : 5} className="h-full w-full">
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />
          <ClickHandler onPick={(lat, lng) => onChange({ latitude: lat, longitude: lng })} />
          {hasValue && <Marker position={[latitude, longitude]} icon={homeIcon} />}
        </MapContainer>
      </div>
      <p className="text-xs text-slate-500">
        {hasValue ? `Pinned at ${latitude.toFixed(5)}, ${longitude.toFixed(5)} — click to move it.` : 'Click the map to drop a pin.'}
      </p>
    </div>
  );
}
