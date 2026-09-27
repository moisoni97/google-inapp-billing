# Google In-App Billing Library v9+ [![API](https://img.shields.io/badge/API-23%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=21) [![JitCI](https://jitci.com/gh/moisoni97/google-inapp-billing/svg)](https://jitci.com/gh/moisoni97/google-inapp-billing) [![JitPack](https://jitpack.io/v/moisoni97/google-inapp-billing.svg)](https://jitpack.io/#moisoni97/google-inapp-billing)
A simple implementation of the Android In-App Billing API.

It supports: in-app purchases (both consumable and non-consumable) and subscriptions with a base plan or multiple offers.

<!--suppress HtmlDeprecatedAttribute -->
<table>
  <tr>
    <td align="center" style="border: none;">
      <img src="https://github.com/moisoni97/google-inapp-billing/blob/master/art/Google%20InApp%20Billing%20Image.jpg" width="80%" alt="image preview">
      <br>
    </td>
    <td align="center" style="border: none;">
      <img src="https://github.com/moisoni97/google-inapp-billing/blob/master/art/Google%20InApp%20Billing%20Gif.gif" width="80%" alt="video example">
      <br>
    </td>
  </tr>
</table>

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
1. When a transaction is first made in `PENDING` state, the library provides `ACKNOWLEDGE_WARNING` and `CONSUME_WARNING` error callbacks to let you know the payment is not completed yet. Here you can inform the user to wait or complete the payment at their provider.
2. Once the user pays (hours or days later), Google Play updates the purchase state to `PURCHASED`.
3. The next time the user opens or returns to your app, `BillingConnector` automatically queries Google Play, detects the completed purchase, executes the auto-acknowledgment/consumption, and triggers `onPurchaseAcknowledged` or `onPurchaseConsumed`.

Because this sync requires an active `BillingConnector`, hosting it in your MainActivity ensures your app regularly reconnects, syncs with Google Play, and never misses a completed transaction.

# Getting Started

* Your project should build against Android 6.0 (minSdkVersion 23).

* Add the JitPack repository to your project's build.gradle file:

```gradle
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

* Add the dependency in your app's build.gradle file:

```gradle
dependencies {
    implementation 'com.github.moisoni97:google-inapp-billing:1.1.9'
}
```

* Open the AndroidManifest.xml of your application and add this permission:

```xml
  <uses-permission android:name="com.android.vending.BILLING" />
```

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
  }

  @Override
  public void onPurchasedProductsFetched(@NonNull ProductType productType, @NonNull List<PurchaseInfo> purchases) {
    /*
     * Provides a list with currently owned products.
     * Note: This callback will be triggered separately for ProductType.INAPP and ProductType.SUBS.
     * It is called even when no purchased products are returned.
     */
  }

  @Override
  public void onProductsPurchased(@NonNull List<PurchaseInfo> purchases) {
    /*Callback after a purchase flow finishes successfully*/
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
     * checks and acknowledges unacknowledged products at startup.
     */
  }

  @Override
  public void onPurchaseConsumed(@NonNull PurchaseInfo purchase) {
    /*Callback after a purchase is consumed*/

    /*
     * Grant user entitlement for CONSUMABLE products here (e.g. coins, gems).
     *
     * Consuming makes the product available to be purchased again.
     */
  }

  @Override
  public void onProductQueryError(@NonNull String productId, @NonNull BillingResponse response) {
    /*Callback after a specific product ID is not found on Play Console*/
  }

  @Override
  public void onBillingError(@NonNull BillingConnector billingConnector, @NonNull BillingResponse response) {
    /*Callback after an error occurs*/

    switch (response.getErrorType()) {
      case CLIENT_NOT_READY:
        //TODO - client is not ready yet
        break;
      case CLIENT_DISCONNECTED:
        //TODO - client has disconnected
        break;
      case PRODUCT_NOT_EXIST:
        //TODO - product does not exist
        break;
      case CONSUME_ERROR:
        //TODO - error during consumption
        break;
      case CONSUME_WARNING:
        /*
         * Triggered when a consumable purchase is in PENDING state.
         * Entitlement should only be granted when the state becomes PURCHASED.
         */
        //TODO - warning during consumption
        break;
      case ACKNOWLEDGE_ERROR:
        //TODO - error during acknowledgment
        break;
      case ACKNOWLEDGE_WARNING:
        /*
         * Triggered when a purchase cannot be acknowledged because it is PENDING.
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
        //TODO - a user billing error occurred during processing
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
        //TODO - failure to purchase since item is already owned
        break;
      case ITEM_NOT_OWNED:
        //TODO - failure to consume since item is not owned
        break;
      case PLAY_STORE_NOT_INSTALLED:
        //TODO - Google Play Store is not installed on the device
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

* Purchase a subscription with a base plan:

```java
billingConnector.subscribe(this, "product_id");
```

* Purchase a subscription with multiple offers:

```java
billingConnector.subscribe(this, "product_id", 0);
billingConnector.subscribe(this, "product_id", 1);
```

* Cancel / Manage a subscription (opens Google Play subscription settings):

```java
billingConnector.unsubscribe(this, "product_id");
```

# Check Purchase Status

You can synchronously query the status of any product or subscription:

```java
// Check if an in-app product is currently purchased
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
        // Billing client is still connecting or querying purchases
        break;
}

// Check if a purchase is currently waiting for payment (e.g. cash payment)
boolean isPending = billingConnector.isPurchasePending("product_id");

// Check if a subscription is currently active (PURCHASED state)
boolean isSubActive = billingConnector.isSubscriptionActive("subscription_id");

// Check if a subscription is active AND auto-renewing (not cancelled / in grace period)
boolean isSubAutoRenewing = billingConnector.isSubscriptionAutoRenewing("subscription_id");

// Check if Google Play Store is installed on the device
boolean isPlayStoreInstalled = billingConnector.isPlayStoreInstalled(context);

// Get the list of all currently owned products
List<PurchaseInfo> purchases = billingConnector.getPurchasedProductsList();
```

# Manual Consume & Acknowledge

If you choose not to use `autoConsume()` or `autoAcknowledge()` (for example, if you verify receipts on your backend or need to deliver digital goods first), you can trigger them manually:

```java
// Manually consume a consumable purchase
billingConnector.consumePurchase(purchaseInfo);

// Manually acknowledge a non-consumable product or subscription
billingConnector.acknowledgePurchase(purchaseInfo);
```

# Useful Methods

Inside callbacks such as `onProductsPurchased`, `onPurchaseAcknowledged`, or `onPurchaseConsumed`, you have access to `PurchaseInfo`:

| Method | Description |
| :--- | :--- |
| `purchase.getProduct()` | Returns the primary product ID / SKU string. |
| `purchase.getProducts()` | Returns a list of all product IDs in this purchase. |
| `purchase.getOrderId()` | Returns the unique Google Play order ID. |
| `purchase.getPurchaseToken()` | Returns the token used to identify this purchase on Google Play. |
| `purchase.getPurchaseTime()` | Returns the time the product was purchased (milliseconds since epoch). |
| `purchase.getQuantity()` | Returns the quantity purchased. |
| `purchase.isAcknowledged()` | Returns `true` if the purchase has already been acknowledged. |
| `purchase.isAutoRenewing()` | Returns `true` if the subscription is set to auto-renew. |
| `purchase.isPurchased()` | Returns `true` if the state is `PURCHASED`. |
| `purchase.isPending()` | Returns `true` if the state is `PENDING`. |

# Release Instance

* When passing a `Lifecycle` object to the constructor, the library automatically handles cleanup when the LifecycleOwner is destroyed:

```java
billingConnector = new BillingConnector(this, "license_key", getLifecycle())
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

# Kotlin

`Kotlin` is interoperable with `Java` and vice versa. This library works without any issues in `Kotlin` projects.

The sample app provides an example for `Kotlin` users.

# Sample App

You can go through the sample app to see a more advanced integration of the library.

It also shows a simple logic for a "remove ads" button scenario.

# Credits

This is an open-source project designed to help developers quickly and easily implement the Google Billing API.

The library uses a codebase from a fork created by [@Mustafa Rasheed](https://github.com/MRZ07) and was heavily modified by me and later by other contributors.
