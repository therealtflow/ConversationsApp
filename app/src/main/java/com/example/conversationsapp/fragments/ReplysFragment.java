package com.example.conversationsapp.fragments;

import android.content.Context;
import android.graphics.Color;
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
import android.widget.Toast;

import com.example.conversationsapp.R;
import com.example.conversationsapp.databinding.FragmentReplysBinding;
import com.example.conversationsapp.databinding.RowItemMessageBinding;
import com.example.conversationsapp.models.Conversation;
import com.example.conversationsapp.models.Message;
import com.example.conversationsapp.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class ReplysFragment extends Fragment {
    public ReplysFragment() {
        // Required empty public constructor
    }

    private static final String CONVERSATION = "CONVERSATION";
    private Conversation mConversation;
    ArrayList<Message> mMessages = new ArrayList<>();

    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    FragmentReplysBinding binding;
    ReplysAdapter adapter;
    ListenerRegistration listenerRegistration;
    User mReceiver, mSender;


    public static ReplysFragment newInstance(Conversation c) {
        ReplysFragment fragment = new ReplysFragment();
        Bundle args = new Bundle();
        args.putSerializable(CONVERSATION, c);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (getArguments() != null) {
            mConversation = (Conversation) getArguments().getSerializable(CONVERSATION);
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.back_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.action_cancel){
            mListener.replyBackButton();
            return true;
        }
        return super.onOptionsItemSelected(item);

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(listenerRegistration != null){
            listenerRegistration.remove();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentReplysBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    void getSender(){
        db.collection("users").document(mConversation.getSenderId()).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                if(error != null){
                    Log.d("errorcv", "onEvent: ");
                    error.printStackTrace();
                    return;
                }
                mSender = null;
                if(value != null && value.exists()){
                    User u = value.toObject(User.class);
                    mSender = u;
                    binding.textViewFrom.setText("FROM: " + mSender.getUsername());
                }
            }
        });
    }
    void getReceiver(){
        db.collection("users").document(mConversation.getReceiverId()).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                if(error != null){
                    Log.d("errorcv", "onEvent: ");
                    error.printStackTrace();
                    return;
                }
                mReceiver = null;
                if(value != null && value.exists()){
                    User u = value.toObject(User.class);
                    mReceiver = u;
                    binding.textViewTo.setText("TO: " + mReceiver.getUsername());
                }
            }
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ReplysAdapter();
        binding.recyclerView.setAdapter(adapter);
        getActivity().setTitle("Messages");

        // retrieve messages method
        getMessages();
        getSender();
        getReceiver();

        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm a");
        binding.textViewDate.setText(sdf.format(mConversation.getDate().toDate()));

        binding.textViewTitle.setText(mConversation.getTitle());

        binding.imageViewDeleteConvo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                db.collection("users").document(mAuth.getUid())
                        .collection("conversations").document(mConversation.getConversationId())
                        .delete();
                mListener.replyBackButton();
            }
        });

        binding.buttonSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String messageReceiverId = "";
                if(mConversation.getReceiverId().equals(mAuth.getUid())){
                    messageReceiverId = mConversation.getSenderId();
                } else{
                    messageReceiverId = mConversation.getReceiverId();
                }

                String nextMessage = binding.editTextReply.getText().toString();
                if(nextMessage.isEmpty()){
                    Toast.makeText(getActivity(), "Reply box must not be empty", Toast.LENGTH_SHORT).show();
                } else if(mReceiver.getBlockedUserIds().contains(mAuth.getUid())){
                    Toast.makeText(getActivity(), "This user has blocked you", Toast.LENGTH_SHORT).show();
                } else if(mSender.getBlockedUserIds().contains(messageReceiverId)){
                    Toast.makeText(getActivity(), "Cannot send message to a blocked user", Toast.LENGTH_SHORT).show();
                } else {
                    DocumentReference senderMessageRef = db.collection("users").document(mAuth.getUid()).collection("conversations").document(mConversation.getConversationId()).collection("messages").document();
                    DocumentReference receiverMessageRef = db.collection("users").document(messageReceiverId).collection("conversations").document(mConversation.getConversationId()).collection("messages").document(senderMessageRef.getId());
                    HashMap<String, Object> senderMessageData = new HashMap<>();
                    HashMap<String, Object> receiverMessageData = new HashMap<>();

                    // create message on sender's end
                    senderMessageData.put("conversationId", mConversation.getConversationId());
                    senderMessageData.put("title", mConversation.getTitle());
                    senderMessageData.put("messageText", nextMessage);
                    senderMessageData.put("mSenderId", mAuth.getUid());
                    senderMessageData.put("mReceiverId", messageReceiverId);
                    senderMessageData.put("mId", senderMessageRef.getId());
                    senderMessageData.put("sentAt", FieldValue.serverTimestamp());

                    // create message on receiver's end
                    receiverMessageData.put("conversationId", mConversation.getConversationId());
                    receiverMessageData.put("title", mConversation.getTitle());
                    receiverMessageData.put("messageText", binding.editTextReply.getText().toString());
                    receiverMessageData.put("mSenderId", mAuth.getUid());
                    receiverMessageData.put("mReceiverId", messageReceiverId);
                    receiverMessageData.put("mId", receiverMessageRef.getId());
                    receiverMessageData.put("sentAt", FieldValue.serverTimestamp());

                    senderMessageRef.set(senderMessageData);
                    receiverMessageRef.set(receiverMessageData);

//                    getMessages();
                }
            }
        });
    }

    class ReplysAdapter extends RecyclerView.Adapter<ReplysAdapter.ReplysViewHolder>{
        @NonNull
        @Override
        public ReplysViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            RowItemMessageBinding itemBinding = RowItemMessageBinding.inflate(getLayoutInflater(), parent, false);
            return new ReplysViewHolder(itemBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull ReplysViewHolder holder, int position) {
            holder.setupUI(mMessages.get(position));
        }

        @Override
        public int getItemCount() {
            return mMessages.size();
        }

        class ReplysViewHolder extends RecyclerView.ViewHolder{
            RowItemMessageBinding itemBinding;
            Message mMessage;
            User lSender, lReceiver;
            public ReplysViewHolder(RowItemMessageBinding itemBinding) {
                super(itemBinding.getRoot());
                this.itemBinding = itemBinding;
            }

            public void setupUI(Message message){
                this.mMessage = message;

                if(mMessage.getSentAt() != null){
                    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm a");
                    itemBinding.textViewDate.setText(sdf.format(mMessage.getSentAt().toDate()));
                } else {
                    itemBinding.textViewDate.setText("N/A");
                }

                if(mMessage.getmSenderId().equals(mAuth.getUid())){
                    db.collection("users").document(mAuth.getUid()).addSnapshotListener(new EventListener<DocumentSnapshot>() {
                        @Override
                        public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                            if(error != null){
                                error.printStackTrace();
                                return;
                            }
                            lSender = new User();
                            if(value != null && value.exists()){
                                lSender = value.toObject(User.class);
                            }
                            adapter.notifyDataSetChanged();
                        }
                        });
                    db.collection("users").document(mMessage.getmReceiverId()).addSnapshotListener(new EventListener<DocumentSnapshot>() {
                        @Override
                        public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                            if(error != null){
                                error.printStackTrace();
                                return;
                            }
                            lReceiver = new User();
                            if(value != null && value.exists()){
                                lReceiver = value.toObject(User.class);
                            }
                            adapter.notifyDataSetChanged();
                        }
                        });
                } else {
                    db.collection("users").document(mAuth.getUid()).addSnapshotListener(new EventListener<DocumentSnapshot>() {
                        @Override
                        public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                            if(error != null){
                                error.printStackTrace();
                                return;
                            }
                            lReceiver = new User();
                            if(value != null && value.exists()){
                                lReceiver = value.toObject(User.class);
                            }
                            adapter.notifyDataSetChanged();
                        }
                        });
                    db.collection("users").document(mMessage.getmSenderId()).addSnapshotListener(new EventListener<DocumentSnapshot>() {
                        @Override
                        public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                            if(error != null){
                                error.printStackTrace();
                                return;
                            }
                            lSender = new User();
                            if(value != null && value.exists()){
                                lSender = value.toObject(User.class);
                            }
                            adapter.notifyDataSetChanged();
                        }
                        });
                }
                if(lSender != null){
                    itemBinding.textViewFrom.setText("From: " + lSender.getUsername());
                }

                itemBinding.textViewMessageText.setText(mMessage.getMessageText());

                if(mMessage.getmSenderId().equals(mAuth.getUid())){
                    itemBinding.getRoot().setBackgroundColor(Color.parseColor("#FF03A9F4"));
                } else {
                    itemBinding.getRoot().setBackgroundColor(Color.WHITE);
                }

                itemBinding.imageViewDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d("DEMO", "onClick: IMAGE VIEW DELETE CLICKED");
                        db.collection("users").document(mAuth.getUid())
                                .collection("conversations").document(mConversation.getConversationId())
                                .collection("messages").document(mMessage.getmId()).delete();
                    }
                });


            }
        }
    }

    void getMessages(){
        if(listenerRegistration != null){
            listenerRegistration.remove();
        }
        listenerRegistration = db.collection("users").document(mAuth.getUid())
                .collection("conversations").document(mConversation.getConversationId())
                .collection("messages").orderBy("sentAt", Query.Direction.ASCENDING).addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if(error != null){
                            error.printStackTrace();
                            return;
                        }
                        mMessages.clear();
                        if(value != null && !value.isEmpty()){
                            for (QueryDocumentSnapshot doc : value) {
                                Message m = doc.toObject(Message.class);
                                mMessages.add(m);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }
    ReplysFragmentListener mListener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mListener = (ReplysFragmentListener) context;
        } catch (ClassCastException e){
            throw new ClassCastException(context.toString() + " must implement CourseReviewsListener");
        }
    }

    public interface ReplysFragmentListener{
        void replyBackButton();
    }

}