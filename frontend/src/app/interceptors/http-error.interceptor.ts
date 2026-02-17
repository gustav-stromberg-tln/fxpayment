import {HttpInterceptorFn, HttpErrorResponse} from '@angular/common/http';
import {inject} from '@angular/core';
import {catchError, throwError} from 'rxjs';
import {NotificationService} from '../services/notification.service';
import {ApiErrorResponse} from '../models/payment.model';

function isApiErrorResponse(body: unknown): body is ApiErrorResponse {
    return (
        typeof body === 'object' &&
        body !== null &&
        'status' in body &&
        'errors' in body &&
        Array.isArray((body as ApiErrorResponse).errors)
    );
}

function formatApiErrors(apiError: ApiErrorResponse): string {
    return apiError.errors.join('; ');
}

export const httpErrorInterceptor: HttpInterceptorFn = (req, next) => {
    const notificationService = inject(NotificationService);

    return next(req).pipe(
        catchError((error: HttpErrorResponse) => {
            const apiError = isApiErrorResponse(error.error) ? error.error : null;
            let message = '';

            switch (true) {
                case error.status === 0:
                    message = 'Unable to reach the server. Please check your connection.';
                    break;

                case error.status === 409:
                    message = apiError ? formatApiErrors(apiError) : 'A conflict occurred.';
                    break;

                case error.status >= 400 && error.status < 500:
                    message = apiError ? formatApiErrors(apiError) : 'Invalid request. Please check your input.';
                    break;

                case error.status >= 500:
                    message = 'A server error occurred. Please try again later.';
                    break;
            }

            if (message) notificationService.showError(message);

            return throwError(() => error);
        })
    );
};
