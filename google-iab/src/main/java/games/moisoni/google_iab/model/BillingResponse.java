package games.moisoni.google_iab.model;

import androidx.annotation.NonNull;

import com.android.billingclient.api.BillingResult;

import games.moisoni.google_iab.type.ErrorType;

/**
 * Describes an error reported to BillingEventListener.onBillingError() or onProductQueryError()
 */
public class BillingResponse {

    private final ErrorType errorType;

    private final String debugMessage;
    private final int responseCode;

    /**
     * @param errorType    - is the type of the error
     * @param debugMessage - is a message describing the error
     * @param responseCode - is the Play Billing response code, or 99 for errors detected by the library
     */
    public BillingResponse(ErrorType errorType, String debugMessage, int responseCode) {
        this.errorType = errorType;
        this.debugMessage = debugMessage;
        this.responseCode = responseCode;
    }

    /**
     * Uses the debug message and response code of a Play Billing result
     */
    public BillingResponse(ErrorType errorType, @NonNull BillingResult billingResult) {
        this(errorType, billingResult.getDebugMessage(), billingResult.getResponseCode());
    }

    /**
     * Returns the type of the error
     */
    public ErrorType getErrorType() {
        return errorType;
    }

    /**
     * Returns a message describing the error, from Play Billing or from the library
     * <p>
     * Meant for logging and debugging, not for display to users
     */
    public String getDebugMessage() {
        return debugMessage;
    }

    /**
     * Returns the Play Billing response code (BillingClient.BillingResponseCode),
     * or 99 for errors detected by the library itself (e.g. CLIENT_NOT_READY)
     */
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