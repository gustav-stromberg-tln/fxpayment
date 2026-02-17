import {ErrorHandler, Injectable, inject} from '@angular/core';
import {NotificationService} from '../services/notification.service';

@Injectable()
export class GlobalErrorHandler implements ErrorHandler {
    private readonly notificationService = inject(NotificationService);

    handleError(error: unknown): void {
        console.error(error);
        this.notificationService.showError('An unexpected error occurred.');
    }
}
