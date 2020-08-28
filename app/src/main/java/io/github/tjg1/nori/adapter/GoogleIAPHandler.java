/*
 * This file is part of nori.
 * Copyright (c) 2014-2016 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: GNU GPLv2
 */

package io.github.tjg1.nori.adapter;


import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import java.util.List;

import io.github.tjg1.nori.util.iab.IabHelper;
import io.github.tjg1.nori.util.iab.IabResult;
import io.github.tjg1.nori.util.iab.Inventory;
import io.github.tjg1.nori.util.iab.Purchase;

// This could be implemented as a fragment displaying the ListView and handling IAB functions.

/**
 * An adapter populating the ListView with available Google donation amounts and handling
 * interactions with the Google Play in-app purchase service.
 */
public class GoogleIAPHandler extends ArrayAdapter<Pair<String, String>>
        implements IabHelper.OnIabSetupFinishedListener, IabHelper.QueryInventoryFinishedListener,
        IabHelper.OnIabPurchaseFinishedListener, IabHelper.OnConsumeFinishedListener,
        AdapterView.OnItemClickListener {

    //region Instance fields
    /**
     * List of item SKUs available for purchase.
     */
    protected final List<String> itemSkus;
    /**
     * Listener handling user events and interactions with the Google IAP service.
     */
    protected final GoogleIAPHandler.Listener listener;
    /**
     * Activity context the adapter is used in.
     */
    private final Activity activity;
    /**
     * Google in-app purchase helper class.
     */
    private final IabHelper iabHelper;
    //endregion

    //region Constructors

    /**
     * Create a new object acting as a listener for {@link IabHelper} events and as an Adapter
     * for the donation amount {@link android.widget.ListView}.
     *
     * @param activity  Activity context.
     * @param iabHelper Google in-app purchase helper class.
     * @param itemSkus  List of item SKUs available for purchase.
     * @param resource  Layout resource used to display donation amounts.
     */
    public GoogleIAPHandler(@NonNull Activity activity, @NonNull IabHelper iabHelper,
                            @LayoutRes int resource, @NonNull List<String> itemSkus,
                            @NonNull Listener listener) {
        super(activity, resource);

        this.activity = activity;
        this.iabHelper = iabHelper;
        this.itemSkus = itemSkus;
        this.listener = listener;
    }
    //endregion

    //region ArrayAdapter<Pair<String, String>> methods
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Create the list view item.
        View view = convertView;

        if (view == null) {
            view = LayoutInflater.from(getContext())
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
        }

        // Get SKU details.
        Pair<String, String> skuPair = getItem(position);

        // Populate the views.
        TextView textView = (TextView) view.findViewById(android.R.id.text1);
        textView.setText(skuPair.second);

        return view;
    }
    //endregion

    //region IabHelper.OnIabSetupFinishedListener methods

    /**
     * Called to notify that setup is complete.
     *
     * @param result The result of the setup process.
     */
    @Override
    public void onIabSetupFinished(IabResult result) {
        if (result.isSuccess()) {
            if (iabHelper == null) return;

            try {
                // Query the prices of IAP items and display them in the ListView.
                iabHelper.queryInventoryAsync(true, itemSkus, null, this);
                return;
            } catch (IabHelper.IabAsyncInProgressException e) {
                listener.onPurchaseError(e);
                return;
            }
        }
        listener.onPurchaseError(null);
    }
    //endregion

    //region IabHelper.QueryInventoryFinishedListener methods

    /**
     * Called to notify that an inventory query operation completed.
     *
     * @param result The result of the operation.
     * @param inv    The inventory.
     */
    @Override
    public void onQueryInventoryFinished(IabResult result, Inventory inv) {
        if (result.isSuccess()) {
            if (iabHelper == null) return;
            // Populate adapter with SKU details.
            for (String sku : itemSkus) {
                if (inv.hasDetails(sku)) {
                    add(new Pair<>(sku, inv.getSkuDetails(sku).getPrice()));
                }
            }
            notifyDataSetChanged();
            return;
        }
        listener.onPurchaseError(null);
    }
    //endregion

    //region IabHelper.OnIabPurchaseFinishedListener methods

    /**
     * Called to notify that an in-app purchase finished. If the purchase was successful,
     * then the sku parameter specifies which item was purchased. If the purchase failed,
     * the sku and extraData parameters may or may not be null, depending on how far the purchase
     * process went.
     *
     * @param result The result of the purchase.
     * @param info   The purchase information (null if purchase failed)
     */
    @Override
    public void onIabPurchaseFinished(IabResult result, Purchase info) {
        if (iabHelper == null) return;

        if (result.isSuccess()) {
            // Consume the purchase, so that the user can donate multiple times.
            try {
                iabHelper.consumeAsync(info, this);
            } catch (IabHelper.IabAsyncInProgressException e) {
                e.printStackTrace();
            }

            listener.onPurchaseComplete(info);
        }
    }
    //endregion

    //region IabHelper.OnConsumeFinishedListener

    /**
     * Called to notify that a consumption has finished.
     *
     * @param purchase The purchase that was (or was to be) consumed.
     * @param result   The result of the consumption operation.
     */
    @Override
    public void onConsumeFinished(Purchase purchase, IabResult result) {
        // Do nothing.
    }
    //endregion

    //region AdapterView.OnItemClickListener methods
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        final String sku = getItem(position).first;

        try {
            // Launch Google Play purchase flow.
            iabHelper.launchPurchaseFlow(activity, sku, 0, this);
        } catch (IabHelper.IabAsyncInProgressException e) {
            listener.onPurchaseError(e);
        }
    }
    //endregion

    //region Listener interface

    /**
     * Interface used to handle user interaction with the ListView and purchase events.
     */
    public interface Listener {

        /**
         * Called when an error occurs while interacting with the Google in-app purchase service.
         *
         * @param error Error that occurred, can be null if not known.
         */
        public void onPurchaseError(@Nullable Exception error);

        /**
         * Called when the Google in-app purchase is completed.
         *
         * @param info Information about the purchase.
         */
        public void onPurchaseComplete(Purchase info);
    }
    //endregion
}
