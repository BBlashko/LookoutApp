package com.canassist.a499.lookout;

import android.app.Activity;
import android.content.Context;
import android.text.InputType;
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

import java.util.ArrayList;

/**
 * Created by lynx on 15/07/17.
 */

public class ContactListAdapter extends BaseAdapter implements ListAdapter {

    private ArrayList<String> mPhoneList = new ArrayList<>();
    private Context mContext;
    private Activity mActivity;
    private LookoutConfig mLookoutConfig;
//    private BluetoothConnection mBluetoothConnection;

    public ContactListAdapter(ArrayList<String> list, Context context, LookoutConfig lookoutConfig) {
        mPhoneList = list;
        mContext = context;
        mLookoutConfig = lookoutConfig;
//        mBluetoothConnection = bluetoothConnection;
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

    public void setNumber(int i, String number) {
        mPhoneList.set(i, number);
        mLookoutConfig.addNumber(number);
    }

    public void removeNumber(int i) {
        if (i <  mPhoneList.size()) {
            mPhoneList.remove(i);
            mLookoutConfig.removeNumber(i);
        }
    }

    @Override
    public View getView(final int i, final View inView, ViewGroup viewGroup) {
        View view = inView;

        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.contact_entry, null);
        }

        //Handle TextView and display string from your list
        final EditText listItemText = (EditText)view.findViewById(R.id.phone_number_edit);
        listItemText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int keyCode, KeyEvent keyEvent) {
                if (keyCode == EditorInfo.IME_ACTION_DONE) {
                    String number = listItemText.getText().toString();
                    if (number.length() != 10) {
                        Toast.makeText(mContext, "Invalid phone number", Toast.LENGTH_SHORT).show();
                        listItemText.setText("");
                        listItemText.setFocusable(true);
                    } else {
                        setNumber(i, listItemText.getText().toString());
                        listItemText.setText(mPhoneList.get(i));
                        listItemText.clearFocus();
                        keyboardToggle((Activity) mContext);
                        notifyDataSetChanged();
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
