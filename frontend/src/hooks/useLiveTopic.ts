import { useEffect, useRef } from 'react';
import type { IMessage } from '@stomp/stompjs';
import { createStompClient } from '@/lib/wsClient';

/**
 * Subscribes to one or more STOMP topics and invokes onMessage for every frame - used where the
 * UI just needs to know "something changed, refetch" rather than caring about the payload shape
 * (e.g. re-running a TanStack Query on a grievance update pushed from GrievanceBroadcaster).
 */
export function useLiveTopic(topics: (string | null | undefined)[], onMessage: () => void) {
  const onMessageRef = useRef(onMessage);
  onMessageRef.current = onMessage;

  const activeTopics = topics.filter((t): t is string => !!t);
  const topicsKey = activeTopics.join('|');

  useEffect(() => {
    if (activeTopics.length === 0) {
      return;
    }

    const client = createStompClient();
    client.onConnect = () => {
      activeTopics.forEach((topic) => {
        client.subscribe(topic, (_message: IMessage) => onMessageRef.current());
      });
    };
    client.activate();

    return () => {
      void client.deactivate();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [topicsKey]);
}
