# Google In-App Billing Library v8+ [![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=21) [![JitCI](https://jitci.com/gh/moisoni97/google-inapp-billing/svg)](https://jitci.com/gh/moisoni97/google-inapp-billing) [![JitPack](https://jitpack.io/v/moisoni97/google-inapp-billing.svg)](https://jitpack.io/#moisoni97/google-inapp-billing)
A simple implementation of the Android In-App Billing API.

It supports: in-app purchases (both consumable and non-consumable) and subscriptions with a base plan or multiple offers.

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

# Implementation

* ### Recommended usage:

It is recommended to implement the `BillingConnector` instance in your MainActivity (or any other activity that the user **frequently interacts with**).

This is necessary because sometimes (due to different reasons) the purchase is not instantly processed and will have a `PENDING` state. All `PENDING` state purchases cannot be `acknowledged` or `consumed` and **will be refunded** by Google after 3 days.

The library automatically handles acknowledgement and consumption, but for that, it needs the `BillingConnector` reference. It cannot happen in a background service. So if the `BillingConnector` is set in a remote activity that the user **rarely interacts with (or not at all)**, it will never be instantiated to receive the `Billing API callback` to acknowledge the new updated purchase status and the user will lose the purchase.

The library provides `ACKNOWLEDGE_WARNING` and `CONSUME_WARNING` error callbacks to let you know that the purchase status is still `PENDING`. Here you can inform the user to wait or to come back a little bit later to receive the purchase.


* ### Special use case only (advanced):
The library also provides a `public void retryPendingPurchase(String productId)` method to "globally" retry `PENDING` purchases and `auto acknowledge/consume` them with exponential backoff, but to reliably use this, the `BillingConnector` must also be set in the `Application` level class and therefore have `two BillingConnector` logics in your app.

Set a method (in the application-level class) to retry all pending purchases:

```java
public void retryPendingPurchases() {
  if (billingConnector == null) return;

  List<PurchaseInfo> purchases = billingConnector.getPurchasedProductsList();
  for (PurchaseInfo purchase : purchases) {
    if (purchase.isPending()) {
      billingConnector.retryPendingPurchase(purchase.getProduct());
    }
  }
}
```

Call the above method in the `onProductsPurchased` callback (from the application-level class):

```java
@Override
public void onProductsPurchased(@NonNull List<PurchaseInfo> purchases) {
  //automatically retry when new pending purchases are detected
  for (PurchaseInfo purchase : purchases) {
    if (purchase.isPending()) {
      retryPendingPurchases();
      break;
    }
  }
}
```

Or in any other activity in `onResume()`, to constantly check for `PENDING` purchases:

```java
@Override
protected void onResume() {
  super.onResume();
  ((MyApplication) getApplication()).getBillingConnector().retryAllPendingPurchases();
}
```

# Getting Started

* Your project should build against Android 5.0 (minSdkVersion 21).

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
    implementation 'com.github.moisoni97:google-inapp-billing:1.1.7'
}
```

* Open the AndroidManifest.xml of your application and add this permission:

```xml
  <uses-permission android:name="com.android.vending.BILLING" />
```

# Usage

* Create an instance of BillingConnector class. Constructor will take 3 parameters:
  - *Context*
  - *License key from `Play Console`*
  - *Lifecycle object (or `null` to handle instance cleanup manually)*

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
    /*Provides a list with fetched products*/
  }

  @Override
  public void onPurchasedProductsFetched(@NonNull ProductType productType, @NonNull List<PurchaseInfo> purchases) {
    /*Provides a list with fetched purchased products*/

    /*
     * This will be called even when no purchased products are returned by the API
     * */
  }

  @Override
  public void onProductsPurchased(@NonNull List<PurchaseInfo> purchases) {
    /*Callback after a product is purchased*/
  }

  @Override
  public void onPurchaseAcknowledged(@NonNull PurchaseInfo purchase) {
    /*Callback after a purchase is acknowledged*/

    /*
     * Grant user entitlement for NON-CONSUMABLE products and SUBSCRIPTIONS here
     *
     * Even though onProductsPurchased is triggered when a purchase is successfully made
     * there might be a problem along the way with the payment and the purchase won't be acknowledged
     *
     * Google will refund users purchases that aren't acknowledged in 3 days
     *
     * To ensure that all valid purchases are acknowledged the library will automatically
     * check and acknowledge all unacknowledged products at the startup
     * */
  }

  @Override
  public void onPurchaseConsumed(@NonNull PurchaseInfo purchase) {
    /*Callback after a purchase is consumed*/

    /*
     * Grant user entitlement for CONSUMABLE products here
     *
     * Even though onProductsPurchased is triggered when a purchase is successfully made
     * there might be a problem along the way with the payment and the user will be able consume the product
     * without actually paying
     * */
  }

  @Override
  public void onProductQueryError(@NonNull String productId, @NonNull BillingResponse response) {
    /*Callback after a specific product ID is not found*/

    /*
     * This is useful for identifying configuration errors in the Play Console
     * */
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
         * This will be triggered when a consumable purchase has a PENDING state
         * User entitlement must be granted when the state is PURCHASED
         *
         * PENDING transactions usually occur when users choose cash as their form of payment
         *
         * Here users can be informed that it may take a while until the purchase complete
         * and to come back later to receive their purchase
         * */
        //TODO - warning during consumption
        break;
      case ACKNOWLEDGE_ERROR:
        //TODO - error during acknowledgment
        break;
      case ACKNOWLEDGE_WARNING:
        /*
         * This will be triggered when a purchase can not be acknowledged because the state is PENDING
         * A purchase can be acknowledged only when the state is PURCHASED
         *
         * PENDING transactions usually occur when users choose cash as their form of payment
         *
         * Here users can be informed that it may take a while until the purchase complete
         * and to come back later to receive their purchase
         * */
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
        //TODO - the purchase failed because the item is already owned
        break;
      case ITEM_NOT_OWNED:
        //TODO - the requested product is not available for purchase
        break;
      case PLAY_STORE_NOT_INSTALLED:
        //TODO - Google Play Store is not installed
        break;

      //related only to a specific method (public void retryPendingPurchase(String productId))
      //https://github.com/moisoni97/google-inapp-billing?tab=readme-ov-file#special-use-case-only-advanced
      case NOT_PENDING:
        //TODO - no pending purchase for product ID
        break;
      case PENDING_PURCHASE_CANCELED:
        //TODO - pending purchase may have been canceled
        break;
      case PENDING_PURCHASE_RETRY_ERROR:
        //TODO - pending purchase still not completed after retries
        break;
    }
  }
});
```

# Initiate Purchase

* Purchase a non-consumable/consumable product:

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

* Cancel a subscription:

```java
billingConnector.unsubscribe(this, "product_id");
```

# Release Instance

* Starting from version `1.1.5`, the library automatically releases the `BillingConnector` instance (set the `lifecycle` object to the `BillingConnector` constructor).
* To avoid memory leaks don't forget to release the BillingConnector instance when it's no longer needed.

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

Go through the sample app to see a more advanced integration of the library.

It also shows a simple logic for a "remove ads button" scenario.

# Credits

This is an open-source project meant to help developers to fastly and easily implement the Google Billing API.

The library uses a code base from a fork created by [@Mustafa Rasheed](https://github.com/MRZ07) and was heavily modified by me and later by other contributors.


