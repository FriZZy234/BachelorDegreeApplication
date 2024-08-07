package com.example.gg_livestream;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.example.gg_livestream.LivestreamActivity;
import com.example.gg_livestream.R;

import java.io.IOException;

public class ScoreboardFragment extends Fragment {

    private static final int PICK_IMAGE_TEAM1 = 1;
    private static final int PICK_IMAGE_TEAM2 = 2;

    private EditText firstPlayerName, secondPlayerName;
    private ImageButton firstTeamLogoButton, secondTeamLogoButton;
    private ImageView scoreboardPreview;
    private Bitmap team1Logo, team2Logo;
    private String score = "0:0";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scoareboard, container, false);

        firstPlayerName = view.findViewById(R.id.first_player_name);
        secondPlayerName = view.findViewById(R.id.second_player_name);
        firstTeamLogoButton = view.findViewById(R.id.first_team_logo_button);
        secondTeamLogoButton = view.findViewById(R.id.second_team_logo_button);
        scoreboardPreview = view.findViewById(R.id.scoreboard_preview);

        firstTeamLogoButton.setOnClickListener(v -> pickImage(PICK_IMAGE_TEAM1));
        secondTeamLogoButton.setOnClickListener(v -> pickImage(PICK_IMAGE_TEAM2));

        view.findViewById(R.id.next_button).setOnClickListener(v -> {
            // Pass data to LivestreamActivity and navigate to the next fragment
            ((LivestreamActivity) getActivity()).setScoreboardDetails(
                    firstPlayerName.getText().toString(),
                    secondPlayerName.getText().toString(),
                    team1Logo,
                    team2Logo
            );
            ((LivestreamActivity) getActivity()).navigateToStreamDetailsFragment();
        });

        // Initial preview with default score
        updateScoreboardPreview();

        return view;
    }

    private void pickImage(int requestCode) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), requestCode);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == getActivity().RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), uri);
                if (requestCode == PICK_IMAGE_TEAM1) {
                    team1Logo = bitmap;
                    firstTeamLogoButton.setImageBitmap(team1Logo);
                } else if (requestCode == PICK_IMAGE_TEAM2) {
                    team2Logo = bitmap;
                    secondTeamLogoButton.setImageBitmap(team2Logo);
                }
                updateScoreboardPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateScoreboardPreview() {
        Bitmap preview = createScoreboardPreview(
                firstPlayerName.getText().toString(),
                secondPlayerName.getText().toString(),
                team1Logo,
                team2Logo,
                score
        );
        scoreboardPreview.setImageBitmap(preview);
    }

    private Bitmap createScoreboardPreview(String team1Name, String team2Name, Bitmap team1Logo, Bitmap team2Logo, String score) {
        int width = 800;
        int height = 200;
        Bitmap scoreboardBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(scoreboardBitmap);
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(40);
        paint.setAntiAlias(true);
        paint.setTypeface(ResourcesCompat.getFont(getContext(), R.font.montserrat_light));

        // Draw team names
        canvas.drawText(team1Name, 50, 50, paint);
        canvas.drawText("VS", width / 2 - 20, 50, paint);
        canvas.drawText(team2Name, width - 200, 50, paint);

        // Draw score
        canvas.drawText(score, width / 2 - 20, 150, paint);

        // Draw team logos
        if (team1Logo != null) {
            canvas.drawBitmap(team1Logo, null, new Rect(50, 100, 150, 200), null);
        }
        if (team2Logo != null) {
            canvas.drawBitmap(team2Logo, null, new Rect(width - 200, 100, width - 100, 200), null);
        }

        return scoreboardBitmap;
    }
}
