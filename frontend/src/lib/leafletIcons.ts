import L from 'leaflet';

// Leaflet's default marker PNGs resolve via relative paths that break under Vite's bundling, and
// pulling them in as static assets is one more thing to keep in sync. Inline SVG sidesteps both -
// no bundler asset wiring, no CDN dependency.
function svgIcon(fill: string): L.DivIcon {
  return L.divIcon({
    className: '',
    html: `<svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24">
      <path fill="${fill}" stroke="white" stroke-width="1" d="M12 0C7 0 3 4 3 9c0 6.5 9 15 9 15s9-8.5 9-15c0-5-4-9-9-9z"/>
      <circle cx="12" cy="9" r="3.2" fill="white"/>
    </svg>`,
    iconSize: [32, 32],
    iconAnchor: [16, 32],
    popupAnchor: [0, -32],
  });
}

export const vehicleIcon = svgIcon('#2563eb');
export const stopIcon = svgIcon('#16a34a');
export const homeIcon = svgIcon('#d97706');
