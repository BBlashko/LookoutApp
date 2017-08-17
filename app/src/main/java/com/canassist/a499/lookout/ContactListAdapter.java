package com.canassist.a499.lookout;

import android.app.Activity;
import android.content.Context;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

public class ContactListAdapter extends BaseAdapter implements ListAdapter {

    private ArrayList<String> mPhoneList = new ArrayList<>();
    private Context mContext;
    private LookoutConfig mLookoutConfig;

    public ContactListAdapter(ArrayList<String> list, Context context, LookoutConfig lookoutConfig) {
        mPhoneList = list;
        mContext = context;
        mLookoutConfig = lookoutConfig;
    }

    @Override
    public int getCount() {
        return mPhoneList.size();
    }

    @Override
    public Object getItem(int i) {
        return mPhoneList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    public void setNumber(int i, String number, boolean sendToLookout) {
        if (mPhoneList.size() == 0) {
            createNewNumberField();
        }
        mPhoneList.set(i, number);
        mLookoutConfig.addNumber(number, sendToLookout);
    }

    public void setAll(JSONArray array) {
        mPhoneList.clear();
        mLookoutConfig.clearNumbers(false);
        for (int i = 0; i < array.length(); i++) {
            try {
                mPhoneList.add(array.getString(i));
                mLookoutConfig.addNumber(array.getString(i), false);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        mLookoutConfig.sendToLookout();
        notifyDataSetChanged();
    }

    public void removeNumber(int i) {
        if (i <  mPhoneList.size()) {
            mPhoneList.remove(i);
            mLookoutConfig.removeNumber(i, true);
        }
    }

    public void clear() {
        mPhoneList.clear();
    }

    @Override
    public View getView(final int i, final View inView, final ViewGroup viewGroup) {
        View view = inView;

        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.contact_entry, null);
        }

        //Handle TextView and display string from your list
        final EditText listItemText = (EditText)view.findViewById(R.id.phone_number_edit);
        listItemText.requestFocus();
        listItemText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int keyCode, KeyEvent keyEvent) {
                if (keyCode == EditorInfo.IME_ACTION_DONE) {
                    String number = listItemText.getText().toString();
                    if (number.length() == 10 || (number.length() == 11 && number.charAt(0) == '1')){
                        if (number.length() == 11) {
                            number = number.substring(1);
                        }
                        setNumber(i, number, true);
                        listItemText.setText(mPhoneList.get(i));
                        listItemText.clearFocus();
                        keyboardToggle((Activity) mContext);
                        notifyDataSetChanged();
                    } else {
                        Toast.makeText(mContext, "Invalid phone number", Toast.LENGTH_SHORT).show();
                        listItemText.setText("");
                        listItemText.setFocusable(true);
                    }
                    return true;
                }
                return false;
            }
        });
        listItemText.setText(mPhoneList.get(i));

        //Handle buttons and add onClickListeners
        ImageButton deleteBtn = (ImageButton)view.findViewById(R.id.delete_btn);

        deleteBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                removeNumber(i);
                notifyDataSetChanged();
            }
        });
        return view;
    }


    public static void keyboardToggle(Activity activity){
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (imm.isActive()){
            imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0); // hide
        } else {
            imm.toggleSoftInput(0, InputMethodManager.HIDE_IMPLICIT_ONLY); // show
        }
    }

    public void createNewNumberField() {
        mPhoneList.add("");
        notifyDataSetChanged();

    }
}
