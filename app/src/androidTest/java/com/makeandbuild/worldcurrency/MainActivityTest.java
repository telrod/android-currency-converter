package com.makeandbuild.worldcurrency;

import android.test.ActivityInstrumentationTestCase2;
import android.widget.Spinner;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private MainActivity mainActivity;

    public MainActivityTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mainActivity = getActivity();
    }

    public void testDefaultCurrencyInSpinner() {
        final String expectedCurrency = "USD";

        Spinner sourceSpinner = (Spinner) mainActivity.findViewById(R.id.currencySourceSpinner);
        String selectedSourceCurrency = sourceSpinner.getSelectedItem().toString();

        assertEquals(expectedCurrency, selectedSourceCurrency);
    }


}