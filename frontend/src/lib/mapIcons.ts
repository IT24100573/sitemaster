import { divIcon, type DivIcon } from 'leaflet';

/**
 * Map pins drawn with CSS (a divIcon) so no marker images need bundling.
 * The fill colour reflects the project status.
 */
export function pinIcon(color: string): DivIcon {
  return divIcon({
    className: '',
    html: `<span style="display:block;width:22px;height:22px;border-radius:50% 50% 50% 0;background:${color};transform:rotate(-45deg);border:3px solid #fff;box-shadow:0 1px 4px rgba(0,0,0,.35)"></span>`,
    iconSize: [22, 22],
    iconAnchor: [11, 22],
    popupAnchor: [0, -22],
  });
}

export const STATUS_PIN_COLORS: Record<string, string> = {
  ACTIVE: '#2563EB',
  DELAYED: '#DC2626',
  COMPLETED: '#16A34A',
};
