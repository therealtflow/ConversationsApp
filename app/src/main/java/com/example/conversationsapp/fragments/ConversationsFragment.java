package com.example.conversationsapp.fragments;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.example.conversationsapp.R;
import com.example.conversationsapp.databinding.FragmentConversationsBinding;
import com.example.conversationsapp.databinding.RowItemConversationBinding;
import com.example.conversationsapp.models.Conversation;
import com.example.conversationsapp.models.Message;
import com.example.conversationsapp.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Firebase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;


public class ConversationsFragment extends Fragment {

    //declares all global variables
    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    FragmentConversationsBinding binding;
    ArrayList<Conversation> mConversations = new ArrayList<Conversation>();
    ListenerRegistration listenerRegistration, messageRegistration, userRegistration;

    ArrayList<User> mUsers = new ArrayList<>();

    User mainUser = null;
    User mReceiver = null;
    User mSender = null;

    public ConversationsFragment() {
        // Required empty public constructor
    }

    //adapter
    ConversationsAdapter adapter;

    //onCreate, allows for options menu
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }


    //creates option menu and gives the correct layout
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.main_menu, menu);
    }

    //creates all the routes to be used in the options menu
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.action_logout){
            mListener.logout();
            return true;
        } if (item.getItemId() == R.id.action_blocked){
            mListener.goToBlocked();
            return true;
        } if (item.getItemId() == R.id.action_new){
            mListener.goToCreateConversation();
            return true;
        } if (item.getItemId() == R.id.action_users){
            mListener.goToUsers();
        }

        return super.onOptionsItemSelected(item);

    }

    //creates onDestroyView
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(listenerRegistration != null){
            listenerRegistration.remove();
        }
        if(messageRegistration != null){
            messageRegistration.remove();
        }
        if(userRegistration != null){
            userRegistration.remove();
        }
    }

    //sets binding
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentConversationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    //sets recyclerview, adapter, title of activity, and gets conversations
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ConversationsAdapter();
        binding.recyclerView.setAdapter(adapter);
        getActivity().setTitle("Conversations");


        //TODO: This could be where the error is caused (error: onCreated all values N/A)
        //makes sure current user is not null and gets conversations
        if(mAuth.getCurrentUser() == null){
            mListener.logout();
        } else {
            getConversations();
        }
        Log.d("DEMO", "onViewCreated: mConversations: " + mConversations.toString());
    }


    //gets conversations
    void getConversations(){
        if(listenerRegistration != null){
            listenerRegistration.remove();
        }
        if(userRegistration != null){
            userRegistration.remove();
        }
        userRegistration = db.collection("users").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if(error != null ){
                    error.printStackTrace();
                }

                mUsers.clear();
                for (QueryDocumentSnapshot doc:value) {
                    User tempUser = doc.toObject(User.class);
                    mUsers.add(tempUser);
                    Log.d("demo1", "onEvent: "+ tempUser);
                    if(tempUser.getUserId().equals(mAuth.getUid())){
                        mainUser = tempUser;
                    }
                }

            }
        });
        //registration for conversations
        listenerRegistration = db.collection("users").document(mAuth.getUid())
                .collection("conversations").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if(error != null){
                    error.printStackTrace();
                    return;
                }
                mConversations.clear();
                if(value != null && !value.isEmpty()){
                    for (QueryDocumentSnapshot doc : value) {

                        Conversation tempConv = doc.toObject(Conversation.class);
                        if(messageRegistration != null){
                            messageRegistration.remove();
                        }
                        //registration for messages
                        messageRegistration = db.collection("users").document(mAuth.getUid())
                                .collection("conversations").document(tempConv.getConversationId())
                                .collection("messages").addSnapshotListener(new EventListener<QuerySnapshot>() {
                            @Override
                            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                                if(error != null){
                                    error.printStackTrace();
                                    return;
                                }
                                ArrayList<Message> messages = new ArrayList<>();
                                if(value != null && !value.isEmpty()){
                                    for (QueryDocumentSnapshot doc : value) {
                                        Message m = doc.toObject(Message.class);
                                        messages.add(m);
                                    }
                                }
                                tempConv.setMessages(messages);
                                adapter.notifyDataSetChanged();
                            }
                        });

                        mConversations.add(tempConv);
                        Log.d("DEMO", "getConversations: tempConv: " + tempConv.toString());
                        Log.d("DEMO", "getConversations: mConversations: " + mConversations.toString());
                    }
                }
                adapter.notifyDataSetChanged();
            }
        });
    }

    //recyclerview setup
    class ConversationsAdapter extends RecyclerView.Adapter<ConversationsAdapter.ConversationsViewHolder> {
        @NonNull
        @Override
        public ConversationsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            RowItemConversationBinding itemBinding = RowItemConversationBinding.inflate(getLayoutInflater(), parent, false);
            return new ConversationsViewHolder(itemBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull ConversationsViewHolder holder, int position) {
            holder.setupUI(mConversations.get(position));
        }

        @Override
        public int getItemCount() {
            return mConversations.size();
        }

        class ConversationsViewHolder extends RecyclerView.ViewHolder {
            RowItemConversationBinding itemBinding;
            Conversation mConversationSingle;

            public ConversationsViewHolder(RowItemConversationBinding itemBinding) {
                super(itemBinding.getRoot());
                this.itemBinding = itemBinding;
            }

            public void setupUI(Conversation conversation) {
                this.mConversationSingle = conversation;
                Log.d("DEMO", "setupUI: MCONVERSATION: " + mConversationSingle.toString());

                for(int i = 0; i < mUsers.size(); i++){
                    if(mUsers.get(i).getUserId().equals(mConversationSingle.getSenderId())){
                        mSender = mUsers.get(i);
                    }
                    if(mUsers.get(i).getUserId().equals(mConversationSingle.getReceiverId())){
                        mReceiver = mUsers.get(i);
                    }
                }

                if(mReceiver != null && mSender != null){
                    itemBinding.textViewTo.setText(mReceiver.getUsername());
                    itemBinding.textViewFrom.setText(mSender.getUsername());
                }
                itemBinding.textViewTitle.setText(mConversationSingle.getTitle());

                if(mConversationSingle.getDate() != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm a");
                    itemBinding.textViewDate.setText(sdf.format(mConversationSingle.getDate().toDate()));
                }

                if(mConversationSingle.isNewMessages()){
                    itemBinding.textViewSeenOrSent.setText("Seen");
                } else {
                    itemBinding.textViewSeenOrSent.setText("Sent");
                }

                itemBinding.imageViewDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String idToDelete = mConversationSingle.getConversationId();
                            db.collection("users").document(mAuth.getUid())
                                    .collection("conversations").document(idToDelete).delete();
                    }
                });

                itemBinding.getRoot().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mListener.goToReplysFragment(mConversationSingle);
                    }
                });
            }
        }

    }


    ConversationListener mListener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try{
            mListener = (ConversationListener) context;
        } catch (ClassCastException e){
            throw new ClassCastException(context.toString() + " must implement ConversationListener");
        }
    }

    public interface ConversationListener {


        void logout();

        void goToUsers();

        void goToBlocked();

        void goToCreateConversation();
        void goToReplysFragment(Conversation c);
    }

}