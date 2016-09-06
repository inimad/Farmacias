package com.chernandezgil.farmacias.view;

import android.location.Location;

import com.chernandezgil.farmacias.model.Pharmacy;

import java.util.List;

/**
 * Created by Carlos on 05/09/2016.
 */

public interface FindContract {
    public interface View {
        public void showResults(List<Pharmacy> pharmacyList);
        public void showNoResults();
        public void hideNoResults();;
        public void showLoading();
        public void hideLoading();

    }
    public interface Presenter<V> {


        void setView(V view);

        void detachView();

        public void onStartLoader();
        public void onRestartLoader(String newText);



    }
}
