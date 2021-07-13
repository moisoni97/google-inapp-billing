package games.moisoni.google_iab.models;

import androidx.annotation.NonNull;

import com.android.billingclient.api.BillingResult;

import games.moisoni.google_iab.enums.ErrorType;

public class BillingResponse {

    private final ErrorType errorType;

    private final String debugMessage;
    private final int responseCode;

    public BillingResponse(ErrorType errorType, String debugMessage, int responseCode) {
        this.errorType = errorType;
        this.debugMessage = debugMessage;
        this.responseCode = responseCode;
    }

    public BillingResponse(ErrorType errorType, BillingResult billingResult) {
        this(errorType, billingResult.getDebugMessage(), billingResult.getResponseCode());
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public String getDebugMessage() {
        return debugMessage;
    }

    public int getResponseCode() {
        return responseCode;
    }

    @NonNull
    @Override
    public String toString() {
        return "BillingResponse: Error type: " + errorType +
                " Response code: " + responseCode + " Message: " + debugMessage;
    }
}