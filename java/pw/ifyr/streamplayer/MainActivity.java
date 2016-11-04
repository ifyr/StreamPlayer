package pw.ifyr.streamplayer;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends Activity {
    protected int speedRate = 4;
    protected String mediaString = "grandfather";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button playButton = (Button) findViewById(R.id.btn_play);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    AssetFileDescriptor afd = getResources().getAssets().openFd(mediaString + ".mp3");
                    StreamPlayer.play(speedRate, afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                } catch (Exception e) {
                }
            }
        });

        ComboBar barRate = (ComboBar) findViewById(R.id.bar_rate);
        List<String> rateStep = Arrays.asList(
                "Speed/2.0","Speed/1.5","Speed/1.2","Speed/1.1",
                "Original",
                "Speed*1.1","Speed*1.2","Speed*1.5","Speed*2.0"
        );
        barRate.setAdapter(rateStep);
        barRate.setSelection(speedRate);

        barRate.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                speedRate = position;
            }
        });

        Spinner spinMedia = (Spinner) findViewById(R.id.spin_media);
        final String[] mediaStrings = {
                "grandfather", "aunt", "teacher", "previous", "president", "pleasure",
                "observer", "measure", "english", "choice", "ability"
        };
        ArrayAdapter<String> spinAdapter = new ArrayAdapter<>(this, R.layout.layout_spinner, mediaStrings);
        spinAdapter.setDropDownViewResource(R.layout.layout_spinner);
        spinMedia.setAdapter(spinAdapter);
        spinMedia.setSelection(0);

        spinMedia.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                mediaString = mediaStrings[i];
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                mediaString = "grandfather";
            }
        });
    }
}
