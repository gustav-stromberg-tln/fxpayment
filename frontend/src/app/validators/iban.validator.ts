import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { electronicFormatIBAN, isValidIBAN } from 'ibantools';

export function ibanValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) {
      return null;
    }
    const iban = electronicFormatIBAN(control.value.toUpperCase()) ?? '';
    return isValidIBAN(iban) ? null : { iban: true };
  };
}
