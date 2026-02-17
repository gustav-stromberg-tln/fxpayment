import {computed, inject, Injectable, signal} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {catchError, Observable, of, retry, finalize, Subject, switchMap} from 'rxjs';
import {CurrencyResponse} from '../models/payment.model';
import {NotificationService} from './notification.service';

@Injectable({
    providedIn: 'root'
})
export class CurrencyService {
    private readonly http = inject(HttpClient);
    private readonly notificationService = inject(NotificationService);

    readonly currencies = signal<CurrencyResponse[]>([]);
    readonly currenciesLoading = signal(true);
    readonly currenciesError = signal(false);

    private readonly loadTrigger = new Subject<void>();

    readonly currencyMap = computed(() => {
        const map = new Map<string, CurrencyResponse>();
        for (const c of this.currencies()) {
            map.set(c.code, c);
        }
        return map;
    });

    constructor() {
        this.loadTrigger.pipe(
            switchMap(() => this.fetchCurrencies())
        ).subscribe(data => this.currencies.set(data));

        this.loadTrigger.next();
    }

    // Fetches currencies from the API with retry logic, updating loading and error states accordingly.
    private fetchCurrencies(): Observable<CurrencyResponse[]> {
        this.currenciesLoading.set(true);
        this.currenciesError.set(false);

        return this.http.get<CurrencyResponse[]>('/api/v1/currencies').pipe(
            retry({count: 2, delay: 1000}),
            catchError(() => {
                this.currenciesError.set(true);
                this.notificationService.showError('Failed to load currencies. Please refresh the page.');
                return of<CurrencyResponse[]>([]);
            }),
            finalize(() => this.currenciesLoading.set(false))
        );
    }

    reloadCurrencies(): void {
        this.loadTrigger.next();
    }

    getAmountFormat(currencyCode: string): string {
        const decimals = this.currencyMap().get(currencyCode)?.decimals ?? 2;
        return `1.${decimals}-${decimals}`;
    }
}
