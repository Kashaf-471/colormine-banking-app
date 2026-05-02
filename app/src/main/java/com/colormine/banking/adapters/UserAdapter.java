package com.colormine.banking.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import com.colormine.banking.R;
import com.colormine.banking.models.User;
import java.util.List;

public class UserAdapter extends BaseAdapter {
    private Context context;
    private List<User> userList;
    private OnUserActionListener listener;

    public interface OnUserActionListener {
        void onEdit(User user);
        void onDelete(User user);
        void onView(User user);
    }

    public UserAdapter(Context context, List<User> userList, OnUserActionListener listener) {
        this.context = context;
        this.userList = userList;
        this.listener = listener;
    }

    @Override
    public int getCount() {
        return userList.size();
    }

    @Override
    public Object getItem(int position) {
        return userList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return userList.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        }

        User user = userList.get(position);

        TextView txtName = convertView.findViewById(R.id.txt_user_name);
        TextView txtEmail = convertView.findViewById(R.id.txt_user_email);
        TextView txtBalance = convertView.findViewById(R.id.txt_user_balance);
        Button btnView = convertView.findViewById(R.id.btn_view_user_data);
        Button btnEdit = convertView.findViewById(R.id.btn_edit_user);
        Button btnDelete = convertView.findViewById(R.id.btn_delete_user);

        txtName.setText(user.getName());
        txtEmail.setText(user.getEmail());
        txtBalance.setText(String.format("Balance: $%.2f", user.getBalance()));

        btnView.setOnClickListener(v -> listener.onView(user));
        btnEdit.setOnClickListener(v -> listener.onEdit(user));
        btnDelete.setOnClickListener(v -> listener.onDelete(user));

        return convertView;
    }
}
