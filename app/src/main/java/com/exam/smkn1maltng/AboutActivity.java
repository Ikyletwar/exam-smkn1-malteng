package com.exam.smkn1maltng;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.exam.smkn1maltng.databinding.ActivityAboutBinding;

public class AboutActivity extends AppCompatActivity {
    
    private ActivityAboutBinding binding;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAboutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Tombol back custom
        binding.ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        
        binding.tvAppName.setText(R.string.about_app_name);
        binding.tvVersion.setText(R.string.about_version);
        binding.tvDescription.setText(R.string.about_description);
        binding.tvDeveloperLabel.setText(R.string.about_developer);
        binding.tvDeveloperName.setText(R.string.about_developer_name);
        binding.tvClass.setText(R.string.about_class);
        binding.tvCopyright.setText(R.string.about_copyright);
    }
    
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.binding = null;
    }
}
