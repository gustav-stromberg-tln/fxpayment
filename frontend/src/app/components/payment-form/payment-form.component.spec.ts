import {ComponentFixture, TestBed} from '@angular/core/testing';
import {signal} from '@angular/core';
import {vi, type Mock} from 'vitest';
import {of, Subject, throwError} from 'rxjs';
import {PaymentFormComponent} from './payment-form.component';
import {PaymentService} from '../../services/payment.service';
import {CurrencyService} from '../../services/currency.service';
import {NotificationService} from '../../services/notification.service';
import {CurrencyResponse, PaymentResponse} from '../../models/payment.model';
import {MAX_AMOUNT, MIN_RECIPIENT_LENGTH, MAX_RECIPIENT_LENGTH} from '../../app.constants';

describe('PaymentFormComponent', () => {
    let component: PaymentFormComponent;
    let fixture: ComponentFixture<PaymentFormComponent>;
    let paymentServiceSpy: {createPayment: Mock; notifyPaymentCreated: Mock; paymentCreated$: Subject<void>};
    let currencyServiceSpy: {getAmountFormat: Mock; currencies: ReturnType<typeof signal>; currencyMap: ReturnType<typeof signal>; currenciesLoading: ReturnType<typeof signal>; currenciesError: ReturnType<typeof signal>; reloadCurrencies: Mock};
    let notificationServiceSpy: {showError: Mock; showSuccess: Mock; clear: Mock; notification: ReturnType<typeof signal>};

    beforeEach(async () => {
        paymentServiceSpy = {
            createPayment: vi.fn(),
            notifyPaymentCreated: vi.fn(),
            paymentCreated$: new Subject<void>()
        };

        currencyServiceSpy = {
            getAmountFormat: vi.fn(),
            currencies: signal<CurrencyResponse[]>([]),
            currencyMap: signal(new Map<string, CurrencyResponse>()),
            currenciesLoading: signal(false),
            currenciesError: signal(false),
            reloadCurrencies: vi.fn()
        };

        notificationServiceSpy = {
            showError: vi.fn(),
            showSuccess: vi.fn(),
            clear: vi.fn(),
            notification: signal(null)
        };

        await TestBed.configureTestingModule({
            imports: [PaymentFormComponent],
            providers: [
                {provide: PaymentService, useValue: paymentServiceSpy},
                {provide: CurrencyService, useValue: currencyServiceSpy},
                {provide: NotificationService, useValue: notificationServiceSpy}
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(PaymentFormComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should have submitting signal initialized to false', () => {
        expect(component.submitting()).toBe(false);
    });

    it('should expose maxAmount from constants', () => {
        expect(component.maxAmount).toBe(MAX_AMOUNT);
    });

    it('should expose minRecipientLength from constants', () => {
        expect(component.minRecipientLength).toBe(MIN_RECIPIENT_LENGTH);
    });

    it('should expose maxRecipientLength from constants', () => {
        expect(component.maxRecipientLength).toBe(MAX_RECIPIENT_LENGTH);
    });

    it('should initialize the form with four controls', () => {
        const form = component.paymentForm;

        expect(form.contains('amount')).toBe(true);
        expect(form.contains('currency')).toBe(true);
        expect(form.contains('recipient')).toBe(true);
        expect(form.contains('recipientAccount')).toBe(true);
    });

    it('should have an invalid form initially', () => {
        expect(component.paymentForm.valid).toBe(false);
    });

    it('should require amount', () => {
        const control = component.paymentForm.controls.amount;
        control.setValue(null);
        expect(control.hasError('required')).toBe(true);
    });

    it('should enforce minimum amount', () => {
        const control = component.paymentForm.controls.amount;
        control.setValue(0);
        expect(control.hasError('min')).toBe(true);
    });

    it('should enforce maximum amount', () => {
        const control = component.paymentForm.controls.amount;
        control.setValue(MAX_AMOUNT + 1);
        expect(control.hasError('max')).toBe(true);
    });

    it('should require currency', () => {
        const control = component.paymentForm.controls.currency;
        control.setValue('');
        expect(control.hasError('required')).toBe(true);
    });

    it('should require recipient', () => {
        const control = component.paymentForm.controls.recipient;
        control.setValue('');
        control.markAsTouched();
        expect(control.hasError('required')).toBe(true);
    });

    it('should enforce recipient minimum length', () => {
        const control = component.paymentForm.controls.recipient;
        control.setValue('A');
        control.markAsTouched();
        expect(control.hasError('minlength')).toBe(true);
    });

    it('should enforce recipient maximum length', () => {
        const control = component.paymentForm.controls.recipient;
        control.setValue('A'.repeat(MAX_RECIPIENT_LENGTH + 1));
        control.markAsTouched();
        expect(control.hasError('maxlength')).toBe(true);
    });

    it('should enforce recipient pattern (Latin letters only)', () => {
        const control = component.paymentForm.controls.recipient;
        control.setValue('John123');
        control.markAsTouched();
        expect(control.hasError('pattern')).toBe(true);
    });

    it('should accept valid recipient name', () => {
        const control = component.paymentForm.controls.recipient;
        control.setValue('John Doe');
        expect(control.valid).toBe(true);
    });

    it('should validate recipientAccount with IBAN validator', () => {
        const control = component.paymentForm.controls.recipientAccount;
        control.setValue('INVALID');
        expect(control.hasError('iban')).toBe(true);
    });

    it('should accept valid IBAN for recipientAccount', () => {
        const control = component.paymentForm.controls.recipientAccount;
        control.setValue('DE89370400440532013000');
        expect(control.valid).toBe(true);
    });

    it('should update amount validators when currency changes', () => {
        const cs = TestBed.inject(CurrencyService) as any;
        cs.currencyMap.set(new Map([['JPY', {code: 'JPY', name: 'Japanese Yen', decimals: 0}]]));

        component.paymentForm.controls.currency.setValue('JPY');

        component.paymentForm.controls.amount.setValue(0.5);
        expect(component.paymentForm.controls.amount.hasError('min')).toBe(true);
    });

    it('should return true from showError when control has error and is touched', () => {
        component.paymentForm.controls.amount.setValue(null);
        component.paymentForm.controls.amount.markAsTouched();
        expect(component.showError('amount', 'required')).toBe(true);
    });

    it('should return false from showError when control is not touched', () => {
        component.paymentForm.controls.amount.setValue(null);
        expect(component.showError('amount', 'required')).toBe(false);
    });

    it('should return true from isFieldInvalid when control is invalid and touched', () => {
        component.paymentForm.controls.amount.setValue(null);
        component.paymentForm.controls.amount.markAsTouched();
        expect(component.isFieldInvalid('amount')).toBe(true);
    });

    it('should return false from isFieldInvalid when control is valid', () => {
        component.paymentForm.controls.amount.setValue(50);
        expect(component.isFieldInvalid('amount')).toBe(false);
    });

    it('should mark all fields as touched when submitting an invalid form', () => {
        component.onSubmit();

        expect(component.paymentForm.controls.amount.touched).toBe(true);
        expect(component.paymentForm.controls.currency.touched).toBe(true);
        expect(component.paymentForm.controls.recipient.touched).toBe(true);
        expect(component.paymentForm.controls.recipientAccount.touched).toBe(true);
    });

    it('should not call createPayment when form is invalid', () => {
        component.onSubmit();
        expect(paymentServiceSpy.createPayment).not.toHaveBeenCalled();
    });

    it('should call createPayment with idempotency key and show success on valid submission', () => {
        const mockResponse: PaymentResponse = {
            id: '1', amount: '100.00', currency: 'USD',
            recipient: 'John Doe', processingFee: '1.50', createdAt: '2024-01-01T00:00:00Z'
        };
        paymentServiceSpy.createPayment.mockReturnValue(of(mockResponse));

        component.paymentForm.setValue({
            amount: 100,
            currency: 'USD',
            recipient: 'John Doe',
            recipientAccount: 'DE89370400440532013000'
        });

        component.onSubmit();

        expect(notificationServiceSpy.clear).toHaveBeenCalled();
        expect(paymentServiceSpy.createPayment).toHaveBeenCalledWith(
            {
                amount: 100,
                currency: 'USD',
                recipient: 'John Doe',
                recipientAccount: 'DE89370400440532013000'
            },
            expect.stringMatching(/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i)
        );
        expect(notificationServiceSpy.showSuccess).toHaveBeenCalledWith('Payment submitted successfully.');
        expect(paymentServiceSpy.notifyPaymentCreated).toHaveBeenCalled();
    });

    it('should set submitting to true during submission and false after', () => {
        let submittingDuringCall = false;
        paymentServiceSpy.createPayment.mockImplementation(() => {
            submittingDuringCall = component.submitting();
            return of({
                id: '1', amount: '100.00', currency: 'USD',
                recipient: 'Jane', processingFee: '0.50', createdAt: '2024-01-01T00:00:00Z'
            } as PaymentResponse);
        });

        component.paymentForm.setValue({
            amount: 50,
            currency: 'EUR',
            recipient: 'Jane',
            recipientAccount: 'DE89370400440532013000'
        });

        component.onSubmit();

        expect(submittingDuringCall).toBe(true);
        expect(component.submitting()).toBe(false);
    });

    it('should remain in submitting state until async response completes', () => {
        const responseSubject = new Subject<PaymentResponse>();
        paymentServiceSpy.createPayment.mockReturnValue(responseSubject.asObservable());

        component.paymentForm.setValue({
            amount: 100,
            currency: 'USD',
            recipient: 'John Doe',
            recipientAccount: 'DE89370400440532013000'
        });

        component.onSubmit();

        expect(component.submitting()).toBe(true);
        expect(notificationServiceSpy.showSuccess).not.toHaveBeenCalled();

        responseSubject.next({
            id: '1', amount: '100.00', currency: 'USD',
            recipient: 'John Doe', processingFee: '1.50', createdAt: '2024-01-01T00:00:00Z'
        });
        responseSubject.complete();

        expect(component.submitting()).toBe(false);
        expect(notificationServiceSpy.showSuccess).toHaveBeenCalledWith('Payment submitted successfully.');
    });

    it('should reset the form after successful submission', () => {
        const mockResponse: PaymentResponse = {
            id: '1', amount: '50.00', currency: 'EUR',
            recipient: 'Jane', processingFee: '0.50', createdAt: '2024-01-01T00:00:00Z'
        };
        paymentServiceSpy.createPayment.mockReturnValue(of(mockResponse));

        component.paymentForm.setValue({
            amount: 50,
            currency: 'EUR',
            recipient: 'Jane',
            recipientAccount: 'DE89370400440532013000'
        });

        component.onSubmit();

        expect(component.paymentForm.controls.amount.value).toBeNull();
    });

    it('should reuse the same idempotency key when resubmitting after a failure', () => {
        paymentServiceSpy.createPayment.mockReturnValue(throwError(() => new Error('Network error')));

        component.paymentForm.setValue({
            amount: 100,
            currency: 'USD',
            recipient: 'John Doe',
            recipientAccount: 'DE89370400440532013000'
        });

        component.onSubmit();
        const firstKey = paymentServiceSpy.createPayment.mock.calls[0][1];

        component.onSubmit();
        const secondKey = paymentServiceSpy.createPayment.mock.calls[1][1];

        expect(firstKey).toBe(secondKey);
    });

    it('should generate a new idempotency key after a successful submission', () => {
        const mockResponse: PaymentResponse = {
            id: '1', amount: '100.00', currency: 'USD',
            recipient: 'John Doe', processingFee: '1.50', createdAt: '2024-01-01T00:00:00Z'
        };
        paymentServiceSpy.createPayment.mockReturnValue(of(mockResponse));

        component.paymentForm.setValue({
            amount: 100,
            currency: 'USD',
            recipient: 'John Doe',
            recipientAccount: 'DE89370400440532013000'
        });

        component.onSubmit();
        const firstKey = paymentServiceSpy.createPayment.mock.calls[0][1];

        // Fill the form again for a second submission
        paymentServiceSpy.createPayment.mockReturnValue(of(mockResponse));
        component.paymentForm.setValue({
            amount: 200,
            currency: 'USD',
            recipient: 'Jane Doe',
            recipientAccount: 'DE89370400440532013000'
        });

        component.onSubmit();
        const secondKey = paymentServiceSpy.createPayment.mock.calls[1][1];

        expect(firstKey).not.toBe(secondKey);
    });

    it('should reset submitting and not show success on submission error', () => {
        paymentServiceSpy.createPayment.mockReturnValue(throwError(() => new Error('API error')));

        component.paymentForm.setValue({
            amount: 100,
            currency: 'USD',
            recipient: 'John Doe',
            recipientAccount: 'DE89370400440532013000'
        });

        component.onSubmit();

        expect(component.submitting()).toBe(false);
        expect(notificationServiceSpy.showSuccess).not.toHaveBeenCalled();
        expect(paymentServiceSpy.notifyPaymentCreated).not.toHaveBeenCalled();
    });

    it('should render the card title "New Payment"', () => {
        const compiled = fixture.nativeElement as HTMLElement;
        const title = compiled.querySelector('.card-title');

        expect(title).toBeTruthy();
        expect(title?.textContent).toContain('New Payment');
    });

    it('should render a submit button', () => {
        const compiled = fixture.nativeElement as HTMLElement;
        const button = compiled.querySelector('button[type="submit"]');

        expect(button).toBeTruthy();
        expect(button?.textContent).toContain('Submit Payment');
    });

    it('should disable the submit button when form is invalid', () => {
        const compiled = fixture.nativeElement as HTMLElement;
        const button = compiled.querySelector('button[type="submit"]') as HTMLButtonElement;

        expect(button.disabled).toBe(true);
    });

    it('should render form labels for all fields', () => {
        const compiled = fixture.nativeElement as HTMLElement;
        const labels = compiled.querySelectorAll('label.form-label');

        expect(labels.length).toBe(4);
        expect(labels[0].textContent).toContain('Amount');
        expect(labels[1].textContent).toContain('Currency');
        expect(labels[2].textContent).toContain('Recipient');
        expect(labels[3].textContent).toContain('Recipient Account');
    });

    it('should render the currency select with a disabled placeholder option', () => {
        const compiled = fixture.nativeElement as HTMLElement;
        const select = compiled.querySelector('select#currency') as HTMLSelectElement;
        const placeholderOption = select.querySelector('option[value=""]');

        expect(select).toBeTruthy();
        expect(placeholderOption?.textContent).toContain('Select currency');
    });

    it('should show loading indicator when currencies are loading', () => {
        currencyServiceSpy.currenciesLoading.set(true);
        fixture.detectChanges();

        const compiled = fixture.nativeElement as HTMLElement;
        expect(compiled.textContent).toContain('Loading currencies...');
        expect(compiled.querySelector('select#currency')).toBeFalsy();
    });

    it('should show error state with retry button when currencies fail to load', () => {
        currencyServiceSpy.currenciesError.set(true);
        fixture.detectChanges();

        const compiled = fixture.nativeElement as HTMLElement;
        expect(compiled.textContent).toContain('Failed to load currencies.');
        const retryButton = compiled.querySelector('.btn-outline-danger');
        expect(retryButton).toBeTruthy();
        expect(retryButton?.textContent).toContain('Retry');
    });

    it('should call retryCurrencies when retry button is clicked', () => {
        currencyServiceSpy.currenciesError.set(true);
        fixture.detectChanges();

        const compiled = fixture.nativeElement as HTMLElement;
        const retryButton = compiled.querySelector('.btn-outline-danger') as HTMLButtonElement;
        retryButton.click();

        expect(currencyServiceSpy.reloadCurrencies).toHaveBeenCalled();
    });
});
