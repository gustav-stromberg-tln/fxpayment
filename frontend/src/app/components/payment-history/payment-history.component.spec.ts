import {ComponentFixture, TestBed} from '@angular/core/testing';
import {signal} from '@angular/core';
import {vi, type Mock} from 'vitest';
import {of, Subject, throwError} from 'rxjs';
import {PaymentHistoryComponent} from './payment-history.component';
import {PaymentService} from '../../services/payment.service';
import {CurrencyService} from '../../services/currency.service';
import {CurrencyResponse, PagedResponse, PaymentResponse} from '../../models/payment.model';
import {DEFAULT_PAGE_SIZE} from '../../app.constants';

describe('PaymentHistoryComponent', () => {
    let component: PaymentHistoryComponent;
    let fixture: ComponentFixture<PaymentHistoryComponent>;
    let paymentServiceSpy: {getPayments: Mock; notifyPaymentCreated: Mock; paymentCreated$: Subject<void>};

    const emptyPage: PagedResponse<PaymentResponse> = {
        content: [],
        page: {totalElements: 0, totalPages: 0, size: DEFAULT_PAGE_SIZE, number: 0}
    };

    beforeEach(async () => {
        paymentServiceSpy = {
            getPayments: vi.fn().mockReturnValue(of(emptyPage)),
            notifyPaymentCreated: vi.fn(),
            paymentCreated$: new Subject<void>()
        };

        const currencyServiceSpy = {
            getAmountFormat: vi.fn().mockReturnValue('1.2-2'),
            currencies: signal<CurrencyResponse[]>([]),
            currencyMap: signal(new Map<string, CurrencyResponse>())
        };

        await TestBed.configureTestingModule({
            imports: [PaymentHistoryComponent],
            providers: [
                {provide: PaymentService, useValue: paymentServiceSpy},
                {provide: CurrencyService, useValue: currencyServiceSpy}
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(PaymentHistoryComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should have empty payments array initially', () => {
        expect(component.payments()).toEqual([]);
    });

    it('should have loading set to false after initial data load', async () => {
        await fixture.whenStable();
        expect(component.loading()).toBe(false);
    });

    it('should have loadError set to false initially', () => {
        expect(component.loadError()).toBe(false);
    });

    it('should have currentPage set to 0 initially', () => {
        expect(component.currentPage()).toBe(0);
    });

    it('should have pageSize set from constants', () => {
        expect(component.pageSize).toBe(DEFAULT_PAGE_SIZE);
    });

    it('should compute isFirstPage as true when currentPage is 0', () => {
        expect(component.isFirstPage()).toBe(true);
    });

    it('should compute isLastPage as true when totalPages is 0', () => {
        expect(component.isLastPage()).toBe(true);
    });

    it('should load payments after initialisation', async () => {
        await fixture.whenStable();

        expect(component.loading()).toBe(false);
        expect(component.payments()).toEqual([]);
        expect(paymentServiceSpy.getPayments).toHaveBeenCalledWith(0, DEFAULT_PAGE_SIZE);
    });

    it('should render the card title "Payment History"', () => {
        const compiled = fixture.nativeElement as HTMLElement;
        const title = compiled.querySelector('.card-title');

        expect(title).toBeTruthy();
        expect(title?.textContent).toContain('Payment History');
    });

    it('should show loading spinner when loading with no payments', () => {
        component.loading.set(true);
        fixture.detectChanges();
        const compiled = fixture.nativeElement as HTMLElement;

        expect(compiled.textContent).toContain('Loading payments...');
    });

    it('should show "No payments yet" when not loading and payments are empty', async () => {
        await fixture.whenStable();
        fixture.detectChanges();
        const compiled = fixture.nativeElement as HTMLElement;

        expect(compiled.textContent).toContain('No payments yet.');
    });

    it('should render error alert with retry button when loadError is true', () => {
        component.loadError.set(true);
        component.loading.set(false);
        fixture.detectChanges();

        const compiled = fixture.nativeElement as HTMLElement;
        expect(compiled.querySelector('.alert-danger')).toBeTruthy();
        expect(compiled.textContent).toContain('Failed to load payments');
        const retryButton = compiled.querySelector('.alert-danger .btn-outline-danger');
        expect(retryButton).toBeTruthy();
        expect(retryButton?.textContent).toContain('Retry');
    });

    it('should reload payments when retry button is clicked', async () => {
        await fixture.whenStable();
        paymentServiceSpy.getPayments.mockReturnValue(throwError(() => new Error('Network error')));

        component.totalPages.set(3);
        component.nextPage();
        fixture.detectChanges();
        await fixture.whenStable();

        expect(component.loadError()).toBe(true);
        paymentServiceSpy.getPayments.mockClear();
        paymentServiceSpy.getPayments.mockReturnValue(of(emptyPage));

        component.retryLoad();
        await fixture.whenStable();

        expect(paymentServiceSpy.getPayments).toHaveBeenCalledWith(component.currentPage(), DEFAULT_PAGE_SIZE);
        expect(component.loadError()).toBe(false);
    });

    it('should display payment table when payments are loaded via data pipeline', async () => {
        const pagedResponse: PagedResponse<PaymentResponse> = {
            content: [{
                id: '1', amount: '100.00', currency: 'USD',
                recipient: 'John Doe', processingFee: '1.50', createdAt: '2024-01-15T10:30:00Z'
            }],
            page: {totalElements: 1, totalPages: 1, size: DEFAULT_PAGE_SIZE, number: 0}
        };
        paymentServiceSpy.getPayments.mockReturnValue(of(pagedResponse));

        paymentServiceSpy.paymentCreated$.next();
        await fixture.whenStable();
        fixture.detectChanges();

        expect(component.payments()).toEqual(pagedResponse.content);
        expect(component.totalPages()).toBe(1);
        expect(component.loading()).toBe(false);

        const compiled = fixture.nativeElement as HTMLElement;
        const table = compiled.querySelector('table');
        expect(table).toBeTruthy();
        const rows = compiled.querySelectorAll('tbody tr');
        expect(rows.length).toBe(1);
        expect(rows[0].textContent).toContain('John Doe');
    });

    it('should render table headers correctly', () => {
        component.payments.set([{
            id: '1', amount: '100.00', currency: 'USD',
            recipient: 'Test', processingFee: '1.00', createdAt: '2024-01-01T00:00:00Z'
        }]);
        component.loading.set(false);

        fixture.detectChanges();
        const compiled = fixture.nativeElement as HTMLElement;
        const headers = compiled.querySelectorAll('thead th');

        expect(headers.length).toBe(5);
        expect(headers[0].textContent).toContain('Timestamp');
        expect(headers[1].textContent).toContain('Recipient');
        expect(headers[2].textContent).toContain('Amount');
        expect(headers[3].textContent).toContain('Currency');
        expect(headers[4].textContent).toContain('Applied Fee');
    });

    describe('pagination', () => {
        it('should not navigate to negative page', () => {
            component.currentPage.set(0);
            component.previousPage();
            expect(component.currentPage()).toBe(0);
        });

        it('should not navigate past last page', () => {
            component.totalPages.set(3);
            component.currentPage.set(2);
            component.nextPage();
            expect(component.currentPage()).toBe(2);
        });

        it('should navigate to next page', () => {
            component.totalPages.set(5);
            component.currentPage.set(0);
            component.nextPage();
            expect(component.currentPage()).toBe(1);
        });

        it('should navigate to previous page', () => {
            component.totalPages.set(5);
            component.currentPage.set(2);
            component.previousPage();
            expect(component.currentPage()).toBe(1);
        });

        it('should navigate to a specific page via goToPage', () => {
            component.totalPages.set(5);
            component.goToPage(3);
            expect(component.currentPage()).toBe(3);
        });

        it('should not navigate to same page', () => {
            component.totalPages.set(5);
            component.currentPage.set(2);
            paymentServiceSpy.getPayments.mockClear();

            component.goToPage(2);

            expect(component.currentPage()).toBe(2);
        });

        it('should compute visiblePages correctly', () => {
            component.totalPages.set(10);
            component.currentPage.set(5);

            const pages = component.visiblePages();

            expect(pages.length).toBe(5);
            expect(pages).toEqual([3, 4, 5, 6, 7]);
        });

        it('should compute showingFrom and showingTo correctly', () => {
            component.currentPage.set(1);
            component.totalElements.set(50);

            expect(component.showingFrom()).toBe(DEFAULT_PAGE_SIZE + 1);
            expect(component.showingTo()).toBe(DEFAULT_PAGE_SIZE * 2);
        });

        it('should cap showingTo at totalElements on the last page', () => {
            component.currentPage.set(2);
            component.totalElements.set(45);

            expect(component.showingTo()).toBe(45);
        });

        it('should compute isFirstPage as false when not on first page', () => {
            component.currentPage.set(1);
            expect(component.isFirstPage()).toBe(false);
        });

        it('should compute isLastPage as false when more pages exist', () => {
            component.totalPages.set(5);
            component.currentPage.set(2);
            expect(component.isLastPage()).toBe(false);
        });

        it('should reset currentPage to 0 and reload when paymentCreated$ emits', async () => {
            await fixture.whenStable();
            component.totalPages.set(5);
            component.currentPage.set(3);
            await fixture.whenStable();
            paymentServiceSpy.getPayments.mockClear();

            paymentServiceSpy.paymentCreated$.next();
            await fixture.whenStable();

            expect(component.currentPage()).toBe(0);
            expect(paymentServiceSpy.getPayments).toHaveBeenCalledWith(0, DEFAULT_PAGE_SIZE);
        });

        it('should fetch data for the correct page when navigating', async () => {
            await fixture.whenStable();
            paymentServiceSpy.getPayments.mockClear();

            component.totalPages.set(5);
            component.nextPage();
            fixture.detectChanges();
            await fixture.whenStable();

            expect(paymentServiceSpy.getPayments).toHaveBeenCalledWith(1, DEFAULT_PAGE_SIZE);
        });

        it('should set loading to true while fetching page data', async () => {
            await fixture.whenStable();
            const responseSubject = new Subject<PagedResponse<PaymentResponse>>();
            paymentServiceSpy.getPayments.mockReturnValue(responseSubject.asObservable());

            component.totalPages.set(3);
            component.nextPage();
            fixture.detectChanges();
            await fixture.whenStable();

            expect(component.loading()).toBe(true);

            responseSubject.next({
                content: [],
                page: {totalElements: 0, totalPages: 3, size: DEFAULT_PAGE_SIZE, number: 1}
            });
            responseSubject.complete();

            expect(component.loading()).toBe(false);
        });

        it('should set loadError on fetch failure during page navigation', async () => {
            await fixture.whenStable();
            paymentServiceSpy.getPayments.mockReturnValue(throwError(() => new Error('Network error')));

            component.totalPages.set(3);
            component.nextPage();
            fixture.detectChanges();
            await fixture.whenStable();

            expect(component.loadError()).toBe(true);
            expect(component.loading()).toBe(false);
        });
    });
});
