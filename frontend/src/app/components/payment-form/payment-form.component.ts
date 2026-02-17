import {ChangeDetectionStrategy, Component, computed, DestroyRef, inject, signal} from '@angular/core';
import {DecimalPipe} from '@angular/common';
import {ReactiveFormsModule, FormControl, FormGroup, ValidatorFn, Validators} from '@angular/forms';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {catchError, EMPTY, finalize} from 'rxjs';
import {PaymentService} from '../../services/payment.service';
import {CurrencyService} from '../../services/currency.service';
import {NotificationService} from '../../services/notification.service';
import {PaymentRequest} from '../../models/payment.model';
import {ibanValidator} from '../../validators/iban.validator';
import {
    DEFAULT_MIN_AMOUNT,
    MAX_AMOUNT,
    MIN_RECIPIENT_LENGTH,
    MAX_RECIPIENT_LENGTH,
    RECIPIENT_NAME_PATTERN
} from '../../app.constants';

interface PaymentFormControls {
    amount: FormControl<number | null>;
    currency: FormControl<string>;
    recipient: FormControl<string>;
    recipientAccount: FormControl<string>;
}

@Component({
    selector: 'app-payment-form',
    standalone: true,
    imports: [DecimalPipe, ReactiveFormsModule],
    templateUrl: './payment-form.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class PaymentFormComponent {
    private readonly paymentService = inject(PaymentService);
    private readonly currencyService = inject(CurrencyService);
    private readonly notificationService = inject(NotificationService);
    private readonly destroyRef = inject(DestroyRef);

    readonly maxAmount = MAX_AMOUNT;
    readonly minRecipientLength = MIN_RECIPIENT_LENGTH;
    readonly maxRecipientLength = MAX_RECIPIENT_LENGTH;

    readonly submitting = signal(false);
    readonly currencies = this.currencyService.currencies;
    readonly currenciesLoading = this.currencyService.currenciesLoading;
    readonly currenciesError = this.currencyService.currenciesError;

    private idempotencyKey = crypto.randomUUID();

    private readonly selectedCurrencyCode = signal('');

    readonly selectedCurrency = computed(() => {
        const code = this.selectedCurrencyCode();
        return this.currencyService.currencyMap().get(code);
    });

    readonly amountStep = computed(() => {
        const decimals = this.selectedCurrency()?.decimals;
        if (decimals == null) return DEFAULT_MIN_AMOUNT;
        return decimals > 0 ? Math.pow(10, -decimals) : 1;
    });

    readonly paymentForm = new FormGroup<PaymentFormControls>({
        amount: new FormControl<number | null>(null, this.buildAmountValidators(DEFAULT_MIN_AMOUNT)),
        currency: new FormControl('', {nonNullable: true, validators: Validators.required}),
        recipient: new FormControl('', {
            nonNullable: true,
            validators: [
                Validators.required,
                Validators.minLength(MIN_RECIPIENT_LENGTH),
                Validators.maxLength(MAX_RECIPIENT_LENGTH),
                Validators.pattern(RECIPIENT_NAME_PATTERN)
            ]
        }),
        recipientAccount: new FormControl('', {
            nonNullable: true,
            validators: [Validators.required, ibanValidator()]
        })
    });

    constructor() {
        this.paymentForm.controls.currency.valueChanges
            .pipe(takeUntilDestroyed())
            .subscribe(code => {
                this.selectedCurrencyCode.set(code);
                const step = this.amountStep();
                this.paymentForm.controls.amount.setValidators(this.buildAmountValidators(step));
                this.paymentForm.controls.amount.updateValueAndValidity();
            });
    }

    showError(controlName: keyof PaymentFormControls, errorName: string): boolean {
        const control = this.paymentForm.controls[controlName];
        return control.hasError(errorName) && control.touched;
    }

    isFieldInvalid(controlName: keyof PaymentFormControls): boolean {
        const control = this.paymentForm.controls[controlName];
        return control.invalid && control.touched;
    }

    onSubmit(): void {
        if (this.paymentForm.invalid) {
            this.paymentForm.markAllAsTouched();
            return;
        }

        this.notificationService.clear();

        const {amount, currency, recipient, recipientAccount} = this.paymentForm.getRawValue();
        if (amount == null) {
            return;
        }

        this.submitting.set(true);

        const request: PaymentRequest = {amount, currency, recipient, recipientAccount};

        this.paymentService.createPayment(request, this.idempotencyKey)
            .pipe(
                takeUntilDestroyed(this.destroyRef),
                finalize(() => this.submitting.set(false)),
                catchError(() => EMPTY)
            )
            .subscribe(() => {
                this.notificationService.showSuccess('Payment submitted successfully.');
                this.paymentForm.reset();
                this.idempotencyKey = crypto.randomUUID();
                this.paymentService.notifyPaymentCreated();
            });
    }

    retryCurrencies(): void {
        this.currencyService.reloadCurrencies();
    }

    private buildAmountValidators(minAmount: number): ValidatorFn[] {
        return [Validators.required, Validators.min(minAmount), Validators.max(MAX_AMOUNT)];
    }
}
