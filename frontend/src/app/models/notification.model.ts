export type NotificationType = 'error' | 'success';

export interface AppNotification {
    type: NotificationType;
    message: string;
}
