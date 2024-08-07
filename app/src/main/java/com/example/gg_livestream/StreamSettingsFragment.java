package com.example.gg_livestream;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class StreamSettingsFragment extends Fragment {

    private EditText titleEditText, descriptionEditText;
    private RadioGroup visibilityRadioGroup;
    private OnStreamSettingsListener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnStreamSettingsListener) {
            listener = (OnStreamSettingsListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnStreamSettingsListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stream_settings, container, false);

        titleEditText = view.findViewById(R.id.title);
        descriptionEditText = view.findViewById(R.id.description);
        visibilityRadioGroup = view.findViewById(R.id.visibility);

        view.findViewById(R.id.next_button).setOnClickListener(v -> {
            String title = titleEditText.getText().toString();
            String description = descriptionEditText.getText().toString();
            String visibility;
            int checkedId = visibilityRadioGroup.getCheckedRadioButtonId();
            if (checkedId == R.id.private_option) {
                visibility = "private";
            } else if (checkedId == R.id.unlisted_option) {
                visibility = "unlisted";
            } else {
                visibility = "public";
            }

            listener.setStreamDetails(title, description, visibility, null);
            ((LivestreamActivity) getActivity()).navigateToFinalizeSetupFragment();
        });

        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }
}
