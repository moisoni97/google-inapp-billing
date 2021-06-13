package games.moisoni.google_iab.models;

import com.android.billingclient.api.BillingResult;

import games.moisoni.google_iab.enums.ErrorType;

public class BillingResponse {

    private final ErrorType errorType;
    private final String message;
    private final int responseCode;

    public BillingResponse(ErrorType errorType, String message, int responseCode) {
        this.errorType = errorType;
        this.message = message;
        this.responseCode = responseCode;
    }

    public BillingResponse(ErrorType errorType, BillingResult billingResult) {
        this(errorType, billingResult.getDebugMessage(), billingResult.getResponseCode());
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public String getMessage() {
        return message;
    }

    public int getResponseCode() {
        return responseCode;
    }
}