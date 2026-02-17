import {TestBed} from '@angular/core/testing';
import {provideHttpClient} from '@angular/common/http';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import {vi} from 'vitest';
import {CurrencyService} from './currency.service';
import {NotificationService} from './notification.service';
import {CurrencyResponse} from '../models/payment.model';

describe('CurrencyService', () => {
    const CURRENCIES_URL = '/api/v1/currencies';

    let service: CurrencyService;
    let httpMock: HttpTestingController;
    let notificationService: NotificationService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                provideHttpClient(),
                provideHttpClientTesting()
            ]
        });
        httpMock = TestBed.inject(HttpTestingController);
        notificationService = TestBed.inject(NotificationService);
        service = TestBed.inject(CurrencyService);
    });

    afterEach(() => {
        httpMock.verify();
    });

    it('should be created', () => {
        httpMock.expectOne(CURRENCIES_URL).flush([]);
        expect(service).toBeTruthy();
    });

    it('should have empty currencies as initial value before API responds', () => {
        expect(service.currencies()).toEqual([]);
        httpMock.expectOne(CURRENCIES_URL).flush([]);
    });

    it('should have currenciesLoading true before API responds', () => {
        expect(service.currenciesLoading()).toBe(true);
        httpMock.expectOne(CURRENCIES_URL).flush([]);
    });

    it('should set currenciesLoading to false after API responds', () => {
        httpMock.expectOne(CURRENCIES_URL).flush([]);
        expect(service.currenciesLoading()).toBe(false);
    });

    it('should have currenciesError false initially', () => {
        expect(service.currenciesError()).toBe(false);
        httpMock.expectOne(CURRENCIES_URL).flush([]);
    });

    it('should load currencies from the API', () => {
        const mockCurrencies: CurrencyResponse[] = [
            {code: 'USD', name: 'US Dollar', decimals: 2},
            {code: 'JPY', name: 'Japanese Yen', decimals: 0}
        ];

        httpMock.expectOne(CURRENCIES_URL).flush(mockCurrencies);
        expect(service.currencies()).toEqual(mockCurrencies);
    });

    it('should build currencyMap from loaded currencies', () => {
        const mockCurrencies: CurrencyResponse[] = [
            {code: 'USD', name: 'US Dollar', decimals: 2},
            {code: 'EUR', name: 'Euro', decimals: 2}
        ];

        httpMock.expectOne(CURRENCIES_URL).flush(mockCurrencies);

        const map = service.currencyMap();
        expect(map.size).toBe(2);
        expect(map.get('USD')).toEqual(mockCurrencies[0]);
        expect(map.get('EUR')).toEqual(mockCurrencies[1]);
    });

    it('should return correct amount format for a currency with 0 decimals', () => {
        const mockCurrencies: CurrencyResponse[] = [
            {code: 'JPY', name: 'Japanese Yen', decimals: 0}
        ];
        httpMock.expectOne(CURRENCIES_URL).flush(mockCurrencies);

        const format = service.getAmountFormat('JPY');
        expect(format).toBe('1.0-0');
    });

    it('should return correct amount format for a currency with 2 decimals', () => {
        const mockCurrencies: CurrencyResponse[] = [
            {code: 'USD', name: 'US Dollar', decimals: 2}
        ];
        httpMock.expectOne(CURRENCIES_URL).flush(mockCurrencies);

        const format = service.getAmountFormat('USD');
        expect(format).toBe('1.2-2');
    });

    it('should return default format (2 decimals) for unknown currency', () => {
        httpMock.expectOne(CURRENCIES_URL).flush([]);

        const format = service.getAmountFormat('UNKNOWN');
        expect(format).toBe('1.2-2');
    });

    it('should show error notification when API call fails after retries', () => {
        vi.useFakeTimers();

        httpMock.expectOne(CURRENCIES_URL).error(new ProgressEvent('error'));

        vi.advanceTimersByTime(1000);
        httpMock.expectOne(CURRENCIES_URL).error(new ProgressEvent('error'));

        vi.advanceTimersByTime(1000);
        httpMock.expectOne(CURRENCIES_URL).error(new ProgressEvent('error'));

        expect(notificationService.notification()?.type).toBe('error');
        expect(notificationService.notification()?.message).toContain('Failed to load currencies');

        vi.useRealTimers();
    });

    it('should set currenciesError to true when API call fails after retries', () => {
        vi.useFakeTimers();

        httpMock.expectOne(CURRENCIES_URL).error(new ProgressEvent('error'));

        vi.advanceTimersByTime(1000);
        httpMock.expectOne(CURRENCIES_URL).error(new ProgressEvent('error'));

        vi.advanceTimersByTime(1000);
        httpMock.expectOne(CURRENCIES_URL).error(new ProgressEvent('error'));

        expect(service.currenciesError()).toBe(true);
        expect(service.currenciesLoading()).toBe(false);

        vi.useRealTimers();
    });

    it('should retry and succeed on second attempt', () => {
        vi.useFakeTimers();

        const mockCurrencies: CurrencyResponse[] = [
            {code: 'USD', name: 'US Dollar', decimals: 2}
        ];

        httpMock.expectOne(CURRENCIES_URL).error(new ProgressEvent('error'));

        vi.advanceTimersByTime(1000);
        httpMock.expectOne(CURRENCIES_URL).flush(mockCurrencies);

        expect(service.currencies()).toEqual(mockCurrencies);
        expect(service.currenciesError()).toBe(false);
        expect(service.currenciesLoading()).toBe(false);

        vi.useRealTimers();
    });

    it('should keep currenciesLoading true between retry attempts', () => {
        vi.useFakeTimers();

        httpMock.expectOne('/api/v1/currencies').error(new ProgressEvent('error'));

        expect(service.currenciesLoading()).toBe(true);

        vi.advanceTimersByTime(1000);

        expect(service.currenciesLoading()).toBe(true);

        httpMock.expectOne('/api/v1/currencies').error(new ProgressEvent('error'));

        vi.advanceTimersByTime(1000);

        expect(service.currenciesLoading()).toBe(true);

        httpMock.expectOne('/api/v1/currencies').error(new ProgressEvent('error'));

        expect(service.currenciesLoading()).toBe(false);
        expect(service.currenciesError()).toBe(true);

        vi.useRealTimers();
    });

    it('should reload currencies when reloadCurrencies is called', () => {
        vi.useFakeTimers();

        // Initial load fails after retries
        httpMock.expectOne(CURRENCIES_URL).error(new ProgressEvent('error'));
        vi.advanceTimersByTime(1000);
        httpMock.expectOne(CURRENCIES_URL).error(new ProgressEvent('error'));
        vi.advanceTimersByTime(1000);
        httpMock.expectOne(CURRENCIES_URL).error(new ProgressEvent('error'));

        expect(service.currenciesError()).toBe(true);

        // Manual retry succeeds
        const mockCurrencies: CurrencyResponse[] = [
            {code: 'USD', name: 'US Dollar', decimals: 2}
        ];
        service.reloadCurrencies();
        httpMock.expectOne(CURRENCIES_URL).flush(mockCurrencies);

        expect(service.currencies()).toEqual(mockCurrencies);
        expect(service.currenciesError()).toBe(false);
        expect(service.currenciesLoading()).toBe(false);

        vi.useRealTimers();
    });

    it('should cancel in-flight request when reloadCurrencies is called', () => {
        service.reloadCurrencies();

        const requests = httpMock.match(CURRENCIES_URL);
        expect(requests.length).toBe(2);
        expect(requests[0].cancelled).toBe(true);

        const mockCurrencies: CurrencyResponse[] = [
            {code: 'EUR', name: 'Euro', decimals: 2}
        ];
        requests[1].flush(mockCurrencies);

        expect(service.currencies()).toEqual(mockCurrencies);
        expect(service.currenciesLoading()).toBe(false);
        expect(service.currenciesError()).toBe(false);
    });
});
