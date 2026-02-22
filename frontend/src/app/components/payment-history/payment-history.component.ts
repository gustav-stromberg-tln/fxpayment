import {ChangeDetectionStrategy, Component, computed, inject, signal} from '@angular/core';
import {DatePipe} from '@angular/common';
import {toObservable, takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {Subject, switchMap, catchError, EMPTY, tap, merge, map} from 'rxjs';
import {PaymentService} from '../../services/payment.service';
import {PaymentResponse} from '../../models/payment.model';
import {CurrencyAmountPipe} from '../../pipes/currency-amount.pipe';
import {DEFAULT_PAGE_SIZE, MAX_VISIBLE_PAGES} from '../../app.constants';

@Component({
    selector: 'app-payment-history',
    standalone: true,
    imports: [DatePipe, CurrencyAmountPipe],
    templateUrl: './payment-history.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class PaymentHistoryComponent {
    private readonly paymentService = inject(PaymentService);
    private readonly retryTrigger = new Subject<number>();

    readonly pageSize = DEFAULT_PAGE_SIZE;

    readonly payments = signal<PaymentResponse[]>([]);
    readonly loading = signal(true);
    readonly loadError = signal(false);
    readonly currentPage = signal(0);
    readonly totalPages = signal(0);
    readonly totalElements = signal(0);

    readonly isFirstPage = computed(() => this.currentPage() === 0);
    readonly isLastPage = computed(() => this.currentPage() >= this.totalPages() - 1);

    readonly visiblePages = computed(() => {
        const maxVisible = MAX_VISIBLE_PAGES;
        const total = this.totalPages();
        const current = this.currentPage();
        let start = Math.max(0, current - Math.floor(maxVisible / 2));
        const end = Math.min(total, start + maxVisible);
        start = Math.max(0, end - maxVisible);
        const pages: number[] = [];
        for (let i = start; i < end; i++) {
            pages.push(i);
        }
        return pages;
    });

    readonly showingFrom = computed(() => this.currentPage() * this.pageSize + 1);
    readonly showingTo = computed(() => Math.min((this.currentPage() + 1) * this.pageSize, this.totalElements()));

    constructor() {
        const page$ = toObservable(this.currentPage);

        const paymentCreated$ = this.paymentService.paymentCreated$.pipe(
            tap(() => this.currentPage.set(0)),
            map(() => 0)
        );

        merge(page$, paymentCreated$, this.retryTrigger)
            .pipe(
                tap(() => {
                    this.loading.set(true);
                    this.loadError.set(false);
                }),
                switchMap(page =>
                    this.paymentService.getPayments(page, this.pageSize).pipe(
                        catchError(() => {
                            this.loadError.set(true);
                            this.loading.set(false);
                            return EMPTY;
                        })
                    )
                ),
                takeUntilDestroyed()
            )
            .subscribe((data) => {
                this.payments.set(data.content);
                this.totalPages.set(data.page.totalPages);
                this.totalElements.set(data.page.totalElements);
                this.loadError.set(false);
                this.loading.set(false);
            });
    }

    goToPage(page: number): void {
        if (page >= 0 && page < this.totalPages() && page !== this.currentPage()) {
            this.currentPage.set(page);
        }
    }

    nextPage(): void {
        this.goToPage(this.currentPage() + 1);
    }

    previousPage(): void {
        this.goToPage(this.currentPage() - 1);
    }

    retryLoad(): void {
        this.retryTrigger.next(this.currentPage());
    }

}
