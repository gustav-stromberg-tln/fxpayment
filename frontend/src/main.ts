import {ErrorHandler} from '@angular/core';
import {bootstrapApplication} from '@angular/platform-browser';
import {provideHttpClient, withInterceptors} from '@angular/common/http';
import {AppComponent} from './app/app.component';
import {httpErrorInterceptor} from './app/interceptors/http-error.interceptor';
import {GlobalErrorHandler} from './app/handlers/global-error.handler';

bootstrapApplication(AppComponent, {
    providers: [
        {provide: ErrorHandler, useClass: GlobalErrorHandler},
        provideHttpClient(withInterceptors([httpErrorInterceptor]))
    ]
}).catch(err => console.error(err));
