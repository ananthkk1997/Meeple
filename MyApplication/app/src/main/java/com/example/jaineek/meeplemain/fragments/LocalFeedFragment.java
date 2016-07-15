package com.example.jaineek.meeplemain.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.jaineek.meeplemain.NewPostActivity;
import com.example.jaineek.meeplemain.R;
import com.example.jaineek.meeplemain.adapters_and_holders.PostRecyclerAdapter;
import com.example.jaineek.meeplemain.model.Post;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Krishnak97 on 7/5/2016.
 */

public class LocalFeedFragment extends Fragment implements MeepleFragment{



    public static final String TAG = "FRAGMENT_LOCAL_FEED";
    public static String title_local_feed_fragment = "My Feed";
    public static int drawable_icon_id = R.drawable.ic_home_white_48dp;

    private RecyclerView mLocalFeedRecyclerView;
    private FloatingActionButton mNewPostFAB;
    private List<Post> mLocalPosts = new ArrayList<>();

    // Declaring Firebase variables
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Save all instance information
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_local_feed, container, false);

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        mLocalFeedRecyclerView = (RecyclerView) v.findViewById(R.id.local_feed_recyclerView);
        mLocalPosts = findLocalPosts();

        setUpRecyclerViewAndAdapter();

        mNewPostFAB = (FloatingActionButton) v.findViewById(R.id.fab_new_post);
        setUpFloatingActionButton();

        return v;
    }

    private void setUpRecyclerViewAndAdapter() {
        // Sets up RecyclerView, LocalPostAdapter with data and LayoutManager

        // Sets linearLayoutManager
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        mLocalFeedRecyclerView.setLayoutManager(layoutManager);

        // Creates adapter w/ data. Sets up w/ RecyclerView
        PostRecyclerAdapter localFeedAdapter = new PostRecyclerAdapter(mLocalPosts, getActivity());
        mLocalFeedRecyclerView.setAdapter(localFeedAdapter);
    }

    private void setUpFloatingActionButton() {
        // Wires the NewPost floating action button

        mNewPostFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // When clicked, start NewPostActivity
                Intent toNewPostActivity = new Intent(getActivity(), NewPostActivity.class);
                startActivity(toNewPostActivity);
            }
        });
    }

    private List<Post> findLocalPosts() {
        // Returns List<Post> close to user locaton
        // TODO: implement actual GPS tracking
        List<Post> testPosts = new ArrayList<>();
        for(int i = 0; i < 30; i++) {
            Post post = new Post();
            post.setPostMessage("Local Post " + i);
            testPosts.add(post);
        }

        return testPosts;
    }

    /* MEEPLE FRAGMENT METHODS */

    @Override
    public String getTitle() {
        // returns Title of fragment
        return title_local_feed_fragment;
    }

    @Override
    public int getDrawableIconId() {
        // Returns Drawable tab icon for this page
        // TODO: make compatible
        return drawable_icon_id;
    }

    @Override
    public String getFragmentTag() {
        return TAG;
    }
}