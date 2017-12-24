package com.example.hp.chatapp;

import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {


    private String mChatUser,mChatUserName;
    private Toolbar mChatToolbar;
    private DatabaseReference mRootRef;

    private TextView mTitleView,mLastSeenView;
    private CircleImageView mProfileImage;

    private FirebaseAuth mAuth;
    private String mCurrentUserId;

    private ImageButton mChatAddBtn;
    private EditText mChatMessageView;
    private ImageButton mChatSendBtn;

    private RecyclerView mMessages_list;

 //   private SwipeRefreshLayout mRefereshLayout;

    private List<Messages> messagesList=new ArrayList<>();
    private LinearLayoutManager mLinearLayout;
    private MessageAdapter mAdapter;

    private static final int TOTAL_ITEMS_TO_LOAD=10;
    private int currentpage=1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mChatToolbar= (Toolbar) findViewById(R.id.chat_app_bar);
        setSupportActionBar(mChatToolbar);


        ActionBar actionBar=getSupportActionBar();

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);

        mChatUser=getIntent().getStringExtra("user_id");



        mChatUserName=getIntent().getStringExtra("user_name");
        getSupportActionBar().setTitle(mChatUserName);



        mRootRef= FirebaseDatabase.getInstance().getReference();
        mAuth=FirebaseAuth.getInstance();
        mCurrentUserId=mAuth.getCurrentUser().getUid();


        LayoutInflater inflater= (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        View action_bar_view =inflater.inflate(R.layout.chat_custom_bar,null);
        actionBar.setCustomView(action_bar_view);


        mTitleView= (TextView) findViewById(R.id.custom_bar_title);
        mLastSeenView= (TextView) findViewById(R.id.custom_bar_seen);
        mProfileImage= (CircleImageView) findViewById(R.id.custom_bar_image);

        mMessages_list= (RecyclerView) findViewById(R.id.messages_list);
        mLinearLayout=new LinearLayoutManager(this);
        mMessages_list.setHasFixedSize(true);
        mMessages_list.setLayoutManager(mLinearLayout);

   //     mRefereshLayout= (SwipeRefreshLayout) findViewById(R.id.message_swipe_layout);
        mAdapter=new MessageAdapter(messagesList);
        mMessages_list.setAdapter(mAdapter);

        mChatAddBtn= (ImageButton) findViewById(R.id.chat_add_btn);
        mChatMessageView= (EditText) findViewById(R.id.chat_message_view);
        mChatSendBtn= (ImageButton) findViewById(R.id.chat_send_btn);

        mTitleView.setText(mChatUserName);



        loadMessage();

        mRootRef.child("Users").child(mChatUser).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                String online=dataSnapshot.child("online").getValue().toString();
                String image=dataSnapshot.child("image").getValue().toString();

                if(online.equals("true"))
                {
                    mLastSeenView.setText("Online");
                }
                else {

                    GetTimeAgo getTimeAgo=new GetTimeAgo();
                    long lastTime=Long.parseLong(online);

                    String lastSeenTime=getTimeAgo.getTimeAgo(lastTime,getApplicationContext());
                    mLastSeenView.setText(lastSeenTime);
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mRootRef.child("Chat").child(mCurrentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if(!dataSnapshot.hasChild(mChatUser))
                {
                    Map chatAddMap=new HashMap<>();
                    chatAddMap.put("seen",false);
                    chatAddMap.put("timestamp", ServerValue.TIMESTAMP);

                    Map chatUserMap=new HashMap();
                    chatUserMap.put("Chat/"+mCurrentUserId+"/"+mChatUser,chatAddMap);
                    chatUserMap.put("Chat/"+mChatUser+"/"+mCurrentUserId,chatAddMap);

                    mRootRef.updateChildren(chatUserMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                            if(databaseError!=null)
                            {
                                Log.d("CHAT_LOG",databaseError.getMessage().toString());
                            }
                        }
                    });



                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mChatSendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                sendMessage();
            }
        });

/*
        mRefereshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                currentpage++;

                messagesList.clear();
                loadMessage();
            }
        });
*/

    }

    private void loadMessage() {

        DatabaseReference messageRef=mRootRef.child("messages").child(mCurrentUserId).child(mChatUser);

        Query messageQuery=messageRef.limitToLast(currentpage*TOTAL_ITEMS_TO_LOAD);

        messageQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                Messages message=dataSnapshot.getValue(Messages.class);
                messagesList.add(message);
                mAdapter.notifyDataSetChanged();

                mMessages_list.scrollToPosition(messagesList.size()-1);
         //       mRefereshLayout.setRefreshing(false);

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    private void sendMessage() {

        String message=mChatMessageView.getText().toString();

        if(!TextUtils.isEmpty(message))
        {
            String current_user_ref="messages/"+mCurrentUserId+"/"+mChatUser;
            String chat_user_ref="messages/"+mChatUser+"/"+mCurrentUserId;

            DatabaseReference user_messgae_push=mRootRef.child("messages").child(mCurrentUserId).child(mChatUser).push();

            String push_id=user_messgae_push.getKey();

            Map messageMap=new HashMap();
            messageMap.put("message",message);
            messageMap.put("type","text");
            messageMap.put("seen",false);
            messageMap.put("time",ServerValue.TIMESTAMP);
            messageMap.put("from",mCurrentUserId);

            Map messageUserMap =new HashMap();
            messageUserMap.put(current_user_ref+"/"+push_id,messageMap);
            messageUserMap.put(chat_user_ref+"/"+push_id,messageMap);

            mChatMessageView.setText("");

            mRootRef.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                    if(databaseError!=null)
                    {
                        Log.d("CHAT_LOG",databaseError.getMessage().toString());
                    }
                }
            });

        }
    }

}