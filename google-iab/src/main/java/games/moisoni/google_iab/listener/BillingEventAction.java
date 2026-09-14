package games.moisoni.google_iab.listener;

import androidx.annotation.NonNull;

/**
 * Action executed on the UI thread against a non-null BillingEventListener
 */
public interface BillingEventAction {
    void dispatch(@NonNull BillingEventListener listener);
}