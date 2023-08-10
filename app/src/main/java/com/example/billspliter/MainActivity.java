package com.example.billspliter;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String PREF_NAME = "BillBreakdownPrefs";
    private static final String KEY_TOTAL_BILL = "totalBill";
    private static final String KEY_NUMBER_OF_PEOPLE = "numberOfPeople";
    private static final String KEY_BREAKDOWN_OPTION = "breakdownOption";

    private EditText totalBillEditText, numberOfPeopleEditText;
    private RadioGroup breakdownOptionRadioGroup;
    private Button calculateButton, shareButton;
    private TextView resultTextView;

    private double totalBill;
    private int numberOfPeople;
    private String breakdownOption;
    private double amountPerPerson;

    private ArrayList<EditText> percentageEditTexts;
    private ArrayList<EditText> amountEditTexts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        totalBillEditText = findViewById(R.id.totalBillEditText);
        numberOfPeopleEditText = findViewById(R.id.numberOfPeopleEditText);
        resultTextView = findViewById(R.id.resultTextView);
        breakdownOptionRadioGroup = findViewById(R.id.breakdownOptionRadioGroup);
        calculateButton = findViewById(R.id.calculateButton);
        shareButton = findViewById(R.id.shareButton);

        breakdownOptionRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                onBreakdownOptionChanged();
            }
        });

        calculateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calculateBillBreakdown();
            }
        });

        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareBillBreakdown();
            }
        });

        loadPreferences();
    }

    private void calculateBillBreakdown() {
        totalBill = Double.parseDouble(totalBillEditText.getText().toString());
        numberOfPeople = Integer.parseInt(numberOfPeopleEditText.getText().toString());

        int selectedOptionId = breakdownOptionRadioGroup.getCheckedRadioButtonId();
        RadioButton selectedOptionRadioButton = findViewById(selectedOptionId);
        breakdownOption = selectedOptionRadioButton.getText().toString();

        // Initialize the percentageEditTexts and amountEditTexts ArrayLists
        percentageEditTexts = new ArrayList<>();
        amountEditTexts = new ArrayList<>();

        switch (breakdownOption) {
            case "Equal":
                amountPerPerson = totalBill / numberOfPeople;
                break;
            case "Custom by Percentage":
                if (numberOfPeople > 0) {
                    showPercentageLayout();
                } else {
                    Toast.makeText(this, "Please enter the number of people first", Toast.LENGTH_SHORT).show();
                    return;
                }
                break;
            case "Custom by Amount":
                if (numberOfPeople > 0) {
                    showAmountLayout();
                } else {
                    Toast.makeText(this, "Please enter the number of people first", Toast.LENGTH_SHORT).show();
                    return;
                }
                break;
            default:
                Toast.makeText(this, "Invalid breakdown option", Toast.LENGTH_SHORT).show();
                return;
        }

        switch (breakdownOption) {
            case "Equal":
                // No need to do anything, as amountPerPerson is already calculated above
                break;
            case "Custom by Percentage":
                calculateCustomByPercentage();
                break;
            case "Custom by Amount":
                calculateCustomByAmount();
                break;
        }

        DecimalFormat decimalFormat = new DecimalFormat("#.##");
        String formattedAmount = decimalFormat.format(amountPerPerson);

        resultTextView.setText(String.format("Each person needs to pay: RM %s", formattedAmount));

        savePreferences();
    }



    private void calculateCustomByPercentage() {
        double[] percentages = new double[numberOfPeople];
        double totalPercentage = 0;

        for (int i = 0; i < numberOfPeople; i++) {
            EditText percentageEditText = percentageEditTexts.get(i);
            String percentageStr = percentageEditText.getText().toString().trim();

            if (percentageStr.isEmpty()) {
                Toast.makeText(this, "Please enter all percentages", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                percentages[i] = Double.parseDouble(percentageStr);
                totalPercentage += percentages[i];
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid percentage format", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (totalPercentage != 100) {
            Toast.makeText(this, "Total percentage must be equal to 100%", Toast.LENGTH_SHORT).show();
            return;
        }

        double totalPercentageAmount = totalBill * (totalPercentage / 100.0);

        for (int i = 0; i < numberOfPeople; i++) {
            percentages[i] = percentages[i] / 100.0;
            double individualAmount = totalPercentageAmount * percentages[i];
            DecimalFormat decimalFormat = new DecimalFormat("#.##");
            String formattedAmount = decimalFormat.format(individualAmount);
            EditText amountEditText = amountEditTexts.get(i);
            amountEditText.setText(formattedAmount);
        }
    }

    private void calculateCustomByAmount() {
        double[] amounts = new double[numberOfPeople];

        for (int i = 0; i < numberOfPeople; i++) {
            EditText amountEditText = amountEditTexts.get(i);
            String amountStr = amountEditText.getText().toString().trim();

            if (amountStr.isEmpty()) {
                Toast.makeText(this, "Please enter all amounts", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                amounts[i] = Double.parseDouble(amountStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid amount format", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        double totalAmount = 0;
        for (double amount : amounts) {
            totalAmount += amount;
        }

        if (totalAmount != totalBill) {
            Toast.makeText(this, "Total amount must be equal to the total bill", Toast.LENGTH_SHORT).show();
            return;
        }

        amountPerPerson = totalBill / numberOfPeople;
    }


    private void onBreakdownOptionChanged() {
        int selectedOptionId = breakdownOptionRadioGroup.getCheckedRadioButtonId();

        switch (selectedOptionId) {
            case R.id.equalRadioButton:
                hidePercentageLayout();
                hideAmountLayout();
                break;
            case R.id.customPercentageRadioButton:
                hideAmountLayout();
                showPercentageLayout();
                break;
            case R.id.customAmountRadioButton:
                hidePercentageLayout();
                showAmountLayout();
                break;
        }
    }


    private void showPercentageLayout() {
        percentageEditTexts = new ArrayList<>(); // Initialize the ArrayList for percentages
        LinearLayout percentageLayout = findViewById(R.id.percentageLayout);
        percentageLayout.setVisibility(View.VISIBLE);
        percentageLayout.removeAllViews(); // Clear existing EditTexts if any

        for (int i = 0; i < numberOfPeople; i++) {
            EditText percentageEditText = new EditText(this);
            percentageEditText.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            percentageEditText.setHint("Enter percentage for Person " + (i + 1));
            percentageEditText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            percentageEditTexts.add(percentageEditText);
            percentageLayout.addView(percentageEditText);
        }
    }

    private void showAmountLayout() {
        amountEditTexts = new ArrayList<>(); // Initialize the ArrayList for amounts
        LinearLayout amountLayout = findViewById(R.id.amountLayout);
        amountLayout.setVisibility(View.VISIBLE);
        amountLayout.removeAllViews(); // Clear existing EditTexts if any

        for (int i = 0; i < numberOfPeople; i++) {
            EditText amountEditText = new EditText(this);
            amountEditText.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            amountEditText.setHint("Enter amount for Person " + (i + 1));
            amountEditText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            amountEditTexts.add(amountEditText);
            amountLayout.addView(amountEditText);
        }
    }

    private void hidePercentageLayout() {
        LinearLayout percentageLayout = findViewById(R.id.percentageLayout);
        percentageLayout.setVisibility(View.GONE);
        percentageEditTexts = null;
    }

    private void hideAmountLayout() {
        LinearLayout amountLayout = findViewById(R.id.amountLayout);
        amountLayout.setVisibility(View.GONE);
        amountEditTexts = null;
    }

    private void shareBillBreakdown() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Bill Breakdown");
        shareIntent.putExtra(Intent.EXTRA_TEXT, getBillBreakdownMessage());
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }

    private String getBillBreakdownMessage() {
        StringBuilder message = new StringBuilder();
        message.append("Total Bill: RM ").append(totalBill).append("\n");
        message.append("Number of People: ").append(numberOfPeople).append("\n");
        message.append("Breakdown Option: ").append(breakdownOption).append("\n");
        message.append("Amount per Person: RM ").append(amountPerPerson).append("\n");
        return message.toString();
    }

    private void loadPreferences() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        totalBill = prefs.getFloat(KEY_TOTAL_BILL, 0);
        numberOfPeople = prefs.getInt(KEY_NUMBER_OF_PEOPLE, 0);
        breakdownOption = prefs.getString(KEY_BREAKDOWN_OPTION, "");

        totalBillEditText.setText(String.valueOf(totalBill));
        numberOfPeopleEditText.setText(String.valueOf(numberOfPeople));

        RadioButton radioButton;
        switch (breakdownOption) {
            case "Equal":
                radioButton = findViewById(R.id.equalRadioButton);
                break;
            case "Custom by Percentage":
                radioButton = findViewById(R.id.customPercentageRadioButton);
                break;
            case "Custom by Amount":
                radioButton = findViewById(R.id.customAmountRadioButton);
                break;
            default:
                radioButton = findViewById(R.id.equalRadioButton);
                break;
        }
        radioButton.setChecked(true);
    }

    private void savePreferences() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat(KEY_TOTAL_BILL, (float) totalBill);
        editor.putInt(KEY_NUMBER_OF_PEOPLE, numberOfPeople);
        editor.putString(KEY_BREAKDOWN_OPTION, breakdownOption);
        editor.apply();
    }

    private int getPercentageEditTextId(int index) {
        return 1000 + index;
    }

    private int getAmountEditTextId(int index) {
        return 2000 + index;
    }
}