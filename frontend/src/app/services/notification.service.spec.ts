import {TestBed} from '@angular/core/testing';
import {NotificationService} from './notification.service';

describe('NotificationService', () => {
    let service: NotificationService;

    beforeEach(() => {
        TestBed.configureTestingModule({});
        service = TestBed.inject(NotificationService);
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
});
