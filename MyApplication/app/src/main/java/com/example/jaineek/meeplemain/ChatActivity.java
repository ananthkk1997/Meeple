package com.example.jaineek.meeplemain;

import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.example.jaineek.meeplemain.adapters_and_holders.ChatViewHolder;
import com.example.jaineek.meeplemain.adapters_and_holders.FirebaseRecyclerAdapter;
import com.example.jaineek.meeplemain.model.Chat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.Date;

public class ChatActivity extends AppCompatActivity {
    // Displays messages between two users

    public final static String NODE_CONVOS = "convos";
    public final static String NODE_USERS = "users";
    public final static String NODE_CONVOKEYS = "convokeys";
    public static final String KEY_EXTRA_CONVOKEY =
            "com.example.jaineek.meeplemain.extra_tag_convokey";

    private Button mSendButton;
    private EditText mEditTextMessage;
    private RecyclerView mRecyclerView;
    private String mConvokey;
    private Long mTimeStamp;
    private Date mDate;
    private Chat mChat;

    private FirebaseRecyclerAdapter mChatAdapter;
    private DatabaseReference mDatabaseReference;
    private DatabaseReference mConvoReference;
    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mDatabaseReference = FirebaseDatabase.getInstance().getReference();
        mSendButton = (Button) findViewById(R.id.chat_activity_send_button);
        mEditTextMessage = (EditText) findViewById(R.id.chat_activity_edit_text);
        mAuth = FirebaseAuth.getInstance();
        mRecyclerView = (RecyclerView) findViewById(R.id.chat_recyclerView);


        /* MUST BE STARTED WITH STRING EXTRA CONVOKEY
         Get correct convoKey from Intent
          */
        Intent intent = getIntent();
        mConvokey = intent.getStringExtra(KEY_EXTRA_CONVOKEY);

        mConvoReference = mDatabaseReference.child(NODE_CONVOS).child(mConvokey);

        // Creates adapter w/ data. Sets up w/ RecyclerView
        mChatAdapter = new FirebaseRecyclerAdapter<Chat, ChatViewHolder>(Chat.class,
                R.layout.chat_sent, ChatViewHolder.class, mConvoReference.orderByChild("timeStamp")) {
                    @Override
                    protected void populateViewHolder(ChatViewHolder viewHolder, Chat model, int position) {
                        // Populate viewHolder with Post info
                        viewHolder.bindViewsWithChat(model);
                    }
                };

        // Sets LayoutManager and Adapter
        mRecyclerView.setLayoutManager(new LinearLayoutManager(ChatActivity.this));
        mRecyclerView.setAdapter(mChatAdapter);

        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Declaring chat object, convokey, and timestamp
                mDate = new Date();
                mTimeStamp = mDate.getTime();
                mChat = new Chat(mAuth.getCurrentUser().getUid(), mAuth.getCurrentUser().getUid(),
                        mEditTextMessage.getText().toString(), mTimeStamp);


                //Pushing data to Database
                mDatabaseReference.child(NODE_CONVOS).child(mConvokey).push().setValue(mChat);
                mDatabaseReference.child(NODE_USERS).child(mAuth.getCurrentUser().getUid())
                        .child(NODE_CONVOKEYS).child(mConvokey).setValue(mConvokey);

                //Clearing the 'Send a Message' edit text
                mEditTextMessage.setText("");
                // Scroll to last Position
                mRecyclerView.scrollToPosition(mRecyclerView.getAdapter().getItemCount());


            }
        });

        // Enable Back arrow on Toolbar
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    private static String findCorrectConvoKey(String userUID1, String userUID2) {
        // Return the correct convo key NO MATTER WHO IS USER 1 / 2

        if (userUID1.compareTo(userUID2) < 0) {
            return userUID1 + ","
                    + userUID2;
        } else {
            return userUID2 + ","
                    + userUID1;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // app icon in action bar clicked; goto back.
                onBackPressed();
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }
}
