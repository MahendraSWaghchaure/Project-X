import { useEffect, useState } from 'react';
import type { IMessage } from '@stomp/stompjs';
import { createStompClient } from '@/lib/wsClient';
import type { CachedLocation } from '@/types/tracking';

export function useLiveVehicleLocation(vehicleId: number | null) {
  const [location, setLocation] = useState<CachedLocation | null>(null);
  const [connected, setConnected] = useState(false);

  useEffect(() => {
    if (vehicleId == null) {
      return;
    }

    const client = createStompClient();
    client.onConnect = () => {
      setConnected(true);
      client.subscribe(`/topic/vehicles/${vehicleId}/location`, (message: IMessage) => {
        setLocation(JSON.parse(message.body) as CachedLocation);
      });
    };
    client.onWebSocketClose = () => setConnected(false);
    client.activate();

    return () => {
      setConnected(false);
      void client.deactivate();
    };
  }, [vehicleId]);

  return { location, connected };
}
