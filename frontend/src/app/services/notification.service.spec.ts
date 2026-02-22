import {TestBed} from '@angular/core/testing';
import {vi} from 'vitest';
import {NotificationService} from './notification.service';
import {NOTIFICATION_AUTO_DISMISS_MS} from '../app.constants';

describe('NotificationService', () => {
    let service: NotificationService;

    beforeEach(() => {
        vi.useFakeTimers();
        TestBed.configureTestingModule({});
        service = TestBed.inject(NotificationService);
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    it('should have null notification initially', () => {
        expect(service.notification()).toBeNull();
    });

    it('should set error notification via showError', () => {
        const message = 'Something went wrong';
        service.showError(message);
        expect(service.notification()).toEqual({type: 'error', message});
    });

    it('should set success notification via showSuccess', () => {
        const message = 'Payment submitted';
        service.showSuccess(message);
        expect(service.notification()).toEqual({type: 'success', message});
    });

    it('should clear the notification', () => {
        service.showError('An error');
        service.clear();
        expect(service.notification()).toBeNull();
    });

    it('should overwrite previous notification with a new one', () => {
        service.showError('First error');
        service.showSuccess('Success message');
        expect(service.notification()).toEqual({type: 'success', message: 'Success message'});
    });

    it('should auto-dismiss success notification after timeout', () => {
        service.showSuccess('Payment submitted');
        expect(service.notification()).not.toBeNull();

        vi.advanceTimersByTime(NOTIFICATION_AUTO_DISMISS_MS);

        expect(service.notification()).toBeNull();
    });

    it('should not auto-dismiss error notification', () => {
        service.showError('Something went wrong');
        expect(service.notification()).not.toBeNull();

        vi.advanceTimersByTime(NOTIFICATION_AUTO_DISMISS_MS);

        expect(service.notification()).toEqual({type: 'error', message: 'Something went wrong'});
    });

    it('should cancel previous auto-dismiss timer when showing a new notification', () => {
        service.showSuccess('First success');
        vi.advanceTimersByTime(NOTIFICATION_AUTO_DISMISS_MS - 1000);

        service.showError('An error');
        vi.advanceTimersByTime(NOTIFICATION_AUTO_DISMISS_MS);

        expect(service.notification()).toEqual({type: 'error', message: 'An error'});
    });
});
