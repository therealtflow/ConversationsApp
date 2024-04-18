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
import android.widget.CompoundButton;

import com.example.conversationsapp.R;
import com.example.conversationsapp.databinding.FragmentBlockedUsersBinding;
import com.example.conversationsapp.databinding.FragmentUsersBinding;
import com.example.conversationsapp.databinding.RowItemFromUserBinding;
import com.example.conversationsapp.databinding.RowItemUserBinding;
import com.example.conversationsapp.models.Conversation;
import com.example.conversationsapp.models.User;
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
import com.google.firebase.firestore.Source;

import java.util.ArrayList;
import java.util.HashMap;

public class UsersFragment extends Fragment {

    public UsersFragment() {
        // Required empty public constructor
    }

    ArrayList<User> mUsers = new ArrayList<>();
    ArrayList<String> blockedUsers = new ArrayList<String>();
    UsersAdapter adapter;
    FragmentUsersBinding binding;
    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    ListenerRegistration listenerRegistration;

    User mainUser = new User();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.back_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.action_cancel){
            mListener.usersBackButton();
            return true;
        }
        return super.onOptionsItemSelected(item);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentUsersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new UsersAdapter();
        binding.recyclerView.setAdapter(adapter);

        getUsers();
        Log.d("UID", "getMainUser: " + mAuth.getUid());

        getActivity().setTitle("Users");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(listenerRegistration != null){
            listenerRegistration.remove();
        }
    }


    public void getUsers(){
        listenerRegistration = db.collection("users").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if(error != null){
                    error.printStackTrace();
                    return;
                }
                mUsers.clear();
                for (QueryDocumentSnapshot doc: value) {
                    User user = doc.toObject(User.class);
                    Log.d("User Object", "onEvent: " + user);
                    mUsers.add(user);
                    Log.d("demo1", "onEvent: "+ user);
                    if(user.getUserId().equals(mAuth.getUid())){
                        mainUser = user;
                    }
                }
                mUsers.remove(mainUser);

                adapter.notifyDataSetChanged();

                Log.d("demo", "onEvent: "+ mUsers);
            }

        });

    }

    class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UsersViewHolder>{
        @NonNull
        @Override
        public UsersViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            RowItemFromUserBinding itemBinding = RowItemFromUserBinding.inflate(getLayoutInflater(), parent, false);
            return new UsersViewHolder(itemBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull UsersViewHolder holder, int position) {
            User user = mUsers.get(position);
            holder.setupUI(user);
        }

        @Override
        public int getItemCount() {
            return mUsers.size();
        }

        class UsersViewHolder extends RecyclerView.ViewHolder{
            RowItemFromUserBinding itemBinding;
            User mUser;
            public UsersViewHolder(RowItemFromUserBinding itemBinding) {
                super(itemBinding.getRoot());
                this.itemBinding = itemBinding;
            }

            public void setupUI(User user) {
                this.mUser = user;
                itemBinding.textViewUserName.setText(user.getUsername());
                itemBinding.textViewEmail.setText(user.getEmail());

                if (mainUser.getBlockedUserIds().contains(mUser.getUserId())) {
                    itemBinding.switchBlock.setChecked(true);
                } else {
                    itemBinding.switchBlock.setChecked(false);
                }

                itemBinding.switchBlock.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        HashMap<String, Object> data = new HashMap<>();
                        DocumentReference blockedUpdate = db.collection("users").document(mAuth.getUid());
                        if (mainUser.getBlockedUserIds().contains(mUser.getUserId())){
                            data.put("blockedUserIds", FieldValue.arrayRemove(mUser.getUserId()));
                            blockedUpdate.update(data);
                        } else {
                            data.put("blockedUserIds", FieldValue.arrayUnion(mUser.getUserId()));
                            blockedUpdate.update(data);

                        }

                    }
                });

            }

        }
    }

    UsersFragmentListener mListener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mListener = (UsersFragmentListener) context;
        } catch (ClassCastException e){
            throw new ClassCastException(context.toString() + " fuck you will");
        }
    }

    public interface UsersFragmentListener{
        void usersBackButton();
    }


}