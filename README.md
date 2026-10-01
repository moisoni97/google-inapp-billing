# Google In-App Billing Library v9+ [![API](https://img.shields.io/badge/API-23%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=23) [![JitCI](https://jitci.com/gh/moisoni97/google-inapp-billing/svg)](https://jitci.com/gh/moisoni97/google-inapp-billing) [![JitPack](https://jitpack.io/v/moisoni97/google-inapp-billing.svg)](https://jitpack.io/#moisoni97/google-inapp-billing)
A simple implementation of the Android In-App Billing API.

It supports: in-app purchases (both consumable and non-consumable) and subscriptions with a base plan or multiple offers.

| <img src="https://github.com/moisoni97/google-inapp-billing/blob/master/art/Google%20InApp%20Billing%20Image.jpg" width="80%" alt="image preview"> | <img src="https://github.com/moisoni97/google-inapp-billing/blob/master/art/Google%20InApp%20Billing%20Gif.gif" width="80%" alt="video example"> |
|:---:|:---:|

> [!IMPORTANT]  
> **Production Notice: Subscriptions**
>
> While one-time purchases have been thoroughly battle-tested in live production environments, the **subscription flows have currently only been tested in sandbox/development environments.**
>
> If you plan to use this library for subscriptions, please rigorously test the complete lifecycle (auto-renewals, cancellations, and grace periods) via the Play Console before deploying.

> [!TIP]  
> **This vs. RevenueCat**
>
> **Use this library if:** You are building a game or app that relies on simple, one-time purchases (e.g., buying consumable coins, removing ads, or unlocking a premium version). This library is lightweight, zero-dependency, and handles the Google Play Billing lifecycle perfectly for these use cases.
>
> **Use RevenueCat if:** Your core business model revolves around **Subscriptions**. Managing subscription lifecycles (grace periods, pauses, cross-platform syncing, and server-side receipt validation) purely on-device is highly prone to edge cases. For robust, production-ready subscriptions, I highly recommend using a dedicated service like [RevenueCat](https://www.revenuecat.com/) instead.

# Implementation

It is recommended to implement the `BillingConnector` instance in your MainActivity (or any other activity that the user **frequently interacts with**).

This is necessary because sometimes (due to slow payment methods like cash, bank transfers, or UPI) a purchase is not instantly processed and will have a `PENDING` state. All `PENDING` state purchases cannot be acknowledged or consumed and **will be refunded** by Google after 3 days if not completed.

The library automatically handles acknowledgment and consumption once the payment clears:
1. When a transaction is first made in `PENDING` state, the library provides `ACKNOWLEDGE_WARNING` and `CONSUME_WARNING` error callbacks to let you know the payment is not completed yet. Here you can inform the user to wait or complete the payment at their provider. The warning is reported once per purchase (not on every refresh). To show the pending state later on, use `billingConnector.isPurchasePending(productId)` or `purchase.isPending()`.
2. Once the user pays (hours or days later), Google Play updates the purchase state to `PURCHASED`.
3. The next time the user opens or returns to your app, `BillingConnector` automatically queries Google Play, detects the completed purchase, executes the auto-acknowledgment/consumption, and triggers `onPurchaseAcknowledged` or `onPurchaseConsumed`.

Because this sync requires an active `BillingConnector`, hosting it in your MainActivity ensures your app regularly reconnects, syncs with Google Play, and never misses a completed transaction.

The sync on return (step 3) happens automatically when you pass a `Lifecycle` object to the constructor. If you pass `null`, call `billingConnector.refreshPurchases()` from your activity's `onResume()`.

> [!WARNING]
> **Keep the screen open until the purchase is processed.** A purchase is processed when `onPurchaseConsumed` (consumables) or `onPurchaseAcknowledged` (non-consumables and subscriptions) is called. Until then, keep the screen that owns the `BillingConnector` open (e.g. don't call `finish()` in `onProductsPurchased`).
>
> When the screen is destroyed, the `BillingConnector` is released (automatically when a `Lifecycle` is passed, or by your `release()` method call) and stops delivering callbacks:
> * **Consumables:** if the consumption completes after that, `onPurchaseConsumed` is never called. Google Play doesn't return a consumed purchase again, so the user pays but doesn't receive the item.
> * **Non-consumables and subscriptions:** nothing is lost. They are acknowledged or restored on the next start (`onPurchaseAcknowledged` / `onPurchasedProductsFetched`).
>
> **Use a single `BillingConnector` at a time.** Each instance consumes and acknowledges purchases on its own, so two active instances send duplicate requests (the second one reports an error).

# Getting Started

* Requirements:
  - `minSdk` 23 (Android 6.0) or higher
  - `compileSdk` 35 or higher (required by the Google Play Billing Library)
  - Kotlin projects: Kotlin 2.0 or higher. Java-only projects have no Kotlin requirement.

* Add the JitPack repository to your project's `build.gradle` file:

```gradle
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

* Add the dependency in your app's `build.gradle` file:

```gradle
dependencies {
    implementation 'com.github.moisoni97:google-inapp-billing:2.0.0'
}
```

* The `com.android.vending.BILLING` permission is added automatically by the Google Play Billing Library, no manifest changes are needed.

# Usage

* Create an instance of the BillingConnector class. The constructor takes 3 parameters:
  - *Context*
  - *License key from `Play Console`* (or `null` / `""` if you perform signature verification on your backend server)
  - *Lifecycle object* (or `null` to handle instance cleanup manually)

```java
billingConnector = new BillingConnector(this, "license_key", getLifecycle())
        .setConsumableIds(consumableIds)
        .setNonConsumableIds(nonConsumableIds)
        .setSubscriptionIds(subscriptionIds)
        .autoAcknowledge()
        .autoConsume()
        .enableLogging()
        .connect();
```

* Implement the listener to handle event results and errors:

```java
billingConnector.setBillingEventListener(new BillingEventListener() {
  @Override
  public void onProductsFetched(@NonNull List<ProductInfo> productDetails) {
    /*Provides a list with fetched products from Play Console*/

    /*
     * Triggered separately for in-app products and subscriptions, and again after the connection is re-established.
     * Replace previously fetched products (e.g. by product ID) instead of appending them.
     */
  }

  @Override
  public void onPurchasedProductsFetched(@NonNull ProductType productType, @NonNull List<PurchaseInfo> purchases) {
    /*
     * Provides a list with currently owned products.
     * Note: This callback will be triggered separately for ProductType.INAPP and ProductType.SUBS.
     * It is called even when no purchased products are returned.
     *
     * It is triggered after connecting and again every time purchases are refreshed
     * (each time the app resumes when a Lifecycle is provided, or when you call refreshPurchases()).
     *
     * Restore entitlements for NON-CONSUMABLE products and SUBSCRIPTIONS the user already owns here
     * (e.g. on app restart), since onPurchaseAcknowledged is only triggered once, when the purchase is acknowledged.
     * Only restore purchases where purchase.isPurchased() and purchase.isAcknowledged() are true:
     * PENDING purchases are listed too, and purchases that are not acknowledged yet are acknowledged
     * right after this callback (they are granted in onPurchaseAcknowledged).
     * Granting must be safe to repeat (e.g. set a flag, don't add to a counter).
     *
     * Do not grant CONSUMABLE products here: an owned consumable has not been consumed yet,
     * it is granted through onPurchaseConsumed once consumed.
     */
  }

  @Override
  public void onProductsPurchased(@NonNull List<PurchaseInfo> purchases) {
    /*Callback after a purchase flow finishes successfully*/

    /*
     * Purchases can still be in PENDING state here (e.g. cash payments).
     * Grant entitlement in onPurchaseAcknowledged / onPurchaseConsumed instead.
     */
  }

  @Override
  public void onPurchaseAcknowledged(@NonNull PurchaseInfo purchase) {
    /*Callback after a purchase is acknowledged*/

    /*
     * Grant user entitlement for NON-CONSUMABLE products and SUBSCRIPTIONS here.
     *
     * Even though onProductsPurchased is triggered when a purchase flow finishes,
     * the transaction must be acknowledged within 3 days or Google will refund it.
     *
     * To ensure valid purchases are acknowledged, the library automatically
     * checks and acknowledges unacknowledged products at startup and each time purchases are refreshed,
     * so this is also triggered for purchases made earlier (e.g. the app was closed before the acknowledgment).
     *
     * Note: purchase.isAcknowledged() still returns false here, it reflects the state before the acknowledgment.
     */
  }

  @Override
  public void onPurchaseConsumed(@NonNull PurchaseInfo purchase) {
    /*Callback after a purchase is consumed*/

    /*
     * Grant user entitlement for CONSUMABLE products here (e.g. coins, gems).
     *
     * Consuming makes the product available to be purchased again.
     * If multi-quantity purchases are enabled in Play Console, grant purchase.getQuantity() items.
     */
  }

  @Override
  public void onProductQueryError(@NonNull String productId, @NonNull BillingResponse response) {
    /*Callback after Google Play doesn't return the details of a product ID*/

    /*
     * The response.getDebugMessage() contains the reason: not found (not created or not active in Play Console),
     * invalid product ID format, or no offer the user is eligible for.
     */
  }

  @Override
  public void onBillingError(@NonNull BillingConnector billingConnector, @NonNull BillingResponse response) {
    /*Callback after an error occurs*/

    /*
     * Errors are also reported for the connection, not only for purchases.
     * A failed connection is retried automatically, so the same error can be reported again
     * (e.g. while the device is offline). Avoid showing a dialog for every error.
     */

    switch (response.getErrorType()) {
      case CLIENT_NOT_READY:
        //TODO - client is not ready yet
        break;
      case CLIENT_DISCONNECTED:
        //TODO - client has disconnected (the library reconnects automatically)
        break;
      case PRODUCT_NOT_EXIST:
        //TODO - product does not exist
        break;
      case CONSUME_ERROR:
        //TODO - error during consumption
        break;
      case CONSUME_WARNING:
        /*
         * Triggered when a consumable purchase is in PENDING state (reported once per purchase).
         * Entitlement should only be granted when the state becomes PURCHASED.
         */
        //TODO - warning during consumption
        break;
      case ACKNOWLEDGE_ERROR:
        //TODO - error during acknowledgment
        break;
      case ACKNOWLEDGE_WARNING:
        /*
         * Triggered when a purchase cannot be acknowledged because it is PENDING (reported once per purchase).
         * A purchase can be acknowledged only when the state is PURCHASED.
         */
        //TODO - warning during acknowledgment
        break;
      case FETCH_PURCHASED_PRODUCTS_ERROR:
        //TODO - error occurred while querying purchased products
        break;
      case BILLING_ERROR:
        //TODO - error occurred during initialization / querying product details
        break;
      case USER_CANCELED:
        //TODO - transaction was canceled by the user
        break;
      case SERVICE_UNAVAILABLE:
        //TODO - the service is currently unavailable
        break;
      case NETWORK_ERROR:
        //TODO - a network error occurred during the operation
        break;
      case BILLING_UNAVAILABLE:
        //TODO - billing is not available on this device or account (e.g. outdated Play Store, no Google account, unsupported country)
        break;
      case ITEM_UNAVAILABLE:
        //TODO - requested product is not available for purchase
        break;
      case DEVELOPER_ERROR:
        //TODO - error resulting from incorrect usage of the API
        break;
      case ERROR:
        //TODO - fatal error during the API action
        break;
      case ITEM_ALREADY_OWNED:
        /*
         * The library automatically refreshes purchases after this error,
         * so with autoConsume() a consumable that was not consumed yet gets consumed.
         */
        //TODO - failure to purchase since item is already owned
        break;
      case ITEM_NOT_OWNED:
        //TODO - failure to consume since item is not owned
        break;
      case PLAY_STORE_NOT_INSTALLED:
        //TODO - Google Play Store is not installed on the device
        break;
      case SIGNATURE_VERIFICATION_FAILED:
        /*
         * Triggered when a purchase fails signature verification: the license key
         * does not match the one from Play Console, or the purchase was tampered with.
         * The purchase is ignored (not acknowledged / consumed).
         */
        //TODO - purchase signature verification failed
        break;
      case FEATURE_NOT_SUPPORTED:
        //TODO - the requested feature is not supported by Google Play on this device
        break;
    }
  }
});
```

# Initiate Purchase

* Purchase a non-consumable or consumable product:

```java
billingConnector.purchase(this, "product_id");
```

* Purchase the first offer of a subscription (`index 0`). Fine for a subscription with a single base plan and no offers:

```java
billingConnector.subscribe(this, "product_id");
```

* Purchase a subscription with a specific base plan or offer (recommended for subscriptions with multiple base plans or offers):

```java
// Base plan without an offer
billingConnector.subscribe(this, "product_id", "base_plan_id", null);

// Base plan with a specific offer (e.g. a free trial)
billingConnector.subscribe(this, "product_id", "base_plan_id", "offer_id");
```

The IDs are the ones configured in Play Console. They are also available from `ProductInfo.getSubscriptionOfferDetails()` (`getBasePlanId()` / `getOfferId()`).

Google Play only returns offers the user is eligible for. If the offer is not available (e.g. the user already used a free trial), `onBillingError` is triggered with `ITEM_UNAVAILABLE`. An unknown base plan triggers `DEVELOPER_ERROR`.

* Purchase a subscription with multiple offers by index:

```java
billingConnector.subscribe(this, "product_id", 0);
billingConnector.subscribe(this, "product_id", 1);
```

> [!NOTE]  
> Google Play does not guarantee the order of subscription offers, so an index may not always point to the same offer. Prefer selecting offers by ID.

* Cancel / Manage a subscription:

```java
// Open the Google Play subscription settings
billingConnector.unsubscribe(this, "product_id");

// Open the general subscriptions page
billingConnector.unsubscribe(this, null);
```

# Check Purchase Status

You can synchronously query the status of any product or subscription:

```java
// Check if a product (in-app product or subscription) is currently purchased
PurchasedResult result = billingConnector.isPurchased("product_id");
switch (result) {
    case YES:
        // User owns this product (e.g. remove ads)
        break;
    case NO:
        // User does not own this product
        break;
    case CLIENT_NOT_READY:
    case PURCHASED_PRODUCTS_NOT_FETCHED_YET:
        // Not known yet. The billing client is still connecting or querying purchases
        // (or the purchases query failed, it is retried on the next refresh)
        // Don't revoke an entitlement here, wait for YES / NO
        break;
}

// Check if a purchase is currently waiting for payment (e.g. cash payment)
boolean isPending = billingConnector.isPurchasePending("product_id");

// Check if a subscription is currently active (PURCHASED state)
boolean isSubsActive = billingConnector.isSubscriptionActive("subscription_id");

// Check if a subscription is active AND auto-renewing
// (false once the user cancelled, the subscription stays active until the end of the paid period)
boolean isSubsAutoRenewing = billingConnector.isSubscriptionAutoRenewing("subscription_id");

// Check if Google Play Store is installed on the device
boolean isPlayStoreInstalled = billingConnector.isPlayStoreInstalled(context);

// Get the list of all currently owned products
List<PurchaseInfo> purchases = billingConnector.getPurchasedProductsList();
```

Once purchases are fetched, `isPurchased()` answers `YES` / `NO` from the last fetched purchases, also while the billing client reconnects or when product details are unavailable. A `PENDING` purchase is reported as `NO`.

`isPurchasePending()`, `isSubscriptionActive()`, `isSubscriptionAutoRenewing()` and `getPurchasedProductsList()` only know the purchases fetched so far, so they return `false` (or an empty list) until purchases are fetched. Use `isPurchased()` when you need to tell "not owned" apart from "not known yet".

# Manual Consume & Acknowledge

If you choose not to use `autoConsume()` or `autoAcknowledge()` (for example, if you verify receipts on your backend or need to deliver digital goods first), you can trigger them manually:

```java
// Manually consume a consumable purchase
billingConnector.consumePurchase(purchaseInfo);

// Manually acknowledge a non-consumable product or subscription
billingConnector.acknowledgePurchase(purchaseInfo);
```

A purchase is consumed / acknowledged only once per `BillingConnector` instance. Repeated calls for the same purchase are ignored, and failed attempts can be retried.

Purchases that don't apply are ignored (and logged when `enableLogging()` is set), so you can pass every owned purchase to both methods:
* `consumePurchase()` only consumes `CONSUMABLE` purchases.
* `acknowledgePurchase()` only acknowledges `NON_CONSUMABLE` and `SUBSCRIPTION` purchases that are not acknowledged yet. Consumables don't need it, consuming a purchase also acknowledges it.
* A `PENDING` purchase can't be consumed or acknowledged yet, it reports `CONSUME_WARNING` / `ACKNOWLEDGE_WARNING` instead.

# Refresh Purchases

When a `Lifecycle` object is passed to the constructor, purchases are refreshed automatically every time the LifecycleOwner resumes. This handles purchases completed while the app was in the background (e.g. `PENDING` payments that cleared).

You can also trigger a refresh manually (e.g. from `onResume()` when you passed `null` for the `lifecycle` parameter, or from a "Restore purchases" button):

```java
billingConnector.refreshPurchases();
```

A refresh queries the owned purchases (triggering `onPurchasedProductsFetched` and the automatic acknowledgment/consumption). If a product details query failed before (e.g. the app was started offline), it runs that query again first. Products that Google Play did not return are not queried again until the next connection, so `onProductQueryError` is not repeated on every refresh. It does nothing while the billing client is not connected.

# Useful Methods

## PurchaseInfo

Inside callbacks such as `onProductsPurchased`, `onPurchaseAcknowledged`, or `onPurchaseConsumed`, you have access to `PurchaseInfo`:

| Method | Description |
| :--- | :--- |
| `purchase.getProduct()` | Returns the product ID. A purchase of several products is reported as one `PurchaseInfo` per product. |
| `purchase.getProducts()` | Returns a list of all product IDs in this purchase. |
| `purchase.getOrderId()` | Returns the unique Google Play order ID. **Can be `null`** while the purchase is `PENDING`. |
| `purchase.getPurchaseToken()` | Returns the token used to identify this purchase on Google Play. |
| `purchase.getPurchaseTime()` | Returns the time the product was purchased (milliseconds since epoch). |
| `purchase.getQuantity()` | Returns the quantity purchased. |
| `purchase.isAcknowledged()` | Returns `true` if the purchase has already been acknowledged. Reflects the state when the purchase was queried (still `false` inside `onPurchaseAcknowledged`). |
| `purchase.isAutoRenewing()` | Returns `true` if the subscription is set to auto-renew. |
| `purchase.isPurchased()` | Returns `true` if the state is `PURCHASED`. |
| `purchase.isPending()` | Returns `true` if the state is `PENDING`. |
| `purchase.getSkuProductType()` | Returns `CONSUMABLE`, `NON_CONSUMABLE` or `SUBSCRIPTION`. |
| `purchase.getProductInfo()` | Returns the product details (title, price, etc.). **Can be `null`** when Google Play does not return details for an owned product (e.g. the product was deactivated in Play Console, or its details query failed). |
| `purchase.getOriginalJson()` | Returns the purchase data as JSON. Send it with `getSignature()` to your server to verify the purchase there. |
| `purchase.getSignature()` | Returns the signature of `getOriginalJson()`, which can be verified with your license key (the public key from Play Console). |
| `purchase.getPurchase()` | Returns the original Play Billing `Purchase` object. |

## ProductInfo

`ProductInfo` (from `onProductsFetched`, or `purchase.getProductInfo()`) describes a product:

| Method | Description |
| :--- | :--- |
| `productInfo.getProduct()` | Returns the product ID. |
| `productInfo.getTitle()` / `getName()` / `getDescription()` | Returns the title, name and description set in Play Console. `getTitle()` also contains the app name in parentheses (e.g. `100 Coins (My App)`), `getName()` doesn't. |
| `productInfo.getSkuProductType()` | Returns `CONSUMABLE`, `NON_CONSUMABLE` or `SUBSCRIPTION`. |
| `productInfo.getOneTimePurchaseOfferFormattedPrice()` | Returns the formatted price of an in-app product (e.g. `$0.99`). **`null` for subscriptions.** |
| `productInfo.getOneTimePurchaseOfferPriceAmountMicros()` | Returns the price in micro-units (1,000,000 micro-units = 1 unit of the currency). `0` for subscriptions. |
| `productInfo.getOneTimePurchaseOfferPriceCurrencyCode()` | Returns the ISO 4217 currency code (e.g. `USD`). **`null` for subscriptions.** |
| `productInfo.getSubscriptionOfferDetails()` | Returns the base plans and offers of a subscription the user is eligible for (empty for in-app products). |
| `productInfo.getProductDetails()` | Returns the original Play Billing `ProductDetails` object. |

Each `SubscriptionOfferDetails` describes a base plan or an offer:

| Method | Description |
| :--- | :--- |
| `offer.getBasePlanId()` | Returns the base plan ID. |
| `offer.getOfferId()` | Returns the offer ID. **`null` for the base plan itself** (no offer). |
| `offer.getOfferTags()` | Returns the tags set in Play Console. |
| `offer.getPricingPhases()` | Returns the pricing phases in order (e.g. a free trial followed by the regular price), each with `getFormattedPrice()`, `getBillingPeriod()` (ISO 8601, e.g. `P1M`), `getBillingCycleCount()` and `getRecurrenceMode()`. |

For example, to show the regular price of each subscription offer (the last pricing phase):

```java
for (SubscriptionOfferDetails offer : productInfo.getSubscriptionOfferDetails()) {
    List<SubscriptionOfferDetails.PricingPhases> phases = offer.getPricingPhases();
    if (!phases.isEmpty()) {
        String regularPrice = phases.get(phases.size() - 1).getFormattedPrice();
    }
}
```

## BillingResponse

`onBillingError` and `onProductQueryError` provide a `BillingResponse`:

| Method | Description |
| :--- | :--- |
| `response.getErrorType()` | Returns the `ErrorType` (see the `onBillingError` example above). |
| `response.getResponseCode()` | Returns the Google Play Billing response code (`BillingClient.BillingResponseCode`), or `99` for errors detected by the library itself (e.g. `CLIENT_NOT_READY`). |
| `response.getDebugMessage()` | Returns a message describing the error. Meant for logging, not for display to users. |

# Release Instance

* When passing a `Lifecycle` object to the constructor, the library automatically handles cleanup when the LifecycleOwner is destroyed:

```java
billingConnector = new BillingConnector(this, "license_key", getLifecycle());
```

* If you passed `null` for the `lifecycle` parameter, call `release()` manually when the instance is no longer needed (e.g. in `onDestroy()`):

```java
@Override
protected void onDestroy() {
  super.onDestroy();
  if (billingConnector != null) {
    billingConnector.release();
  }
}
```

A released instance can't be connected again. Create a new `BillingConnector` if you need one later.

# Kotlin

`Kotlin` is interoperable with `Java` and vice versa. This library works without any issues in `Kotlin` projects.

Getters that can return `null` are annotated with `@Nullable`, so Kotlin sees them as nullable types (e.g. `productInfo.oneTimePurchaseOfferFormattedPrice` is a `String?`).

The sample app provides an example for `Kotlin` users.

# Sample App

You can go through the sample app to see a more advanced integration of the library.

It also shows a simple logic for a "remove ads" button scenario.

# Credits

This is an open-source project designed to help developers quickly and easily implement the Google Billing API.

The library uses a codebase from a fork created by [@Mustafa Rasheed](https://github.com/MRZ07) and was heavily modified by me and later by other contributors.
