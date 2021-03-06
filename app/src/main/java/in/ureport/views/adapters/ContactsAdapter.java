package in.ureport.views.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

import in.ureport.R;
import in.ureport.managers.PrototypeManager;
import in.ureport.models.Contact;

/**
 * Created by johncordeiro on 19/07/15.
 */
public class ContactsAdapter extends RecyclerView.Adapter {

    private List<Contact> contacts;

    private OnContactInvitedListener onContactInvitedListener;

    public ContactsAdapter(List<Contact> contacts) {
        this.contacts = contacts;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return new ViewHolder(inflater.inflate(R.layout.item_contact_invite, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ((ViewHolder)holder).bindView(contacts.get(position));
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    private class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView name;
        private final TextView phone;

        public ViewHolder(View itemView) {
            super(itemView);

            name = (TextView) itemView.findViewById(R.id.name);
            phone = (TextView) itemView.findViewById(R.id.phone);

            Button invite = (Button) itemView.findViewById(R.id.invite);
            invite.setOnClickListener(onInviteContactClickListener);
        }

        private void bindView(Contact contact) {
            name.setText(contact.getName());
            phone.setText(contact.getPhoneNumber());
        }

        private View.OnClickListener onInviteContactClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(onContactInvitedListener != null)
                    onContactInvitedListener.onContactInvited(contacts.get(getLayoutPosition()));
            }
        };
    }

    public void setOnContactInvitedListener(OnContactInvitedListener onContactInvitedListener) {
        this.onContactInvitedListener = onContactInvitedListener;
    }

    public interface OnContactInvitedListener {
        void onContactInvited(Contact contact);
    }
}
