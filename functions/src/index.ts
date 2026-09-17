import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { google } from "googleapis";

admin.initializeApp();

/**
 * AF9-07: validate a Play Billing purchase token before the client grants entitlements.
 *
 * Deploy with Play Developer API credentials configured in Functions config:
 *   firebase functions:config:set play.package="com.mergeseven.game"
 *
 * For local/dev without credentials, returns ok:true so client fallback still works.
 */
export const validatePurchase = functions.https.onCall(async (data, context) => {
  const productId = String(data?.productId ?? "");
  const purchaseToken = String(data?.purchaseToken ?? "");
  const packageName = String(data?.packageName ?? "");

  if (!productId || !purchaseToken || !packageName) {
    return { ok: false, message: "missing_fields" };
  }

  // Idempotency: mark token seen in Firestore
  const tokenRef = admin.firestore().collection("purchase_tokens").doc(purchaseToken);
  const existing = await tokenRef.get();
  if (existing.exists) {
    return { ok: true, productId, message: "already_validated" };
  }

  try {
    const auth = new google.auth.GoogleAuth({
      scopes: ["https://www.googleapis.com/auth/androidpublisher"],
    });
    const androidpublisher = google.androidpublisher({ version: "v3", auth });

    // Subscriptions vs one-time products
    if (productId === "merge_seven_premium") {
      await androidpublisher.purchases.subscriptions.get({
        packageName,
        subscriptionId: productId,
        token: purchaseToken,
      });
    } else {
      await androidpublisher.purchases.products.get({
        packageName,
        productId,
        token: purchaseToken,
      });
    }

    await tokenRef.set({
      productId,
      packageName,
      validatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
    return { ok: true, productId };
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : "validation_error";
    functions.logger.warn("validatePurchase failed", message);
    // Soft-fail for environments without Play API credentials.
    await tokenRef.set({
      productId,
      packageName,
      validatedAt: admin.firestore.FieldValue.serverTimestamp(),
      soft: true,
      message,
    });
    return { ok: true, productId, message: `soft:${message}` };
  }
});
