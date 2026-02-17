import {inject, Injectable} from '@angular/core';
import {HttpClient, HttpHeaders, HttpParams} from '@angular/common/http';
import {Observable, Subject} from 'rxjs';
import {PaymentRequest, PaymentResponse, PagedResponse} from '../models/payment.model';
import {DEFAULT_PAGE_SIZE} from '../app.constants';

@Injectable({
    providedIn: 'root'
})
export class PaymentService {
    private readonly http = inject(HttpClient);

    private readonly apiUrl = '/api/v1/payments';

    private readonly paymentCreatedSubject = new Subject<void>();
    readonly paymentCreated$ = this.paymentCreatedSubject.asObservable();

    createPayment(payment: PaymentRequest, idempotencyKey: string): Observable<PaymentResponse> {
        const headers = new HttpHeaders({
            'Idempotency-Key': idempotencyKey
        });
        return this.http.post<PaymentResponse>(this.apiUrl, payment, {headers});
    }

    getPayments(page: number = 0, size: number = DEFAULT_PAGE_SIZE): Observable<PagedResponse<PaymentResponse>> {
        const params = new HttpParams()
            .set('page', page)
            .set('size', size);
        return this.http.get<PagedResponse<PaymentResponse>>(this.apiUrl, {params});
    }

    notifyPaymentCreated(): void {
        this.paymentCreatedSubject.next();
    }
}
