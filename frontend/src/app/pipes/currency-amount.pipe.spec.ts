import {TestBed} from '@angular/core/testing';
import {LOCALE_ID} from '@angular/core';
import {vi} from 'vitest';
import {CurrencyAmountPipe} from './currency-amount.pipe';
import {CurrencyService} from '../services/currency.service';

describe('CurrencyAmountPipe', () => {
    let pipe: CurrencyAmountPipe;
    const getAmountFormat = vi.fn();

    beforeEach(() => {
        getAmountFormat.mockReturnValue('1.2-2');

        TestBed.configureTestingModule({
            providers: [
                CurrencyAmountPipe,
                {provide: CurrencyService, useValue: {getAmountFormat}},
                {provide: LOCALE_ID, useValue: 'en-US'}
            ]
        });

        pipe = TestBed.inject(CurrencyAmountPipe);
    });

    it('should create', () => {
        expect(pipe).toBeTruthy();
    });

    it('should return null for null value', () => {
        const result = pipe.transform(null, 'USD');
        expect(result).toBeNull();
    });

    it('should format a numeric value with 2 decimal places', () => {
        getAmountFormat.mockReturnValue('1.2-2');

        const result = pipe.transform(1234.5, 'USD');

        expect(result).toBe('1,234.50');
        expect(getAmountFormat).toHaveBeenCalledWith('USD');
    });

    it('should format a string numeric value', () => {
        getAmountFormat.mockReturnValue('1.2-2');

        const result = pipe.transform('1234.5', 'EUR');

        expect(result).toBe('1,234.50');
    });

    it('should return null for a non-numeric string', () => {
        const result = pipe.transform('abc', 'USD');
        expect(result).toBeNull();
    });

    it('should use currency-specific format for zero-decimal currencies', () => {
        getAmountFormat.mockReturnValue('1.0-0');

        const result = pipe.transform(1234, 'JPY');

        expect(result).toBe('1,234');
        expect(getAmountFormat).toHaveBeenCalledWith('JPY');
    });

    it('should format zero correctly', () => {
        getAmountFormat.mockReturnValue('1.2-2');

        const result = pipe.transform(0, 'USD');

        expect(result).toBe('0.00');
    });
});
