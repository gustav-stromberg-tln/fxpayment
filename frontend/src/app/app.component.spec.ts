import {ComponentFixture, TestBed} from '@angular/core/testing';
import {NO_ERRORS_SCHEMA} from '@angular/core';
import {vi} from 'vitest';
import {AppComponent} from './app.component';
import {NotificationService} from './services/notification.service';

describe('AppComponent', () => {
    let component: AppComponent;
    let fixture: ComponentFixture<AppComponent>;
    let notificationService: NotificationService;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [AppComponent]
        })
        .overrideComponent(AppComponent, {
            set: {imports: [], schemas: [NO_ERRORS_SCHEMA]}
        })
        .compileComponents();

        notificationService = TestBed.inject(NotificationService);
        fixture = TestBed.createComponent(AppComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should have null notification initially', () => {
        expect(component.notification()).toBeNull();
    });

    it('should delegate clearNotification to NotificationService.clear', () => {
        vi.spyOn(notificationService, 'clear');

        component.clearNotification();

        expect(notificationService.clear).toHaveBeenCalled();
    });

    it('should render the application title in an h1 element', () => {
        const compiled = fixture.nativeElement as HTMLElement;
        const heading = compiled.querySelector('h1');

        expect(heading).toBeTruthy();
        expect(heading?.textContent).toContain('FX Payment Processor');
    });

    it('should not display notification alert when there is no notification', () => {
        const compiled = fixture.nativeElement as HTMLElement;
        expect(compiled.querySelector('.alert')).toBeNull();
    });

    it('should display error notification with alert-danger class', () => {
        notificationService.showError('Something went wrong');

        fixture.detectChanges();
        const compiled = fixture.nativeElement as HTMLElement;
        const alert = compiled.querySelector('.alert');

        expect(alert).toBeTruthy();
        expect(alert?.textContent).toContain('Something went wrong');
        expect(alert?.classList.contains('alert-danger')).toBe(true);
    });

    it('should display success notification with alert-success class', () => {
        notificationService.showSuccess('Payment submitted');

        fixture.detectChanges();
        const compiled = fixture.nativeElement as HTMLElement;
        const alert = compiled.querySelector('.alert');

        expect(alert).toBeTruthy();
        expect(alert?.textContent).toContain('Payment submitted');
        expect(alert?.classList.contains('alert-success')).toBe(true);
    });

    it('should have a close button on the notification alert', () => {
        notificationService.showError('Error');
        fixture.detectChanges();
        const compiled = fixture.nativeElement as HTMLElement;

        const closeButton = compiled.querySelector('.btn-close');

        expect(closeButton).toBeTruthy();
    });
});
