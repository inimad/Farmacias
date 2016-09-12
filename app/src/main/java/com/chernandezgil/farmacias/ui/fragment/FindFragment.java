package com.chernandezgil.farmacias.ui.fragment;

import android.app.LoaderManager;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.location.Location;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;

import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.chernandezgil.farmacias.R;
import com.chernandezgil.farmacias.Utilities.Constants;
import com.chernandezgil.farmacias.Utilities.SearchUtils;
import com.chernandezgil.farmacias.Utilities.Util;
import com.chernandezgil.farmacias.data.LoaderProvider;
import com.chernandezgil.farmacias.data.source.local.DbContract;
import com.chernandezgil.farmacias.data.source.local.RecentSuggestionsProvider;
import com.chernandezgil.farmacias.model.Pharmacy;
import com.chernandezgil.farmacias.model.SuggestionsBean;
import com.chernandezgil.farmacias.presenter.FindPresenter;
import com.chernandezgil.farmacias.ui.adapter.FindQuickSearchAdapter;
import com.chernandezgil.farmacias.ui.adapter.FindRecyclerViewAdapter;
import com.chernandezgil.farmacias.ui.adapter.FindSuggestionsAdapter;
import com.chernandezgil.farmacias.view.FindContract;
import com.jakewharton.rxbinding.widget.RxTextView;
import com.linearlistview.LinearListView;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import it.gmariotti.recyclerview.adapter.SlideInBottomAnimatorAdapter;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;


/**
 * Created by Carlos on 10/07/2016.
 */
public class FindFragment extends Fragment implements FindContract.View,FindQuickSearchAdapter.OnClickHandler {


    private static final String LOG_TAG = FindFragment.class.getSimpleName();

    @BindView(R.id.progressBar)
    ProgressBar mProgressBar;
    @BindView(R.id.findRecyclerView)
    RecyclerView mRecyclerView;
    @BindView(R.id.emptyView)
    TextView mEmptyView;
    @BindView(R.id.list)
    LinearListView mFrankySardo;

    //Activity UI elements
    private RecyclerView mQuickSearchRecyclerView;
    private EditText mSearchEditor;
    private ImageView mClearSearch;
    private CardView mSearchCardView;
    private ImageView mImageSearchBack;

    private FindPresenter mPresenter;
    private Unbinder unbinder;
    private FindRecyclerViewAdapter mAdapter;
    private FindSuggestionsAdapter mAdapter1;
    private SearchRecentSuggestions mRecentSearchSuggestions;
    private FindQuickSearchAdapter mFindQuickSearchAdapter;
    private RelativeLayout mViewSearch;

    private Location mLocation;
    private boolean mRotation;
    private boolean mCardOnScreen;
    private CompositeSubscription mCompositeSubscription;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Util.logD(LOG_TAG, "onCreate");
        Bundle bundle = getArguments();

        if (savedInstanceState == null) {
            if (bundle != null) {
                mLocation = bundle.getParcelable("location_key");
            }
        } else {
            mRotation=true;
            mLocation = savedInstanceState.getParcelable("location_key");

        }
        LoaderProvider loaderProvider = new LoaderProvider(getContext());
        LoaderManager loaderManager = getActivity().getLoaderManager();
        mPresenter = new FindPresenter(mLocation,loaderManager, loaderProvider);

        setHasOptionsMenu(true);
        mRecentSearchSuggestions = new SearchRecentSuggestions(getContext(),
                RecentSuggestionsProvider.AUTHORITY, RecentSuggestionsProvider.MODE);
        mCompositeSubscription = new CompositeSubscription();
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Util.logD(LOG_TAG, "onCreateView");
        View view = inflater.inflate(R.layout.fragment_find, container, false);
        unbinder = ButterKnife.bind(this, view);
        setUpRecyclerView();
        mPresenter.setView(this);


        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        Util.logD(LOG_TAG, "onActivityCreated");
        super.onActivityCreated(savedInstanceState);
        initializeSearchUiWidgets();
        if(savedInstanceState == null) {
            mPresenter.onInitLoader();
        } else {
            String searchText= Constants.EMPTY_STRING;

                mCardOnScreen = savedInstanceState.getBoolean("card_on_screen_key");
                if(mCardOnScreen) {
                    searchText = savedInstanceState.getString("edit_search_key", Constants.EMPTY_STRING);


                    mPresenter.onInitLoader();
                    mSearchCardView.setVisibility(View.VISIBLE);

                    int options = mSearchEditor.getImeOptions();
            //        mSearchEditor.setImeOptions(options | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
                    if (savedInstanceState.getInt("recyclerview_state")==View.VISIBLE) {
                        mQuickSearchRecyclerView.setVisibility(View.VISIBLE);
                        mPresenter.onInitLoaderQuickSearch();
                    }

                    //mSearchEditor.setText(searchText);

                    //falta por ver el foco; si al poner el searcheditor el loader del quick search se activa
                    // y el teclado
                }else { //restore only the state of the recyclerview results
                     mPresenter.onInitLoader();

                }



        }

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_fragment_find, menu);



    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_search:

                mPresenter.onStartLoaderQuickSearch("");
                initializeSearchCardView();
                return true;


            case R.id.action_filter:
                Toast.makeText(getContext(), "onclick filter", Toast.LENGTH_SHORT).show();
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("location_key",mLocation);
        outState.putString("last_search_editor_key", mSearchEditor.getText().toString());
        outState.putBoolean("card_on_screen_key",mCardOnScreen);
        outState.putInt("recyclerview_state",mQuickSearchRecyclerView.getVisibility());
       //0 visible; 8 gone; 4 invisible

    }

    private void initializeSearchCardView() {
        SearchUtils.setUpAnimations(getContext(),mSearchCardView,mViewSearch, mQuickSearchRecyclerView);
        //if we haben removed the focus before this is necessary. If it is the first click not.
        requestFocusOnSearchEditor();
        mCardOnScreen = true;
    }
    private void initializeSearchUiWidgets(){

        setUpQuickSearchRecyclerView();

        mSearchEditor = (EditText) getActivity().findViewById(R.id.edit_text_search);
        mSearchEditor.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean isVisible) {
                if(isVisible) {
                    showQuickSearchRecyclerView();
                }
            }
        });
        mClearSearch = (ImageView) getActivity().findViewById(R.id.clearSearch);
        mSearchCardView = (CardView) getActivity().findViewById(R.id.card_search);
        mViewSearch =(RelativeLayout)getActivity().findViewById(R.id.view_search);
        mImageSearchBack = (ImageView) getActivity().findViewById(R.id.image_search_back);
//        mSearchEditor.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//
//            }
//
//            @Override
//            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//
//            }
//
//            @Override
//            public void afterTextChanged(Editable editable) {
////                mPresenter.onStartLoaderQuickSearch(mSearchEditor.getText().toString());
////                if(mSearchEditor.getText().length()>0) {
////                    mClearSearch.setVisibility(View.VISIBLE);
////                } else {
////                    mClearSearch.setVisibility(View.INVISIBLE);
////                }
//                startQuickSearch(mSearchEditor.getText().toString());
//            }
//        });
        //https://kotlin.link/articles/RxAndroid-and-Kotlin-Part-1.html
//        Subscription editorAterTextChangeEvent = RxTextView.afterTextChangeEvents(mSearchEditor)
//                .debounce(100, TimeUnit.MILLISECONDS)
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(event->{
//                    startQuickSearch(event.view().getText().toString());
//                });

        Subscription editorActionEvent= RxTextView.editorActionEvents(mSearchEditor)
                .subscribe(event-> {
                    if(event.actionId()== EditorInfo.IME_ACTION_SEARCH){
                        onClickImeSearchIcon(event.view().getText().toString());
                    }
                });

    //    mCompositeSubscription.add(editorAterTextChangeEvent);
        mCompositeSubscription.add(editorActionEvent);

        mImageSearchBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SearchUtils.setUpAnimations(getContext(),mSearchCardView,mViewSearch, mQuickSearchRecyclerView);
                //delete current text so that in the next appearance don't show
                clearSearchEditor();
                clearFocusFromSearchEditor();
                mCardOnScreen = false;
            }
        });
        mClearSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearSearchEditor();
                requestFocusOnSearchEditor();
            }
        });

    }
    private void startQuickSearch(String s){
        mPresenter.onStartLoaderQuickSearch(s);
        mFindQuickSearchAdapter.setmSearchString(s);
        if(s.length()>0) {
            mClearSearch.setVisibility(View.VISIBLE);
        } else {
            mClearSearch.setVisibility(View.INVISIBLE);
        }

    }

    private void requestFocusOnSearchEditor(){
        mSearchEditor.requestFocus();
    }
    private void clearSearchEditor(){
             mSearchEditor.getText().clear();
    }
    private void setUpQuickSearchRecyclerView() {

        mQuickSearchRecyclerView = (RecyclerView) getActivity().findViewById(R.id.listView);
        mFindQuickSearchAdapter = new FindQuickSearchAdapter(getContext(),this);
        mQuickSearchRecyclerView.setAdapter(mFindQuickSearchAdapter);
        mQuickSearchRecyclerView.setHasFixedSize(true);
        mQuickSearchRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

    }
    private void showQuickSearchRecyclerView(){
        mSearchCardView.post(new Runnable() {
            @Override
            public void run() {
                mQuickSearchRecyclerView.setVisibility(View.VISIBLE);
            }
        });

    }
    private void hideSearchCardView(){
        mSearchCardView.setVisibility(View.INVISIBLE);
    }




    private void setUpRecyclerView() {

        mAdapter = new FindRecyclerViewAdapter(getContext());
        SlideInBottomAnimatorAdapter animatorAdapter = new SlideInBottomAnimatorAdapter(mAdapter, mRecyclerView);
        mRecyclerView.setAdapter(animatorAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setHasFixedSize(true);




    }

    @Override
    public void showResults(List<Pharmacy> pharmacyList) {

        //       mAdapter.swapData(pharmacyList);

//            if(!mRotation) {
//
//                if (mQuickSearchRecyclerView.getVisibility() == View.VISIBLE) {
//                    mQuickSearchRecyclerView.setVisibility(View.GONE);
//                }
//            } else {
//                mRotation = false;
//            }


    }

    public MatrixCursor transformListInToCursor(List<Pharmacy> pharmacyList) {
        if (pharmacyList == null) return null;
        String[] columnNames = {DbContract.FarmaciasEntity._ID,
                DbContract.FarmaciasEntity.NAME,
                DbContract.FarmaciasEntity.ADDRESS,
                DbContract.FarmaciasEntity.LOCALITY,
                DbContract.FarmaciasEntity.PROVINCE
        };
        MatrixCursor cursor = new MatrixCursor(columnNames, pharmacyList.size());
        MatrixCursor.RowBuilder builder;
        for (Pharmacy ph : pharmacyList) {
            builder = cursor.newRow();
            builder.add(ph.get_id());
            builder.add(ph.getName());
            builder.add(ph.getAddress());
            builder.add(ph.getLocality());
            builder.add(ph.getProvince());
        }
        return cursor;
    }

    @Override
    public void showNoResults() {

             mAdapter.swapData(null);

    }

    @Override
    public void showResultsQuickSearch(List<SuggestionsBean> list) {
        mFindQuickSearchAdapter.swapData(list);
    }

    @Override
    public void showNoResultsQuickSearch() {
        List<SuggestionsBean> voidList = new ArrayList<>();
        mFindQuickSearchAdapter.swapData(voidList);
    }

    @Override
    public void hideNoResults() {
        mEmptyView.setVisibility(View.GONE);
    }

    @Override
    public void showLoading() {
        mProgressBar.setVisibility(View.VISIBLE);

    }

    @Override
    public void hideLoading() {
        mProgressBar.setVisibility(View.GONE);
    }

    @Override
    public void hideQuickSearchRecyclerView() {
        mQuickSearchRecyclerView.setVisibility(View.GONE);
    }



    private void onClickImeSearchIcon(String text) {
        onClickSuggestions(text);
    }

    @Override
    public void onClickSuggestions(String text) {

        mRecentSearchSuggestions.saveRecentQuery(text,null);
        hideSoftKeyBoard();
        mSearchEditor.setText(text);
        clearFocusFromSearchEditor();
        mPresenter.onRestartLoader(text);



    }

    private void clearFocusFromSearchEditor(){
        mSearchEditor.clearFocus();
        //Note: When a View clears focus the framework is trying to give focus to the first focusable View from the top. Hence, if this View is the first from the top that can take focus, then all callbacks related to clearing focus will be invoked after which the framework will give focus to this view.
        //the solution is make another element focusable and request its focus, in this case I chose mRecyclerview
        mRecyclerView.requestFocus();
    }
    private void getSoftKeyboardState(){
        //((InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE))
    }
    private void hideSoftKeyBoard(){

        View view=getActivity().getCurrentFocus();
        if (view != null) {
            ((InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
    private void restoreToolbarActivityUiState(){
        hideSoftKeyBoard(); // task done when back in search editor
        hideSearchCardView(); // task done when back in search editor
        hideQuickSearchRecyclerView(); // task done when back in search editor
        clearSearchEditor();
        clearFocusFromSearchEditor();
    }

    @Override
    public void onDestroyView() {
        Util.logD(LOG_TAG, "onDestroyView");

        restoreToolbarActivityUiState();
        mCompositeSubscription.unsubscribe();
        unbinder.unbind();
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        Util.logD(LOG_TAG, "onDestroy");
        super.onDestroy();
    }


}
