import {Injectable, signal} from '@angular/core';
import {AppNotification} from '../models/notification.model';

@Injectable({
    providedIn: 'root'
})
export class NotificationService {
    readonly notification = signal<AppNotification | null>(null);

    showError(message: string): void {
        this.notification.set({type: 'error', message});
    }

    showSuccess(message: string): void {
        this.notification.set({type: 'success', message});
    }

    clear(): void {
        this.notification.set(null);
    }
}
