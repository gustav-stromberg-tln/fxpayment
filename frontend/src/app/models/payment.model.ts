export interface PaymentRequest {
    amount: number;
    currency: string;
    recipient: string;
    recipientAccount: string;
}

export interface PaymentResponse {
    id: string;
    amount: string;
    currency: string;
    recipient: string;
    processingFee: string;
    createdAt: string;
}

export interface CurrencyResponse {
    code: string;
    name: string;
    decimals: number;
}

export interface PagedResponse<T> {
    content: T[];
    page: {
        totalElements: number;
        totalPages: number;
        size: number;
        number: number;
    };
}

export interface ApiErrorResponse {
    timestamp: string;
    status: number;
    errors: string[];
}
