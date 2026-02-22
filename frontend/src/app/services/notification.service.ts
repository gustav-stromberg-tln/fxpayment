import {Injectable, signal} from '@angular/core';
import {AppNotification} from '../models/notification.model';
import {NOTIFICATION_AUTO_DISMISS_MS} from '../app.constants';

@Injectable({
    providedIn: 'root'
})
export class NotificationService {
    readonly notification = signal<AppNotification | null>(null);

    private autoDismissTimer: ReturnType<typeof setTimeout> | null = null;

    showError(message: string): void {
        this.clearAutoDismissTimer();
        this.notification.set({type: 'error', message});
    }

    showSuccess(message: string): void {
        this.clearAutoDismissTimer();
        this.notification.set({type: 'success', message});
        this.autoDismissTimer = setTimeout(() => this.clear(), NOTIFICATION_AUTO_DISMISS_MS);
    }

    clear(): void {
        this.clearAutoDismissTimer();
        this.notification.set(null);
    }

    private clearAutoDismissTimer(): void {
        if (this.autoDismissTimer !== null) {
            clearTimeout(this.autoDismissTimer);
            this.autoDismissTimer = null;
        }
    }
}
