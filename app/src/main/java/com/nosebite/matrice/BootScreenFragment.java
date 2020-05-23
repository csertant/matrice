package com.nosebite.matrice;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;


/**
 * A simple {@link Fragment} subclass that is displayed at the right start of the Application.
 */
public class BootScreenFragment extends Fragment {

    private boolean isSignedIn;
    private MainActivity mainActivity;

    public BootScreenFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_boot_screen, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mainActivity = (MainActivity) getActivity();
        isSignedIn = false;

        /*
          Adds callback to the launcher Button to navigate the user to the Main Screen
         */
        ImageButton toMainButton = (ImageButton) view.findViewById(R.id.goToMainButton);
        toMainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mainActivity.getSignedInAccount() == null) {
                    mainActivity.startSignInIntent();
                } else {
                    Navigation.findNavController(view)
                            .navigate(BootScreenFragmentDirections.actionBootScreenFragmentToMainScreenFragment());
                }
            }
        });
    }
}
