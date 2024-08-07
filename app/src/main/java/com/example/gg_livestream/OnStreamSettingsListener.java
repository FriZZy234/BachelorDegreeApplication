package com.example.gg_livestream;

import android.graphics.Bitmap;
import android.net.Uri;

public interface OnStreamSettingsListener {
    void setScoreboardDetails(String firstPlayerName, String secondPlayerName);

    void setScoreboardDetails(String firstPlayerName, String secondPlayerName, Bitmap team1Logo, Bitmap team2Logo);

    void setStreamDetails(String title, String description, String visibility, Uri thumbnailUri);
}
