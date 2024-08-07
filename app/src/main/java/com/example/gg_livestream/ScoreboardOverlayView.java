package com.example.gg_livestream;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

public class ScoreboardOverlayView extends View {
    private Paint paint;
    private String team1Name = "Team 1";
    private String team2Name = "Team 2";
    private int team1Score = 0;
    private int team2Score = 0;

    public ScoreboardOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(50);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
    }

    public void setScoreboardDetails(String team1Name, String team2Name, int team1Score, int team2Score) {
        this.team1Name = team1Name;
        this.team2Name = team2Name;
        this.team1Score = team1Score;
        this.team2Score = team2Score;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawText(team1Name + ": " + team1Score, 50, 50, paint);
        canvas.drawText(team2Name + ": " + team2Score, 50, 150, paint);
    }
}
