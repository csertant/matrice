package com.nosebite.matrice;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;
import androidx.navigation.Navigation;


/**
 * A simple {@link Fragment} subclass that displays a Success Screen upon finishing the actual Level.
 */
public class SuccessScreenFragment extends Fragment {

    private String previousGame;

    public SuccessScreenFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_success_screen, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        updateBackgroundNightMode();

        /* Adds callback to Home Button to navigate the User to the Main Screen. */
        ImageButton toHomeButton = (ImageButton) view.findViewById(R.id.homeButtonSuccess);
        toHomeButton.setOnClickListener(v -> {
            //Uses popUpTo global action to remove Fragment instances from backstack
            Navigation.findNavController(view)
                    .navigate(MainNavGraphDirections.actionPopUpToMainScreenFragment());
        });

        /* Adds callback to Replay Button to navigate the user back and restart previous game */
        ImageButton replayButton = (ImageButton) view.findViewById(R.id.successControlsReplayButton);
        replayButton.setOnClickListener(v -> {
            //Sets the data of the previous game as a result for GameScreenFragment
            Bundle previousGameData = new Bundle();
            previousGameData.putString("previousGame", previousGame);
            getParentFragmentManager().setFragmentResult("replayGameData", previousGameData);

            Navigation.findNavController(v)
                    .navigate(SuccessScreenFragmentDirections.actionSuccessScreenFragmentToGameScreenFragment());
        });

        /* Adds callback to Levels Button to navigate the user to the level chooser screen */
        ImageButton levelsButton = (ImageButton) view.findViewById(R.id.successControlsLevelsButton);
        levelsButton.setOnClickListener(v -> {
            //TODO Navigate to Levels Screen
            Toast.makeText(getContext(), "This feature is coming soon!", Toast.LENGTH_SHORT).show();
        });

        /* Adds callback to Play Button to navigate the user back and start new game */
        ImageButton playButton = (ImageButton) view.findViewById(R.id.successControlsPlayButton);
        playButton.setOnClickListener(v -> {
            //TODO When levels will be implemented jump to next level instead
            Navigation.findNavController(v)
                    .navigate(SuccessScreenFragmentDirections.actionSuccessScreenFragmentToGameScreenFragment());
        });

        /*
        * Adds callback that listens to the results from the Game Fragment in order to display game details.
        */
        getParentFragmentManager()
                .setFragmentResultListener("gameData", this, (FragmentResultListener) (requestKey, result) -> {
                    String elapsedTime = result.getString("elapsedTime");
                    String score = result.getString("score");
                    String stepSize = result.getString("stepSize");

                    previousGame = result.getString("previousGame");

                    TextView scoreView = (TextView) view.findViewById(R.id.scoreText);
                    scoreView.setText(score);

                    TextView scoreDetailView = (TextView) view.findViewById(R.id.scoreDetail);
                    String scoreDetails = getString(R.string.text_score_details_start) + " " + stepSize + " "
                            + getString(R.string.text_score_details_middle) + " " + elapsedTime + " "
                            + getString(R.string.text_score_details_end);
                    scoreDetailView.setText(scoreDetails);
                });
    }

    private void updateBackgroundNightMode() {
        int nightModeFlags = getContext().getResources().getConfiguration()
                .uiMode & Configuration.UI_MODE_NIGHT_MASK;
        ConstraintLayout layout = (ConstraintLayout) getView().findViewById(R.id.successFragmentLayout);
        switch (nightModeFlags) {
            case Configuration.UI_MODE_NIGHT_YES:
                layout.setBackgroundResource(R.drawable.ic_game_surface2);
                break;
            case Configuration.UI_MODE_NIGHT_NO:
            case Configuration.UI_MODE_NIGHT_UNDEFINED:
                layout.setBackgroundResource(R.drawable.ic_game_surface3);
                break;
        }
    }
}
