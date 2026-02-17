import {TestBed} from '@angular/core/testing';
import {HttpClient, provideHttpClient, withInterceptors} from '@angular/common/http';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import {httpErrorInterceptor} from './http-error.interceptor';
import {NotificationService} from '../services/notification.service';

describe('httpErrorInterceptor', () => {
    const TEST_URL = '/api/test';

    let httpClient: HttpClient;
    let httpMock: HttpTestingController;
    let notificationService: NotificationService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                provideHttpClient(withInterceptors([httpErrorInterceptor])),
                provideHttpClientTesting()
            ]
        });

        httpClient = TestBed.inject(HttpClient);
        httpMock = TestBed.inject(HttpTestingController);
        notificationService = TestBed.inject(NotificationService);
    });

    afterEach(() => {
        httpMock.verify();
    });

    it('should show connection error message for status 0', () => {
        httpClient.get(TEST_URL).subscribe({error: () => {}});

        httpMock.expectOne(TEST_URL).error(new ProgressEvent('error'), {status: 0, statusText: ''});

        expect(notificationService.notification()).toEqual({
            type: 'error',
            message: 'Unable to reach the server. Please check your connection.'
        });
    });

    it('should show API error message for 409 conflict with API error body', () => {
        httpClient.get(TEST_URL).subscribe({error: () => {}});

        httpMock.expectOne(TEST_URL).flush(
            {timestamp: '2024-01-01T00:00:00Z', status: 409, errors: ['Duplicate payment detected']},
            {status: 409, statusText: 'Conflict'}
        );

        expect(notificationService.notification()).toEqual({
            type: 'error',
            message: 'Duplicate payment detected'
        });
    });

    it('should show generic conflict message for 409 without API error body', () => {
        httpClient.get(TEST_URL).subscribe({error: () => {}});

        httpMock.expectOne(TEST_URL).flush('conflict', {status: 409, statusText: 'Conflict'});

        expect(notificationService.notification()).toEqual({
            type: 'error',
            message: 'A conflict occurred.'
        });
    });

    it('should join multiple API errors for 4xx with API error body', () => {
        httpClient.get(TEST_URL).subscribe({error: () => {}});

        httpMock.expectOne(TEST_URL).flush(
            {timestamp: '2024-01-01T00:00:00Z', status: 400, errors: ['Invalid amount', 'Missing currency']},
            {status: 400, statusText: 'Bad Request'}
        );

        expect(notificationService.notification()).toEqual({
            type: 'error',
            message: 'Invalid amount; Missing currency'
        });
    });

    it('should show generic client error for 4xx without API error body', () => {
        httpClient.get(TEST_URL).subscribe({error: () => {}});

        httpMock.expectOne(TEST_URL).flush('bad request', {status: 422, statusText: 'Unprocessable Entity'});

        expect(notificationService.notification()).toEqual({
            type: 'error',
            message: 'Invalid request. Please check your input.'
        });
    });

    it('should show server error message for 5xx', () => {
        httpClient.get(TEST_URL).subscribe({error: () => {}});

        httpMock.expectOne(TEST_URL).flush('error', {status: 500, statusText: 'Internal Server Error'});

        expect(notificationService.notification()).toEqual({
            type: 'error',
            message: 'A server error occurred. Please try again later.'
        });
    });

    it('should re-throw the error to the subscriber', () => {
        let caughtError: any;
        httpClient.get(TEST_URL).subscribe({
            error: (err) => { caughtError = err; }
        });

        httpMock.expectOne(TEST_URL).flush('error', {status: 503, statusText: 'Service Unavailable'});

        expect(caughtError).toBeTruthy();
        expect(caughtError.status).toBe(503);
    });
});
