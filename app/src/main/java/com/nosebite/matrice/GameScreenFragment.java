package com.nosebite.matrice;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GestureDetectorCompat;
import androidx.fragment.app.Fragment;
import androidx.gridlayout.widget.GridLayout;
import androidx.navigation.Navigation;
import androidx.preference.PreferenceManager;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import matrice.FigureSet;
import matrice.Game;
import matrice.GameData;
import matrice.GameState;
import matrice.Move;
import matrice.Transformation;

/**
 * Fragment responsible for the actual game
 */
public class GameScreenFragment extends Fragment {

    //TAG for Logging & Debugging
    private static final String TAG = GameScreenFragment.class.getSimpleName();

    private MainActivity mainActivity;
    private String userId;

    private GameScreenViewModel mViewModel;
    private Game game;

    /**
     * Indicator used to switch between New Game / Stop Game buttons
     */
    private boolean isGameStopped = false;

    /**
     * Local variables to dynamically update the TextView showing elapsed time from the game start
     */
    private TextView timer;
    private Handler timerHandler;
    private Runnable timerRunnable;

    /**
     *  Local variable to display stepCount to the screen
     */
    private TextView stepCounter;

    /**
     * Storing the gridLayouts to not have to look up them in every function call
     */
    private GridLayout gameLayout;
    private GridLayout endLayout;

    /**
     * Detector for detecting common user gestures. Used on the Game Board where users can swipe to
     * move.
     */
    private GestureDetectorCompat mDetector;

    /* Reference of the Firebase Database Root */
    private DatabaseReference dataBase;

    /**
     * Store reference of Button to be able to change its drawable when needed.
     */
    private ImageButton pausePlayButton;

    public static GameScreenFragment newInstance() {
        return new GameScreenFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.game_screen_fragment, container, false);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        /* Initialising Game Object upon saved Preferences */
        initGameUponPreferences(null);

        /* Requiring player id from Main Activity */
        mainActivity = (MainActivity) getActivity();
        if(mainActivity != null) {
            userId = mainActivity.getUserID();
        }

        /* Get Instance of Firebase Database Reference */
        dataBase = FirebaseDatabase.getInstance().getReference();

        /* Adds callback to Home Button which navigates the user to the Main Screen */
        ImageButton toHomeButton = (ImageButton) view.findViewById(R.id.leftControlsHomeButton);
        toHomeButton.setOnClickListener(v -> {
            //Uses popUpTo global action to remove Fragment instances from backstack
            Navigation.findNavController(view).navigate(MainNavGraphDirections.actionPopUpToMainScreenFragment());
        });
        toHomeButton.setOnLongClickListener(v -> {
            Toast.makeText(getContext(), getString(R.string.text_gotohome), Toast.LENGTH_SHORT).show();
            return true;
        });

        /* Adds callback to Play/Pause Button to suspend/resume measuring elapsed time */
        pausePlayButton = (ImageButton) view.findViewById(R.id.rightControlsPausePlayButton);
        pausePlayButton.setOnClickListener(this::onPausePlayButtonPressed);
        pausePlayButton.setOnLongClickListener(v -> {
            Toast.makeText(getContext(), getString(R.string.text_pauseplay), Toast.LENGTH_SHORT).show();
            return true;
        });

        /* Adds callback to Stop/New Game Button to let the user stop the game or create new game */
        ImageButton stopButton = (ImageButton) view.findViewById(R.id.rightControlsStopButton);
        stopButton.setOnClickListener(this::onStopButtonPressed);
        stopButton.setOnLongClickListener(v -> {
            Toast.makeText(getContext(), getString(R.string.text_stop), Toast.LENGTH_SHORT).show();
            return true;
        });

        /* Adds callback to Retry Button in order to let the user go back to the start
          states of the level */
        ImageButton retryButton = (ImageButton) view.findViewById(R.id.rightControlsRetryButton);
        retryButton.setOnClickListener(this::onRetryButtonPressed);
        retryButton.setOnLongClickListener(v -> {
            Toast.makeText(getContext(), getString(R.string.text_retry), Toast.LENGTH_SHORT).show();
            return true;
        });

        /* Adds callback to Levels Button to navigate the user to the Level choser Screen */
        ImageButton levelsButton = (ImageButton) view.findViewById(R.id.leftControlsLevelsButton);
        levelsButton.setOnClickListener(v -> {
            //TODO Navigate to Levels Screen
            Toast.makeText(getContext(), getString(R.string.message_feature_coming_soon), Toast.LENGTH_SHORT).show();
        });
        levelsButton.setOnLongClickListener(v -> {
            Toast.makeText(getContext(), getString(R.string.text_gotolevels), Toast.LENGTH_SHORT).show();
            return true;
        });

        /* Adds callback to Back Button to navigate the user down in the back stack */
        ImageButton backButton = (ImageButton) view.findViewById(R.id.leftControlsBackButton);
        backButton.setOnClickListener(v -> mainActivity.onBackPressed());
        backButton.setOnLongClickListener(v -> {
            Toast.makeText(getContext(), getString(R.string.text_goback), Toast.LENGTH_SHORT).show();
            return true;
        });

        /* Adds callback to Help Button to show hints to the user. */
        ImageButton helpButton = (ImageButton) view.findViewById(R.id.helpButtonGameScreen);
        helpButton.setOnClickListener(v -> {
            /* Sends current game data so the user is able to continue the same game after closing the help screen. */
            setScoreDetails();
            Navigation.findNavController(v).navigate(GameScreenFragmentDirections.actionGameScreenFragmentToGameHelpFragment());
        });

        /* Set On CLick Listeners for user move help buttons */
        view.findViewById(R.id.leftHelp1).setOnClickListener(v -> onSwipe(Move.HORIZONTAL, Transformation.INVERT, 0));
        view.findViewById(R.id.leftHelp2).setOnClickListener(v -> onSwipe(Move.HORIZONTAL, Transformation.INVERT, 1));
        view.findViewById(R.id.leftHelp3).setOnClickListener(v -> onSwipe(Move.HORIZONTAL, Transformation.INVERT, 2));
        view.findViewById(R.id.topHelp1).setOnClickListener(v -> onSwipe(Move.VERTICAL, Transformation.INVERT, 0));
        view.findViewById(R.id.topHelp2).setOnClickListener(v -> onSwipe(Move.VERTICAL, Transformation.INVERT, 1));
        view.findViewById(R.id.topHelp3).setOnClickListener(v -> onSwipe(Move.VERTICAL, Transformation.INVERT, 2));
        view.findViewById(R.id.rightHelp1).setOnClickListener(v -> onSwipe(Move.HORIZONTAL, Transformation.ROTATE, 0));
        view.findViewById(R.id.rightHelp2).setOnClickListener(v -> onSwipe(Move.HORIZONTAL, Transformation.ROTATE, 1));
        view.findViewById(R.id.rightHelp3).setOnClickListener(v -> onSwipe(Move.HORIZONTAL, Transformation.ROTATE, 2));
        view.findViewById(R.id.bottomHelp1).setOnClickListener(v -> onSwipe(Move.VERTICAL, Transformation.ROTATE, 0));
        view.findViewById(R.id.bottomHelp2).setOnClickListener(v -> onSwipe(Move.VERTICAL, Transformation.ROTATE, 1));
        view.findViewById(R.id.bottomHelp3).setOnClickListener(v -> onSwipe(Move.VERTICAL, Transformation.ROTATE, 2));

        view.findViewById(R.id.leftHelp1).setOnLongClickListener(v -> {Toast.makeText(getContext(), getString(R.string.text_pref_action_inversion), Toast.LENGTH_SHORT).show(); return true;});
        view.findViewById(R.id.leftHelp2).setOnLongClickListener(v -> {Toast.makeText(getContext(), getString(R.string.text_pref_action_inversion), Toast.LENGTH_SHORT).show(); return true;});
        view.findViewById(R.id.leftHelp3).setOnLongClickListener(v -> {Toast.makeText(getContext(), getString(R.string.text_pref_action_inversion), Toast.LENGTH_SHORT).show(); return true;});
        view.findViewById(R.id.topHelp1).setOnLongClickListener(v -> {Toast.makeText(getContext(), getString(R.string.text_pref_action_inversion), Toast.LENGTH_SHORT).show(); return true;});
        view.findViewById(R.id.topHelp2).setOnLongClickListener(v -> {Toast.makeText(getContext(), getString(R.string.text_pref_action_inversion), Toast.LENGTH_SHORT).show(); return true;});
        view.findViewById(R.id.topHelp3).setOnLongClickListener(v -> {Toast.makeText(getContext(), getString(R.string.text_pref_action_inversion), Toast.LENGTH_SHORT).show(); return true;});
        view.findViewById(R.id.rightHelp1).setOnLongClickListener(v -> {Toast.makeText(getContext(), getString(R.string.text_pref_action_rotation), Toast.LENGTH_SHORT).show(); return true;});
        view.findViewById(R.id.rightHelp2).setOnLongClickListener(v -> {Toast.makeText(getContext(), getString(R.string.text_pref_action_rotation), Toast.LENGTH_SHORT).show(); return true;});
        view.findViewById(R.id.rightHelp3).setOnLongClickListener(v -> {Toast.makeText(getContext(), getString(R.string.text_pref_action_rotation), Toast.LENGTH_SHORT).show(); return true;});
        view.findViewById(R.id.bottomHelp1).setOnLongClickListener(v -> {Toast.makeText(getContext(), getString(R.string.text_pref_action_rotation), Toast.LENGTH_SHORT).show(); return true;});
        view.findViewById(R.id.bottomHelp2).setOnLongClickListener(v -> {Toast.makeText(getContext(), getString(R.string.text_pref_action_rotation), Toast.LENGTH_SHORT).show(); return true;});
        view.findViewById(R.id.bottomHelp3).setOnLongClickListener(v -> {Toast.makeText(getContext(), getString(R.string.text_pref_action_rotation), Toast.LENGTH_SHORT).show(); return true;});

        /* Handling TextView to displaying elapsed time in every second */
        timer = (TextView) view.findViewById(R.id.rightControlsGameDuration);
        timer.setText(this.game.getFormattedDuration());

        timerHandler = new Handler();
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                timer.setText(game.getFormattedDuration());
                timerHandler.postDelayed(this, 1000);
            }
        };

        /* Displaying stepCount */
        stepCounter = (TextView) view.findViewById(R.id.topDetailsStepCount);
        stepCounter.setText(Integer.toString(this.game.getCurrentGame().getStepSize()));

        /* Initialising local variables that store grid layouts */
        gameLayout = (GridLayout) view.findViewById(R.id.startStateLayout);
        endLayout = (GridLayout) view.findViewById(R.id.endStateLayout);

        /* Apply Gesture listener */
        mDetector = new GestureDetectorCompat(getActivity(), new FlingGestureListener());

        /* Setting on touch listener to the Game Board */
        gameLayout.setOnTouchListener((view13, event) -> mDetector.onTouchEvent(event));

        /* Getting previous game when returning from success screen to replay level. */
        getParentFragmentManager()
                .setFragmentResultListener("replayGameData", this, (requestKey, result) -> {
                    String previousGame = result.getString("previousGame");
                    initGameUponPreferences(previousGame);
                    updateLayout(endLayout, game.getCurrentGame().getEndState());
                    updateLayout(gameLayout, game.getCurrentGame().getStartState());
                    game.start();
                    timerHandler.postDelayed(timerRunnable, 1000);
                });
    }

    /* Handling Fragment Lifecycle Changes */

    /**
     * Overrides default onStart() method of Fragment.
     * Updates Grid Layouts with start and end states of the current Game.
     * Starts the Game - starts Stopwatch measuring time.
     */
    @Override
    public void onStart() {
        super.onStart();
        updateLayout(endLayout, this.game.getCurrentGame().getEndState());
        updateLayout(gameLayout, this.game.getCurrentGame().getCurrentState());
        if(!this.game.isGameStarted())
            this.game.start();
        else if(this.game.isGamePaused())
            this.game.resume();
        timerHandler.postDelayed(timerRunnable, 1000);
    }

    /**
     * Overrides default onPause() method of Fragment.
     * When Fragment's paused pauses the Game.
     */
    @Override
    public void onPause() {
        super.onPause();
        this.game.pause();
    }

    /**
     * Overrides default onResume() method of Fragment.
     * When the user gets back to the App resumes the Game.
     */
    @Override
    public void onResume() {
        super.onResume();
        this.game.resume();
    }

    /**
     * Overrides default onStop() method of Fragment.
     * When Fragment's about to be destroyed and gets stopped stops and logs out the Game.
     */
    @Override
    public void onStop() {
        super.onStop();
        this.game.pause();
        timerHandler.removeCallbacks(timerRunnable);
    }

    /* Handling User Commands*/

    /**
     * Callback function firing when Pause/Play Button is clicked.
     * @param view View which the click happened in. Default parameter of onClick callbacks.
     */
    private void onPausePlayButtonPressed(@NonNull View view) {
        if(!this.isGameStopped) {
            if (!this.game.isGamePaused()) {
                this.game.pause();
                pausePlayButton.setImageResource(R.drawable.ic_play_icon);
            } else {
                this.game.resume();
                pausePlayButton.setImageResource(R.drawable.ic_pause_icon);
            }
        }
    }

    /**
     * Callback function firing when Stop/New Game Button is clicked.
     * @param view View which the click happened in. Default parameter of onCLick callbacks.
     */
    private void onStopButtonPressed(@NonNull View view) {
        ImageButton stopButton = (ImageButton) view.findViewById(R.id.rightControlsStopButton);
        if(this.game.isGameStarted()) {
            if(!this.isGameStopped) {
                //Stops current Game
                this.game.stop();
                this.isGameStopped = true;
                stopButton.setImageResource(R.drawable.ic_new_game_icon);
                pausePlayButton.setImageResource(R.drawable.ic_play_icon);
            }
            else {
                //Initialises New Game
                initGameUponPreferences(null);
                updateLayout(endLayout, this.game.getCurrentGame().getEndState());
                updateLayout(gameLayout, this.game.getCurrentGame().getStartState());
                this.game.start();
                this.isGameStopped = false;
                stopButton.setImageResource(R.drawable.ic_stop_icon);
                pausePlayButton.setImageResource(R.drawable.ic_pause_icon);
            }
        }
    }

    /**
     * Callback function firing when Retry Button is clicked.
     * @param view View which the click happened in. Default parameter of onCLick callbacks.
     */
    private void onRetryButtonPressed(View view) {
        if(this.game.isGameStarted()) {
            this.game.restart();
            this.isGameStopped = false;

            /* Update UI elements */
            updateLayout(gameLayout, this.game.getCurrentGame().getStartState());
            stepCounter.setText(Integer.toString(this.game.getCurrentGame().getStepSize()));
            pausePlayButton.setImageResource(R.drawable.ic_pause_icon);
        }
    }

    /**
     * Handles user swipes.
     * @param move Direction of the swipe. Types specified in {@link Move} class.
     * @param id Identity of the row or column on which the swipe occurs.
     */
    private void onSwipe(Move move, Transformation transformation, int id) {
        //Handles swipe
        boolean finished = this.game.handleMove(move, transformation, id);
        updateLayout(gameLayout, this.game.getCurrentGame().getCurrentState());
        stepCounter.setText(Integer.toString(this.game.getCurrentGame().getStepSize()));
        //If the game is finished stops it and navigates the user to the Success Screen
        if(finished) {
            this.game.stop();
            this.isGameStopped = true;

            gameToDatabase();
            incrementGameAchievements();
            updateLeaderboardScores();

            setScoreDetails();
            final Handler handler  = new Handler();
            handler.postDelayed(() -> Navigation.findNavController(requireView())
                    .navigate(GameScreenFragmentDirections.actionGameScreenFragmentToSuccessScreenFragment()), 1000);
        }
    }

    /* Helper functions and Classes */

    /**
     * Creates and initialises a Game Object upon User Preferences stored in Shared Preferences.
     */
    private void initGameUponPreferences(@Nullable String savedState) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        int boardSize = preferences.getInt(getString(R.string.key_game_boardsize), 3);
        //ListPreference items are stored as Strings. To get int values we need to cast with Integer.parseInt()
        int figureSetId = Integer.parseInt(preferences.getString(getString(R.string.key_figure_set), "0"));

        //If there is a saved state start from there, else start new random game
        if(savedState != null)
        {
            //To pass correct instances of the Enum types we need to cast ints with .fromId()
            this.game = new Game(boardSize, FigureSet.fromId(figureSetId), savedState);
        }
        else {
            //To pass correct instances of the Enum types we need to cast ints with .fromId()
            this.game = new Game(boardSize, FigureSet.fromId(figureSetId));
        }
    }

    /**
     * Updates layout with the appropriate figures in each cell of the Game board matrix
     * @param layout Layout to be updated (GridLayout)
     * @param state State with which the layout will be updated (GameState)
     */
    private void updateLayout(@NonNull GridLayout layout, @NonNull GameState state) {
        int count = layout.getChildCount();
        FigureSet figureSet = game.getFigureSet();
        Boolean value;
        int boardSize = this.game.getCurrentGame().getCurrentState().getBoardSize();
        //Loops through the Childs of the Layout
        for(int i = 0; i < count; i++) {
            ImageView field = (ImageView) layout.getChildAt(i);
            //Gets the value of the appropriate board Cell and sets the Figure
            value = state.getCell((i / boardSize), (i % boardSize));
            if(value) {
                switch (figureSet) {
                    case PLUMP:
                        field.setImageResource(R.drawable.ic_figure_plump1);
                        break;
                    case TICTACTOE:
                        field.setImageResource(R.drawable.ic_figure_o);
                        break;
                    case PLUSMINUS:
                        field.setImageResource(R.drawable.ic_figure_minus);
                }
            }
            else
                switch (figureSet) {
                    case PLUMP:
                        field.setImageResource(R.drawable.ic_figure_plump2);
                        break;
                    case TICTACTOE:
                        field.setImageResource(R.drawable.ic_figure_x);
                        break;
                    case PLUSMINUS:
                        field.setImageResource(R.drawable.ic_figure_plus);
                }
        }
    }

    /**
     * Upon finishing game the function sets the score details needed to be sent to
     * {@link SuccessScreenFragment}.
     * FragmentResult is a feature in androidx.fragment:1.3.0-alpha04 version. Might be not stable.
     */
    private void setScoreDetails() {
        Bundle result = new Bundle();
        result.putString("elapsedTime", this.game.getFormattedDuration());
        result.putString("score", String.valueOf(this.game.getScore()));
        result.putString("stepSize", String.valueOf(this.game.getCurrentGame().getStepSize()));

        /* Deleting current state (that equals to end state) from Previous Game and replacing
         it with start state */
        String[] previousGameTemp = this.game.getCurrentGame().toString().split(":");
        String previousGame = previousGameTemp[0] + ":" + previousGameTemp[1] + ":" + previousGameTemp[0];
        result.putString("previousGame", previousGame);

        getParentFragmentManager().setFragmentResult("gameData", result);
    }

    /**
     * Writes the game into the Firebase Realtime Database.
     */
    private void gameToDatabase() {
        /* Writes game data into a readable format */
        GameData gameData = new GameData(game.getCurrentGame().getStartState().getStateId(),
                game.getCurrentGame().getEndState().getStateId(),
                game.getCurrentGame().sequenceToString(),
                game.getCurrentGame().getStepSize(),
                game.getCurrentGame().getStartState().getBoardSize(),
                game.getStartTime(),
                game.getDuration());
        Map<String, Object> gameDataValues = gameData.toMap();

        /* Creates new user if needed */
        DatabaseReference userReference = dataBase.child("users").child(userId);
        userReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(!dataSnapshot.exists()) {
                    dataBase.child("users").child(userId).setValue(true);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "loadUser:onCancelled", databaseError.toException());
            }
        });

        /* Writes the data */
        String key = dataBase.child("games").child(userId).push().getKey();
        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("/games/" + userId + "/" + key, gameDataValues);
        dataBase.updateChildren(childUpdates);
    }

    /**
     * Increments achievements that counts played games.
     */
    private void incrementGameAchievements() {
        mainActivity.getAchievementsClient().increment(getString(R.string.achievement_novice_player), 1);
        mainActivity.getAchievementsClient().increment(getString(R.string.achievement_experienced_player), 1);
        mainActivity.getAchievementsClient().increment(getString(R.string.achievement_fellow_researcher), 1);
        mainActivity.getAchievementsClient().increment(getString(R.string.achievement_doctor_of_researching_things), 1);
    }

    /**
     * Updates leaderboard scores of current player.
     */
    private void updateLeaderboardScores() {
        mainActivity.getLeaderboardsClient()
                .submitScore(getString(R.string.leaderboard_best_score_ever), game.getScore());
        mainActivity.getLeaderboardsClient()
                .submitScore(getString(R.string.leaderboard_you_are_so_fast), game.getDuration());
    }

    /**
     * Gesture Listener class used to handle common user gestures such as swipes.
     */
    class FlingGestureListener extends GestureDetector.SimpleOnGestureListener {
        //private static final String DEBUG_TAG = "Gestures";

        /* Constants that help decide whether the Motion has to be handled */
        private static final int SWIPE_MIN_DISTANCE = 10;
        private static final int SWIPE_THRESHOLD_VELOCITY = 5;

        /* Needed to Override onDown() method to listen to any motion */
        @Override
        public boolean onDown(@NonNull MotionEvent event) {
            //Log.d(DEBUG_TAG, "onDown: " + event.toString());
            return true;
        }

        /* Overrides default onFling method */
        @Override
        public boolean onFling(@NonNull MotionEvent event1, @NonNull MotionEvent event2,
                               float velocityX, float velocityY) {
            // Log.d(DEBUG_TAG, "onFLing: " + event1.toString() + event2.toString());

            /* Gets coordinates of the move */
            float x1 = event1.getX();
            float y1 = event1.getY();
            float x2 = event2.getX();
            float y2 = event2.getY();

            /* Checks if values exceed thresholds */
            if(Math.abs(x1 - x2) < SWIPE_MIN_DISTANCE || Math.abs(y1 - y2) < SWIPE_MIN_DISTANCE
                    || Math.abs(velocityX) < SWIPE_THRESHOLD_VELOCITY
                    || Math.abs(velocityY) < SWIPE_THRESHOLD_VELOCITY)
            {
                return false;
            }

            /* Gets the characteristics of the move */
            double angle = getAngle(x1, y1, x2, y2);
            Move move = Move.fromAngle(angle);
            Transformation transformation = Transformation.fromAngle(angle);
            int id = getMoveId(move, x1, y1);
            /* Handles move */
            onSwipe(move, transformation, id);
            return true;
        }

        /**
         * Calculates the angle of the motion.
         * @param x1 x coordinate of the start event
         * @param y1 y coordinate of the start event
         * @param x2 x coordinate of the end event
         * @param y2 y coordinate of the end event
         * @return (double) angle of the motion
         */
        private double getAngle(float x1, float y1, float x2, float y2) {
            double angleInDegrees = Math.toDegrees(Math.atan2(y1 - y2, x2 - x1));
            if(angleInDegrees < 0) {
                return 360 + angleInDegrees;
            }
            else return angleInDegrees;
        }

        /**
         * Calculates the id of the row or column on which the motion started
         * @param move Type of the move
         * @param x1 x coordinate of the start event
         * @param y1 y coordinate of the start event
         * @return (int) id of the row/column affected by user swipe
         */
        private int getMoveId(@NonNull Move move, float x1, float y1) {
            int gameLayoutSize = getParentFragment().getView().findViewById(R.id.startStateLayout).getWidth();
            int boardSize = game.getCurrentGame().getCurrentState().getBoardSize();
            int scale = gameLayoutSize / boardSize;
            switch (move) {
                case HORIZONTAL:
                    return (int) (y1 / scale);
                case VERTICAL:
                    return (int) (x1 / scale);
                default:
                    return 0;
            }
        }
    }
}