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
import com.example.conversationsapp.databinding.FragmentNewConversationBinding;
import com.example.conversationsapp.databinding.RowItemFromUserBinding;
import com.example.conversationsapp.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;

public class BlockedUsersFragment extends Fragment {
    public BlockedUsersFragment() {
        // Required empty public constructor
    }

    User mainUser = new User();
    ArrayList<User> mBlockedUsers = new ArrayList<>();
    ArrayList<User> mUsers = new ArrayList<>();
    BlockedUsersAdapter adapter;
    FragmentBlockedUsersBinding binding;
    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    ListenerRegistration listenerRegistration;

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
            mListener.backButton();
            return true;
        }
        return super.onOptionsItemSelected(item);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentBlockedUsersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getActivity().setTitle("Blocked Users");
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new BlockedUsersAdapter();
        binding.recyclerView.setAdapter(adapter);

        getUsers();
        getActivity().setTitle("Blocked Users");
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
                Log.d("Users", "onEvent: " + mUsers);
                Log.d("blockedUsers1", "onEvent: "+ mainUser.getBlockedUserIds());
                for(int i = 0; i < mUsers.size(); i++){
                    if(mainUser.getBlockedUserIds().contains(mUsers.get(i).getUserId())){
                        mBlockedUsers.add(mUsers.get(i));
                    }

                }
                mBlockedUsers.remove(mainUser);

                adapter.notifyDataSetChanged();

                Log.d("blockedUsers", "onCheckedChanged: " + mBlockedUsers);
            }

        });

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(listenerRegistration != null){
            listenerRegistration.remove();
        }
    }

    class BlockedUsersAdapter extends RecyclerView.Adapter<BlockedUsersAdapter.BlockedUsersViewHolder>{
        @NonNull
        @Override
        public BlockedUsersViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            RowItemFromUserBinding itemBinding = RowItemFromUserBinding.inflate(getLayoutInflater(), parent, false);
            return new BlockedUsersViewHolder(itemBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull BlockedUsersViewHolder holder, int position) {
            holder.setupUI(mBlockedUsers.get(position));
        }

        @Override
        public int getItemCount() {
            return mBlockedUsers.size();
        }

        class BlockedUsersViewHolder extends RecyclerView.ViewHolder{
            RowItemFromUserBinding itemBinding;
            User mUser;
            public BlockedUsersViewHolder(RowItemFromUserBinding itemBinding) {
                super(itemBinding.getRoot());
                this.itemBinding = itemBinding;
            }

            public void setupUI(User user){
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
                            mBlockedUsers.remove(mUser);
                        } else {
                            data.put("blockedUserIds", FieldValue.arrayUnion(mUser.getUserId()));
                            blockedUpdate.update(data);
                            mBlockedUsers.add(mUser);
                        }
                        Log.d("blockedUsers", "onCheckedChanged: " + mBlockedUsers);

                    }
                });
            }
        }
    }

    BlockedUsersListener mListener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mListener = (BlockedUsersListener) context;
        } catch (ClassCastException e){
            throw new ClassCastException(context.toString() + " must implement CourseReviewsListener");
        }
    }

    public interface BlockedUsersListener {

        void backButton();

    }


}