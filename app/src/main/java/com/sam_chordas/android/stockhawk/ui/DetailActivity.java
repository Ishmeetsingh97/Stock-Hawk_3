package com.sam_chordas.android.stockhawk.ui;

import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.db.chart.model.LineSet;
import com.db.chart.view.LineChartView;
import com.sam_chordas.android.stockhawk.R;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

public class DetailActivity extends AppCompatActivity {

    private static String LOG_TAG = DetailActivity.class.getSimpleName();

    private OkHttpClient client = new OkHttpClient();

    String symbol;
    TextView tvStockSymbol;
    LineChartView lineChartView;
    boolean isConnected;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent upIntent = NavUtils.getParentActivityIntent(this);
                if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
                    TaskStackBuilder.create(this)
                            .addNextIntentWithParentStack(upIntent)
                            .startActivities();
                } else {
                    NavUtils.navigateUpTo(this, upIntent);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    String fetchData(String url) throws IOException {

        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    public void networkToast(){
        Toast.makeText(this, getString(R.string.network_toast), Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_detail);

        ConnectivityManager cm =
                (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        if (isConnected) {
            Intent intent = getIntent();
            if ( intent.hasExtra("SYMBOL") ) {
                symbol = intent.getStringExtra("SYMBOL");
                tvStockSymbol = (TextView) findViewById(R.id.stock_symbol_textview);
                tvStockSymbol.setText(symbol);

                DownloadQuoteDetailsTask quoteDetailsTask = new DownloadQuoteDetailsTask();
                quoteDetailsTask.execute(symbol);
            }
        } else {
            networkToast();
        }

    }

    private class DownloadQuoteDetailsTask extends AsyncTask<String, Void, BuildChart> {
        @Override
        protected BuildChart doInBackground(String... params) {

            String stockInput = params[0];

            StringBuilder urlStringBuilder = new StringBuilder();

            try{
                urlStringBuilder.append("http://chartapi.finance.yahoo.com/instrument/1.0/");
                urlStringBuilder.append(URLEncoder.encode(stockInput, "UTF-8"));
                urlStringBuilder.append("/chartdata;type=close;range=1y/json");

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            String urlString;
            urlString = urlStringBuilder.toString();

            String getResponse = "";

            try {
                getResponse = fetchData(urlString);
            } catch (IOException e){
                e.printStackTrace();
            }

            int startIndex = "finance_charts_json_callback( ".length();
            int endIndex = getResponse.length() - 1;

            getResponse = getResponse.substring(startIndex, endIndex);

            Log.d(LOG_TAG, getResponse);

            JSONObject jsonObject = null;
            JSONArray resultsArray = null;

            BuildChart buildChart = new BuildChart();
            float[] results;

            try {
                jsonObject = new JSONObject(getResponse);

                jsonObject = jsonObject.getJSONObject("ranges").getJSONObject("close");

                buildChart.setMinimum(jsonObject.getString("min"));
                buildChart.setMaximum(jsonObject.getString("max"));

                jsonObject = new JSONObject(getResponse);
                resultsArray = jsonObject.getJSONArray("series");

                if (resultsArray != null && resultsArray.length() != 0){
                    for (int i = 0; i < resultsArray.length(); i++){
                        jsonObject = resultsArray.getJSONObject(i);
                        buildChart.addDataPoint(jsonObject);
                    }
                }

            } catch (JSONException e){
                Log.e(LOG_TAG, "String to JSON failed: " + e);
            }

            return buildChart;
        }

        @Override
        protected void onPostExecute(BuildChart buildChart) {
            super.onPostExecute(buildChart);

            LineSet dataset = new LineSet(buildChart.getLabels(), buildChart.getPoints());

            dataset.setColor(Color.parseColor("#FFFFFF"))
                    .setFill(Color.parseColor("#AAAAAA"))
                    .setThickness(1)
                    .setDashed(new float[]{10f,10f})
                    .beginAt(0);

            lineChartView = (LineChartView) findViewById(R.id.linechart);
            lineChartView.addData(dataset);

            int step = 1;

            if (buildChart.getMaximum() > 100) {
                step = 100;
            } else if (buildChart.getMaximum() > 1) {
                step = 10;
            } else {
                step = 1;
            }

            lineChartView.setAxisBorderValues(buildChart.getMinimum(), buildChart.getMaximum(), step);
            lineChartView.setAxisColor(Color.parseColor("#FFFFFF"));
            lineChartView.setLabelsColor(Color.parseColor("#FFFFFF"));
            lineChartView.show();

        }
    }

    public class BuildChart {

        ArrayList<String> mLabel;
        ArrayList<Float> mValue;
        int mMinimum;
        int mMaximum;

        public BuildChart() {
            mLabel = new ArrayList<String>();
            mValue = new ArrayList<Float>();
        }

        public void addDataPoint(JSONObject jsonObject) {
            try {
                String[] months = new String[]{"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sept", "Oct", "Nov", "Dec"};
                String close = jsonObject.getString("close");
                String closeDate = jsonObject.getString("Date");
                int currentMonth = Integer.valueOf(closeDate.substring(4,6))-1;
                mLabel.add(months[currentMonth]);
                mValue.add(Float.valueOf(close));
            } catch (JSONException e){
                Log.e(LOG_TAG, "String to JSON failed: " + e);
            }
        }

        public String[] getLabels() {
            String[] mResults = new String[mLabel.size()];
            mResults = mLabel.toArray(mResults);

            String previousLabel = mResults[0];

            for(int i = 1; i < mResults.length - 1; i++) {
                if (previousLabel.equalsIgnoreCase(mResults[i])) {
                    mResults[i] = "";
                } else {
                    previousLabel =  mResults[i];
                }
            }

            return mResults;
        }

        public float[] getPoints() {
            float[] mResults = new float[mValue.size()];
            int i = 0;

            for(Float f : mValue) {
                mResults[i++] = (f != null ? f : f.NaN);
            }

            return mResults;
        }

        public void setMinimum(String minimum) {

            Integer result = 0;

            try {
                String value = minimum.substring(0, minimum.indexOf("."));
                result = Integer.valueOf(value);
            } catch (NumberFormatException e) {
                Log.e(LOG_TAG, e.toString());
            }

            if ( result < 100 ) {
                mMinimum = 0;
            } else {
                int length = minimum.indexOf(".") - 1;
                Integer firstDigit = Integer.valueOf(minimum.substring(0,1));
                mMinimum = (int)(firstDigit * (Math.pow((double)10.0, (double)length))) ;
            }
        }

        public void setMaximum(String maximum) {

            Integer result = 1000;

            try {
                String value = maximum.substring(0, maximum.indexOf("."));
                result = Integer.valueOf(value);
            } catch (NumberFormatException e) {
                Log.e(LOG_TAG, e.toString());
            }

            int length = maximum.indexOf(".") - 1;
            Integer firstDigit = Integer.valueOf(maximum.substring(0, 1));
            mMaximum = (int)((firstDigit+1) * (Math.pow((double)10.0, (double)length)));
        }

        public int getMinimum() {
            return mMinimum;
        }

        public int getMaximum() {
            return mMaximum;
        }
    }
}
