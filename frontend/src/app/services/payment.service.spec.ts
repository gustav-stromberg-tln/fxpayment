import {TestBed} from '@angular/core/testing';
import {provideHttpClient} from '@angular/common/http';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import {PaymentService} from './payment.service';
import {PaymentRequest, PaymentResponse, PagedResponse} from '../models/payment.model';

describe('PaymentService', () => {
    const PAYMENTS_URL = '/api/v1/payments';
    const IDEMPOTENCY_KEY = 'Idempotency-Key';

    let service: PaymentService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                provideHttpClient(),
                provideHttpClientTesting()
            ]
        });
        service = TestBed.inject(PaymentService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpMock.verify();
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    it('should expose paymentCreated$ observable', () => {
        expect(service.paymentCreated$).toBeTruthy();
    });

    it('should send POST request with the provided Idempotency-Key header when creating a payment', () => {
        const mockRequest: PaymentRequest = {
            amount: 100,
            currency: 'USD',
            recipient: 'John Doe',
            recipientAccount: 'DE89370400440532013000'
        };
        const mockResponse: PaymentResponse = {
            id: '1',
            amount: '100.00',
            currency: 'USD',
            recipient: 'John Doe',
            processingFee: '1.50',
            createdAt: '2024-01-01T00:00:00Z'
        };
        const idempotencyKey = crypto.randomUUID();

        service.createPayment(mockRequest, idempotencyKey).subscribe(response => {
            expect(response).toEqual(mockResponse);
        });

        const req = httpMock.expectOne(PAYMENTS_URL);
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual(mockRequest);
        expect(req.request.headers.get(IDEMPOTENCY_KEY)).toBe(idempotencyKey);
        req.flush(mockResponse);
    });

    it('should send the same Idempotency-Key for repeated calls with the same key', () => {
        const mockRequest: PaymentRequest = {
            amount: 100,
            currency: 'USD',
            recipient: 'John Doe',
            recipientAccount: 'DE89370400440532013000'
        };
        const idempotencyKey = crypto.randomUUID();

        service.createPayment(mockRequest, idempotencyKey).subscribe();
        service.createPayment(mockRequest, idempotencyKey).subscribe();

        const requests = httpMock.match(PAYMENTS_URL);
        expect(requests.length).toBe(2);
        expect(requests[0].request.headers.get(IDEMPOTENCY_KEY)).toBe(idempotencyKey);
        expect(requests[1].request.headers.get(IDEMPOTENCY_KEY)).toBe(idempotencyKey);
        requests.forEach(req => req.flush({id: '1', amount: '100.00', currency: 'USD', recipient: 'John Doe', processingFee: '1.50', createdAt: '2024-01-01T00:00:00Z'}));
    });

    it('should send GET request with pagination params when fetching payments', () => {
        const mockResponse: PagedResponse<PaymentResponse> = {
            content: [],
            page: {totalElements: 0, totalPages: 0, size: 20, number: 0}
        };

        service.getPayments(1, 10).subscribe(response => {
            expect(response).toEqual(mockResponse);
        });

        const req = httpMock.expectOne(r => r.url === PAYMENTS_URL);
        expect(req.request.method).toBe('GET');
        expect(req.request.params.get('page')).toBe('1');
        expect(req.request.params.get('size')).toBe('10');
        req.flush(mockResponse);
    });

    it('should use default page size when fetching payments without size param', () => {
        const mockResponse: PagedResponse<PaymentResponse> = {
            content: [],
            page: {totalElements: 0, totalPages: 0, size: 20, number: 0}
        };

        service.getPayments().subscribe();

        const req = httpMock.expectOne(r => r.url === PAYMENTS_URL);
        expect(req.request.params.get('page')).toBe('0');
        expect(req.request.params.get('size')).toBe('20');
        req.flush(mockResponse);
    });

    it('should emit on paymentCreated$ when notifyPaymentCreated is called', () => {
        const emissions: void[] = [];
        service.paymentCreated$.subscribe(() => emissions.push(undefined));

        service.notifyPaymentCreated();
        expect(emissions.length).toBe(1);

        service.notifyPaymentCreated();
        expect(emissions.length).toBe(2);
    });
});
