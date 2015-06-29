package com.makeandbuild.worldcurrency;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.makeandbuild.worldcurrency.domain.RateConversion;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {

    private ProgressDialog progressDialog;

    String[] currencyNames = {"USD", "GBP", "EUR", "JPY", "BRL"};

    String[] currencyDescriptions = {"US Dollar", "UK Pounds", "EU Euro", "Japan Yen", "Brazil Reais"};

    int images[] = {R.drawable.usd, R.drawable.gbp, R.drawable.eur,
            R.drawable.jpy, R.drawable.brl};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        Spinner sourceSpanner = (Spinner) findViewById(R.id.currencySourceSpinner);
        CurrencyAdapter currencySourceAdapter = new CurrencyAdapter(MainActivity.this, R.layout.spinner_layout, currencyNames);
        sourceSpanner.setAdapter(currencySourceAdapter);
    }

    /**
     * Called when user clicks Convert button
     * @param view
     */
    public void onCheckConversion(View view) {

        Spinner sourceSpinner = (Spinner) findViewById(R.id.currencySourceSpinner);
        String selectedSourceCurrency = sourceSpinner.getSelectedItem().toString();

        TextView amountTextView = (TextView) findViewById(R.id.amount);
        String amountStr = amountTextView.getText().toString();
        if(amountStr == null || amountStr.length() == 0) {
            showErrorDialog("Please enter amount.");
            return;
        }

        showProgressDialog("Loading currency rates...");
        BigDecimal amount = new BigDecimal(amountStr);

        RequestQueue queque = Volley.newRequestQueue(this);
        String url = "http://api.fixer.io/latest?base=" + selectedSourceCurrency;

        FixerListener fixerListener = new FixerListener(selectedSourceCurrency, amount, currencyNames);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, fixerListener, fixerListener);

        // Add the request to the RequestQueue.
        queque.add(stringRequest);
    }

    public void showProgressDialog(CharSequence message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setIndeterminate(true);
        }

        progressDialog.setMessage(message);
        progressDialog.show();
    }

    public void showErrorDialog(CharSequence message) {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        builder.setMessage(message).setTitle("Error");

        final AlertDialog errorDialog = builder.create();
        errorDialog.show();

        new Handler().postDelayed(new Runnable() {

            // Showing message with a timer.
            @Override
            public void run() {
                errorDialog.hide();
            }
        }, 4000);
    }

    public void dismissProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    /**
     * Make HTTP GET request to Fixer.io for conversion rates
     */
    public class FixerListener implements Response.Listener<String>, Response.ErrorListener {

        String selectedSourceCurrency;
        BigDecimal amount;
        String[] currencyNames;

        public FixerListener(String selectedSourceCurrency, BigDecimal amount, String[] currencyNames) {
            this.selectedSourceCurrency = selectedSourceCurrency;
            this.amount = amount;
            this.currencyNames = currencyNames;
        }

        @Override
        public void onResponse(String response) {

            try {
                Map<String, String> rateConversions = processResponse(response);
                List<RateConversion> rateConversionList = convertRates(rateConversions, amount, currencyNames);

                ListView ratesListView = (ListView) findViewById(R.id.convertedRates);
                RatesListAdapter ratesListAdapter = new RatesListAdapter(MainActivity.this, rateConversionList);
                ratesListView.setAdapter(ratesListAdapter);

                dismissProgressDialog();
            } catch (Exception e) {
                showErrorDialog("Error processing conversion rate data");
            }
        }

        public List<RateConversion> convertRates(Map<String, String> rateConversions, BigDecimal amount, String[] currencyNames) {
            List<RateConversion> convertedRates = new ArrayList<>();

            for (int x = 0; x < currencyNames.length; x++) {
                String targetCurrency = currencyNames[x];
                if (!selectedSourceCurrency.equalsIgnoreCase(targetCurrency)) {
                    String conversionRate = rateConversions.get(targetCurrency);
                    if (conversionRate != null && conversionRate.length() > 0) {
                        BigDecimal conversionRateFloat = new BigDecimal(conversionRate);
                        BigDecimal convertedRate = conversionRateFloat.multiply(amount);
                        BigDecimal convertedRounded = convertedRate.setScale(2, BigDecimal.ROUND_HALF_EVEN);

                        RateConversion rateConversion = new RateConversion();
                        rateConversion.setConvertedAmount(convertedRounded.toString());
                        rateConversion.setCurrencyDescription(currencyDescriptions[x]);
                        rateConversion.setCurrencyName(targetCurrency);
                        rateConversion.setImageId(images[x]);

                        convertedRates.add(rateConversion);
                    }
                }
            }
            return convertedRates;
        }

        /**
         * Take the JSON response from fixer.io and convert to map of currencies and conversion rate
         * @param response
         * @return
         * @throws JSONException
         */
        public Map<String, String> processResponse(String response) throws JSONException {
            Map<String, String> rateConversions = new HashMap<>();
            JSONObject jsonObject = new JSONObject(response);
            JSONObject ratesJson = jsonObject.getJSONObject("rates");

            for (int x = 0; x < ratesJson.length(); x++) {
                String key = null;
                String value = null;
                Iterator itr = ratesJson.keys();
                while (itr.hasNext()) {
                    key = (String) itr.next();
                    if (key != null) {
                        value = ratesJson.getString(key);
                        rateConversions.put(key, value);
                    }
                }
            }
            return rateConversions;
        }

        @Override
        public void onErrorResponse(VolleyError error) {
            showErrorDialog("Error getting conversion rate data from fixer.io");
        }
    }

    /**
     * Spinner adapter for currency selection
     */
    public class CurrencyAdapter extends ArrayAdapter<String> {

        LayoutInflater inflater;
        int groupid;

        public CurrencyAdapter(Context context, int groupid, String[] objects) {
            super(context, groupid, objects);

            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.groupid = groupid;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }

        public View getCustomView(int position, View convertView, ViewGroup parent) {

            LayoutInflater inflater = getLayoutInflater();
            View row = inflater.inflate(R.layout.spinner_layout, parent, false);
            TextView tvName = (TextView) row.findViewById(R.id.name);
            tvName.setText(currencyNames[position]);

            TextView tvDesc = (TextView) row.findViewById(R.id.desc);
            tvDesc.setText(currencyDescriptions[position]);

            ImageView icon = (ImageView) row.findViewById(R.id.image);
            icon.setImageResource(images[position]);

            return row;
        }
    }

    /**
     * Creates rows for the conversion results
     */
    public class RatesListAdapter extends ArrayAdapter<RateConversion> {

        private Activity activity;
        private List<RateConversion> rateConversions;

        public RatesListAdapter(Activity activity, List<RateConversion> rateConversions) {
            super(activity, R.layout.rates_row, rateConversions);
            this.activity = activity;
            this.rateConversions = rateConversions;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            // Create inflater
            LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            //RateConversion rateConversion = rateConversions.get(position);

            RateConversion rateConversion = getItem(position);

            // Get row from inflater
            View rowView = inflater.inflate(R.layout.rates_row, parent, false);

            TextView tvAmount = (TextView) rowView.findViewById(R.id.amount);
            tvAmount.setText(rateConversion.getConvertedAmount());

            TextView tvName = (TextView) rowView.findViewById(R.id.name);
            tvName.setText(rateConversion.getCurrencyName());

            TextView tvDesc = (TextView) rowView.findViewById(R.id.desc);
            tvDesc.setText(rateConversion.getCurrencyDescription());

            ImageView icon = (ImageView) rowView.findViewById(R.id.image);
            icon.setImageResource(rateConversion.getImageId());

            return rowView;
        }

    }
}

