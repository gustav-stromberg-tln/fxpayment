import {FormControl} from '@angular/forms';
import {ibanValidator} from './iban.validator';

describe('ibanValidator', () => {
    const validator = ibanValidator();

    it('should return null for empty string (no validation on empty)', () => {
        const control = new FormControl('');
        const result = validator(control);
        expect(result).toBeNull();
    });

    it('should return null for null value', () => {
        const control = new FormControl(null);
        const result = validator(control);
        expect(result).toBeNull();
    });

    it('should return null for a valid German IBAN', () => {
        const control = new FormControl('DE89370400440532013000');
        const result = validator(control);
        expect(result).toBeNull();
    });

    it('should return null for a valid Estonian IBAN', () => {
        const control = new FormControl('EE382200221020145685');
        const result = validator(control);
        expect(result).toBeNull();
    });

    it('should return null for a valid Finnish IBAN', () => {
        const control = new FormControl('FI2112345600000785');
        const result = validator(control);
        expect(result).toBeNull();
    });

    it('should return null for a valid lowercase IBAN', () => {
        const control = new FormControl('de89370400440532013000');
        const result = validator(control);
        expect(result).toBeNull();
    });

    it('should return null for a valid IBAN with spaces', () => {
        const control = new FormControl('DE89 3704 0044 0532 0130 00');
        const result = validator(control);
        expect(result).toBeNull();
    });

    it('should return iban error for value too short', () => {
        const control = new FormControl('DE893704');
        const result = validator(control);
        expect(result).toEqual({iban: true});
    });

    it('should return iban error for value with special characters', () => {
        const control = new FormControl('DE89!3704@0044#0532');
        const result = validator(control);
        expect(result).toEqual({iban: true});
    });

    it('should return iban error when country code is missing', () => {
        const control = new FormControl('1289370400440532013000');
        const result = validator(control);
        expect(result).toEqual({iban: true});
    });

    it('should return iban error when check digits are invalid', () => {
        const control = new FormControl('DE00370400440532013000');
        const result = validator(control);
        expect(result).toEqual({iban: true});
    });

    it('should return iban error for invalid country code', () => {
        const control = new FormControl('XX89370400440532013000');
        const result = validator(control);
        expect(result).toEqual({iban: true});
    });

    it('should return iban error for completely invalid input', () => {
        const control = new FormControl('INVALID_IBAN');
        const result = validator(control);
        expect(result).toEqual({iban: true});
    });
});
