import {formatNumber} from '@angular/common';
import {inject, LOCALE_ID, Pipe, PipeTransform} from '@angular/core';
import {CurrencyService} from '../services/currency.service';

@Pipe({
    name: 'currencyAmount',
    standalone: true,
    pure: false
})
export class CurrencyAmountPipe implements PipeTransform {
    private readonly currencyService = inject(CurrencyService);
    private readonly locale = inject(LOCALE_ID);

    transform(value: string | number | null, currencyCode: string): string | null {
        if (value == null) return null;
        const numValue = typeof value === 'string' ? parseFloat(value) : value;
        if (isNaN(numValue)) return null;
        const format = this.currencyService.getAmountFormat(currencyCode);
        return formatNumber(numValue, this.locale, format);
    }
}
