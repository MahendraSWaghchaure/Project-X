import { apiClient, unwrap } from '@/lib/apiClient';
import type { ApiResponse } from '@/types/api';

export interface NotificationRecord {
  id: number;
  channel: 'SMS' | 'EMAIL' | 'PUSH';
  templateKey: string;
  status: 'SENT' | 'SKIPPED_NOT_CONFIGURED' | 'FAILED';
  createdAt: string;
}

export function listMyNotifications(): Promise<NotificationRecord[]> {
  return apiClient.get<ApiResponse<NotificationRecord[]>>('/notifications/me').then(unwrap);
}
