import {ErrorHandler} from '@angular/core';
import {TestBed} from '@angular/core/testing';
import {vi} from 'vitest';
import {GlobalErrorHandler} from './global-error.handler';
import {NotificationService} from '../services/notification.service';

describe('GlobalErrorHandler', () => {
    let handler: GlobalErrorHandler;
    let notificationService: NotificationService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                {provide: ErrorHandler, useClass: GlobalErrorHandler}
            ]
        });

        handler = TestBed.inject(ErrorHandler) as GlobalErrorHandler;
        notificationService = TestBed.inject(NotificationService);
    });

    it('should show an error notification', () => {
        const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

        handler.handleError(new Error('test error'));

        expect(notificationService.notification()).toEqual({
            type: 'error',
            message: 'An unexpected error occurred.'
        });
        consoleSpy.mockRestore();
    });

    it('should log the error to console', () => {
        const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
        const error = new Error('test error');

        handler.handleError(error);

        expect(consoleSpy).toHaveBeenCalledWith(error);
        consoleSpy.mockRestore();
    });

    it('should handle non-Error objects', () => {
        const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

        handler.handleError('string error');

        expect(notificationService.notification()).toEqual({
            type: 'error',
            message: 'An unexpected error occurred.'
        });
        expect(consoleSpy).toHaveBeenCalledWith('string error');
        consoleSpy.mockRestore();
    });
});
