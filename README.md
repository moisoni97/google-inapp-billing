# Google In-App Billing Library v4+
A simple implementation of the Android In-App Billing API.

It supports: in-app purchases (both consumable and non-consumable) and subscriptions.

![image preview](https://i.postimg.cc/kMSPYb5H/Google-In-App-Billing-Image.jpg)
![video example](https://i.postimg.cc/DZX0sDY2/Google-In-App-Billing-Purchase.gif)

# Getting Started

* You project should build against Android 4.1.x (minSdkVersion 16).

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
    implementation 'com.github.moisoni97:google-inapp-billing:1.0.3'
}
```

* Open the AndroidManifest.xml of your application and add this permission:

```xml
  <uses-permission android:name="com.android.vending.BILLING" />
```

# Important Notice

* For builds that use `minSdkVersion` lower than `24` it is very important to include the following in your app's build.gradle file:

```gradle
android {
  compileOptions {
    coreLibraryDesugaringEnabled true
    
    sourceCompatibility JavaVersion.VERSION_1_8
    targetCompatibility JavaVersion.VERSION_1_8
  }
}

dependencies {
  coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.5'
}
```

* For builds that use `minSdkVersion` lower than `21` add the above and also this:

```gradle
android {
    defaultConfig {
        multiDexEnabled true
    }
}
```

This step is required to enable support for some APIs on lower SDK versions that aren't available natively only starting from `minSdkVersion 24`.

# Usage

* Create an instance of BillingConnector class. Constructor will take 2 parameters:
  - *Context*
  - *License key from `Play Console`*
  
```java
billingConnector = new BillingConnector(this, "license_key")
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
            public void onProductsFetched(@NonNull List<SkuInfo> skuDetails) {
                /*Provides a list with fetched products*/
            }

            @Override
            public void onPurchasedProductsFetched(@NonNull List<PurchaseInfo> purchases) {
                /*Provides a list with fetched purchased products*/
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
                 * CONSUMABLE products entitlement can be granted either here or in onProductsPurchased
                 * */
            }

            @Override
            public void onBillingError(@NonNull BillingConnector billingConnector, @NonNull BillingResponse response) {
                /*Callback after an error occurs*/
                
                switch (response.getErrorType()) {
                    case CLIENT_NOT_READY:
                        //TODO - client is not ready
                        break;
                    case CLIENT_DISCONNECTED:
                        //TODO - client has disconnected
                        break;
                    case ITEM_NOT_EXIST:
                        //TODO - item doesn't exist
                        break;
                    case ITEM_ALREADY_OWNED:
                        //TODO - item is already owned
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
                    case CONSUME_ERROR:
                        //TODO - error during consumption
                        break;
                    case FETCH_PURCHASED_PRODUCTS_ERROR:
                        //TODO - error occurred while querying purchases
                        break;
                    case BILLING_ERROR:
                        //TODO - error occurred during initialization / querying sku details
                        break;
                }
            }
        });
```

# Initiate a purchase

* Purchase a non-consumable / consumable product:

```java
billingConnector.purchase(this, "sku_id");
```

* Purchase a subscription:

```java
billingConnector.subscribe(this, "sku_id");
```

* Cancel a subscription:

```java
billingConnector.unsubscribe(this, "sku_id");
```

# Sample App

Go through the sample app to see a more advanced integration of the library. 

It also shows a `Kotlin` example and how to implement some `useful public methods`.
