package com.example.conversationsapp.auth;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.widget.Toast;

import com.example.conversationsapp.databinding.FragmentSignUpBinding;
import com.example.conversationsapp.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;


public class SignUpFragment extends Fragment {
    public SignUpFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    FragmentSignUpBinding binding;
    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    ArrayList<String> existingEmails = new ArrayList<>();
    ListenerRegistration listenerRegistration;

    void getUsers(){
        if(listenerRegistration != null){
            listenerRegistration.remove();
        }
        listenerRegistration = db.collection("users")
                .whereNotEqualTo("userId", mAuth.getUid()).addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if(error != null){
                    error.printStackTrace();
                    return;
                }
                existingEmails.clear();
                for (QueryDocumentSnapshot doc : value) {
                    User tempUser = doc.toObject(User.class);
                    String tempEmail = tempUser.getEmail();
                    existingEmails.add(tempEmail);
                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSignUpBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getUsers();

        binding.buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.login();
            }
        });

        binding.buttonSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = binding.editTextUsername.getText().toString();
                String email = binding.editTextEmail.getText().toString();
                String password = binding.editTextPassword.getText().toString();

                if(name.isEmpty()){
                    Toast.makeText(getActivity(), "Enter valid name!", Toast.LENGTH_SHORT).show();
                } else if(email.isEmpty()){
                    Toast.makeText(getActivity(), "Enter valid email!", Toast.LENGTH_SHORT).show();
                } else if (password.isEmpty()){
                    Toast.makeText(getActivity(), "Enter valid password!", Toast.LENGTH_SHORT).show();
                } else if(existingEmails.contains(email)){
                    Toast.makeText(getActivity(), "Email used on another account", Toast.LENGTH_SHORT).show();
                } else {
                    mAuth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener(task -> {
                                if(task.isSuccessful()){
                                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                            .setDisplayName(name)
                                            .build();

                                    mAuth.getCurrentUser().updateProfile(profileUpdates).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(task.isSuccessful()){
                                                Toast.makeText(getActivity(), "Account created successfully!", Toast.LENGTH_SHORT).show();
                                                createUserInFirebase(email,  name);
                                            } else {
                                                Toast.makeText(getActivity(), "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });

                                } else {
                                    Toast.makeText(getActivity(), "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });

                }
            }
        });

        getActivity().setTitle("Sign Up");

    }

    public void createUserInFirebase(String email, String username){

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        DocumentReference userRef = db.collection("users").document(mAuth.getUid());
        HashMap<String, Object> data = new HashMap<>();
        ArrayList<String> blockedUserIds = new ArrayList<>();
        data.put("username", username);
        data.put("email", email);
        data.put("userId", mAuth.getUid());
        data.put("blockedUserIds", blockedUserIds);

        userRef.set(data).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()) {
                    mListener.authCompleted();
                } else {
                    Toast.makeText(getActivity(), task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    SignUpListener mListener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mListener = (SignUpListener) context;
    }

    public interface SignUpListener {
        void login();
        void authCompleted();
    }
}