package com.example.conversationsapp.fragments;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.conversationsapp.R;
import com.example.conversationsapp.databinding.FragmentNewConversationBinding;
import com.example.conversationsapp.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.HashMap;

public class NewConversationFragment extends Fragment {
    public NewConversationFragment() {
        // Required empty public constructor
    }

    FragmentNewConversationBinding binding;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    ArrayList<String> mEmails = new ArrayList<>();
    ArrayList<User> mUsers = new ArrayList<>();
    ArrayList<String> mBlockedUserIds = new ArrayList<>();

    ListenerRegistration usersRegistration, blockedRegistration;

    User receiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentNewConversationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getActivity().setTitle("New Conversation");

        getUsers();
        getBlockedUsers();

        binding.submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //error handling
                if(binding.editTextTo.getText().toString().isEmpty()){
                    Toast.makeText(getActivity(), "Receiver email cannot be empty", Toast.LENGTH_SHORT).show();
                } else {
                    for (int i = 0; i < mUsers.size(); i++) {
                        if(mUsers.get(i) != null && mUsers.get(i).getEmail().equals(binding.editTextTo.getText().toString())){
                            getReceiver(mUsers.get(i).getUserId());
                        }
                    }
                }
                //error handling
                if(mBlockedUserIds.contains(binding.editTextTo.getText().toString())){
                    Toast.makeText(getActivity(), "Cannot send to a blocked user.", Toast.LENGTH_SHORT).show();
                } else if(!mEmails.contains(binding.editTextTo.getText().toString())){
                    Toast.makeText(getActivity(), "Receiving user does not exist.", Toast.LENGTH_SHORT).show();
                } else if(binding.editTextTitle.getText().toString().isEmpty()){
                    Toast.makeText(getActivity(), "Title cannot be empty", Toast.LENGTH_SHORT).show();
                } else if(binding.editTextMessage.getText().toString().isEmpty()){
                    Toast.makeText(getActivity(), "Message cannot be empty", Toast.LENGTH_SHORT).show();
                } else {
                    if(receiver != null){
                        DocumentReference senderConversationRef = db.collection("users").document(mAuth.getCurrentUser().getUid()).collection("conversations").document();
                        String convId = senderConversationRef.getId();
                        DocumentReference receiverConversationRef = db.collection("users").document(receiver.getUserId()).collection("conversations").document(convId);
                        DocumentReference receiverMessageRef = receiverConversationRef.collection("messages").document();
                        DocumentReference senderMessageRef = senderConversationRef.collection("messages").document(receiverMessageRef.getId());

                        HashMap<String, Object> senderConversationData = new HashMap<>();
                        HashMap<String, Object> receiverConversationData = new HashMap<>();
                        HashMap<String, Object> senderMessageData = new HashMap<>();
                        HashMap<String, Object> receiverMessageData = new HashMap<>();

                        // create message on sender's end
                        senderMessageData.put("conversationId", convId);
                        senderMessageData.put("title", binding.editTextTitle.getText().toString());
                        senderMessageData.put("messageText", binding.editTextMessage.getText().toString());
                        senderMessageData.put("mSenderId", mAuth.getUid());
                        senderMessageData.put("mReceiverId", receiver.getUserId());
                        senderMessageData.put("mId", receiverMessageRef.getId());
                        senderMessageData.put("sentAt", FieldValue.serverTimestamp());

                        // create message on receiver's end
                        receiverMessageData.put("conversationId", convId);
                        receiverMessageData.put("title", binding.editTextTitle.getText().toString());
                        receiverMessageData.put("messageText", binding.editTextMessage.getText().toString());
                        receiverMessageData.put("mSenderId", mAuth.getUid());
                        receiverMessageData.put("mReceiverId", receiver.getUserId());
                        receiverMessageData.put("mId", receiverMessageRef.getId());
                        receiverMessageData.put("sentAt", FieldValue.serverTimestamp());

                        //create conversation on sender's end
                        senderConversationData.put("date", FieldValue.serverTimestamp());
                        senderConversationData.put("title", binding.editTextTitle.getText().toString());
                        senderConversationData.put("senderId", mAuth.getUid());
                        senderConversationData.put("receiverId", receiver.getUserId());
                        senderConversationData.put("newMessages", false);
                        senderConversationData.put("conversationId", convId);

                        //create conversation on receiver's end
                        receiverConversationData.put("title", binding.editTextTitle.getText().toString());
                        receiverConversationData.put("date", FieldValue.serverTimestamp());
                        receiverConversationData.put("receiverId", receiver.getUserId());
                        receiverConversationData.put("senderId", mAuth.getUid());
                        receiverConversationData.put("newMessages", true);
                        receiverConversationData.put("conversationId", convId);

                        senderConversationRef.set(senderConversationData);
                        receiverConversationRef.set(receiverConversationData);
                        senderMessageRef.set(senderMessageData);
                        receiverMessageRef.set(receiverMessageData);

                        mListener.submitCreate();
                    }

                }
            }
        });

        binding.buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.cancelCreate();
            }
        });
    }

    void getReceiver(String userId){
        db.collection("users").document(userId).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                if(error != null){
                    error.printStackTrace();
                    return;
                }
                receiver = null;
                if(value.exists() && value != null){
                    User currentUser = value.toObject(User.class);
                    receiver = currentUser;
                }
            }
        });
    }

    void getUsers(){
        if(usersRegistration != null){
            usersRegistration.remove();
        }
        usersRegistration = db.collection("users").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if(error != null){
                    error.printStackTrace();
                    return;
                }
                mEmails.clear();
                mUsers.clear();
                for (QueryDocumentSnapshot doc : value) {
                    if(value != null && !value.isEmpty()){
                        User tempUser = doc.toObject(User.class);
                        mUsers.add(tempUser);
                        mEmails.add(tempUser.getEmail());
                    }

                }
            }
        });
    }
    void getBlockedUsers(){
        if(blockedRegistration != null){
            blockedRegistration.remove();
        }
        blockedRegistration = db.collection("users")
                .document(mAuth.getUid()).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                if(error != null){
                    error.printStackTrace();
                    return;
                }
                mBlockedUserIds.clear();
                if(value.exists() && value != null){
                    User currentUser = value.toObject(User.class);
                    mBlockedUserIds = currentUser.getBlockedUserIds();
                }
            }
        });
    }

    CreateConversationListener mListener;
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mListener = (CreateConversationListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement CreateConversationListener");
        }
    }

    public interface CreateConversationListener{
        void cancelCreate();
        void submitCreate();
    }
}