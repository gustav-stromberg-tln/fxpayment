import {ChangeDetectionStrategy, Component, inject} from '@angular/core';
import {PaymentFormComponent} from './components/payment-form/payment-form.component';
import {PaymentHistoryComponent} from './components/payment-history/payment-history.component';
import {NotificationService} from './services/notification.service';

@Component({
    selector: 'app-root',
    standalone: true,
    imports: [PaymentFormComponent, PaymentHistoryComponent],
    templateUrl: './app.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class AppComponent {
    private readonly notificationService = inject(NotificationService);

    readonly notification = this.notificationService.notification;

    clearNotification(): void {
        this.notificationService.clear();
    }
}
