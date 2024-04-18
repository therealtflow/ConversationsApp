package com.example.conversationsapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.os.Bundle;
import android.util.Log;

import com.example.conversationsapp.auth.LoginFragment;
import com.example.conversationsapp.auth.SignUpFragment;
import com.example.conversationsapp.fragments.BlockedUsersFragment;
import com.example.conversationsapp.fragments.ConversationsFragment;
import com.example.conversationsapp.fragments.NewConversationFragment;
import com.example.conversationsapp.fragments.ReplysFragment;
import com.example.conversationsapp.fragments.UsersFragment;
import com.example.conversationsapp.models.Conversation;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity implements LoginFragment.LoginListener, SignUpFragment.SignUpListener, ConversationsFragment.ConversationListener, BlockedUsersFragment.BlockedUsersListener, UsersFragment.UsersFragmentListener, ReplysFragment.ReplysFragmentListener, NewConversationFragment.CreateConversationListener{

    FirebaseAuth mAuth = FirebaseAuth.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        Log.d("DEMO","UID: " + mAuth.getUid());
//        Log.d("DEMO","CURRENT USER UID: " + mAuth.getCurrentUser().getUid());


        if(mAuth.getCurrentUser() == null){
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainerView, new LoginFragment())
                    .commit();
        } else {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainerView, new ConversationsFragment())
                    .commit();
        }
    }

    @Override
    public void createNewAccount() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainerView, new SignUpFragment())
                .commit();
    }

    @Override
    public void authCompleted() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainerView, new ConversationsFragment())
                .commit();
    }

    @Override
    public void login() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainerView, new LoginFragment())
                .commit();
    }


    @Override
    public void logout() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainerView, new LoginFragment())
                .commit();
        mAuth.signOut();
    }

    @Override
    public void goToUsers() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainerView, new UsersFragment())
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void goToBlocked() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainerView, new BlockedUsersFragment())
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void goToCreateConversation() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainerView, new NewConversationFragment())
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void goToReplysFragment(Conversation c) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainerView, ReplysFragment.newInstance(c))
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void backButton() {
        getSupportFragmentManager().popBackStack();
    }

    @Override
    public void usersBackButton() {
        getSupportFragmentManager().popBackStack();
    }

    @Override
    public void replyBackButton() {
        getSupportFragmentManager().popBackStack();
    }

    @Override
    public void cancelCreate() {
        getSupportFragmentManager().popBackStack();
    }

    @Override
    public void submitCreate() {
        getSupportFragmentManager().popBackStack();
    }
}