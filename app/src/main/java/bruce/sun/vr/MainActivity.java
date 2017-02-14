package bruce.sun.vr;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import bruce.sun.vr.ui.VRActivity;

/**
 * Update by sunhongzhi on 2017/2/14.
 */

public class MainActivity extends Activity {

    private EditText sphereEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sphereEditText = (EditText) findViewById(R.id.sphere_edit_text);
        Button spherePlayButton = (Button) findViewById(R.id.sphere_play_button);

        spherePlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), VRActivity.class);
                intent.putExtra("PlayUrl", sphereEditText.getText().toString());
                startActivity(intent);
            }
        });
    }
}
