package com.t7droid.letsmeet.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.t7droid.letsmeet.R;
import com.t7droid.letsmeet.network.ApiClient;
import com.t7droid.letsmeet.network.ApiService;
import com.t7droid.letsmeet.utilities.Constants;

import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class IncomingInvitationActivity extends AppCompatActivity {

    private String meetingType = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incoming_invitation);

        ImageView imageMeetingType = findViewById(R.id.imageMeetingTypeIncoming);
        meetingType = getIntent().getStringExtra(Constants.REMOTE_MSG_MEETING_TYPE);

        if(meetingType != null){
            if (meetingType.equals("video")){
                imageMeetingType.setImageResource(R.drawable.ic_video);
            } else {
                imageMeetingType.setImageResource(R.drawable.ic_audio);
            }
        }

        TextView textFirstChar = findViewById(R.id.textFirstCharIncoming);
        TextView textUserName = findViewById(R.id.textUserNameIncoming);
        TextView textEmail = findViewById(R.id.textEmailIncoming);

        String firstName = getIntent().getStringExtra(Constants.KEY_FIRST_NAME);
        if (firstName != null){
            textFirstChar.setText(firstName.substring(0, 1));
        }

        textUserName.setText(
                String.format("%s %s",
                        firstName,
                        getIntent().getStringExtra(Constants.KEY_LAST_NAME))
        );

        textEmail.setText(getIntent().getStringExtra(Constants.KEY_EMAIL));

        ImageView imageAcceptInvitation = findViewById(R.id.imageAcceptInvitation);
        imageAcceptInvitation.setOnClickListener(v -> sendInvitationResponse(
                Constants.REMOTE_MSG_INVITATION_ACCEPTED,
                getIntent().getStringExtra(Constants.REMOTE_MSG_INVITER_TOKEN)
        ));

        ImageView imageRejectInvitation = findViewById(R.id.imageRejectInvitation);
        imageRejectInvitation.setOnClickListener(v -> sendInvitationResponse(
                Constants.REMOTE_MSG_INVITATION_REJECTED,
                getIntent().getStringExtra(Constants.REMOTE_MSG_INVITER_TOKEN)
        ));
    }

    private void sendInvitationResponse(String type, String receiverToken){
        try {
            JSONArray tokens = new JSONArray();
            tokens.put(receiverToken);

            JSONObject body = new JSONObject();
            JSONObject data = new JSONObject();

            data.put(Constants.REMOTE_MSG_TYPE, Constants.REMOTE_INVITATION_RESPONSE);
            data.put(Constants.REMOTE_INVITATION_RESPONSE, type);

            body.put(Constants.REMOTE_MSG_DATA, data);
            body.put(Constants.REMOTE_REGISTRATION_IDS, tokens);

            sendRemoteMessage(body.toString(), type);

        } catch (Exception e){
            mensagem(e.getMessage());
            finish();
        }
    }

    private void sendRemoteMessage(String remoteMessageBody, String type){
        ApiClient.getClient().create(ApiService.class).sendRemoteMessage(
                Constants.getRemoteMessageHeaders(), remoteMessageBody
        ).enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                if (response.isSuccessful()){
                   if (type.equals(Constants.REMOTE_MSG_INVITATION_ACCEPTED)){
                       try {
                           URL serverURL = new URL("https://meet.jit.si");
                           JitsiMeetConferenceOptions.Builder builder = new JitsiMeetConferenceOptions.Builder();
                           builder.setServerURL(serverURL);
                           builder.setWelcomePageEnabled(false);
                           builder.setRoom(getIntent().getStringExtra(Constants.REMOTE_MSG_MEETING_ROOM));
                           if (meetingType.equals("audio")){
                               builder.setVideoMuted(true);
                           }
                           JitsiMeetActivity.launch(IncomingInvitationActivity.this, builder.build());
                           finish();
                       } catch (Exception e){
                           mensagem(e.getMessage());
                           finish();
                       }
                   } else {
                       mensagem("Convite Rejeitado");
                       finish();
                   }
                } else {
                    mensagem(response.message());
                    finish();
                }

            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                mensagem(t.getMessage());
                finish();
            }
        });
    }

    private void mensagem(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private BroadcastReceiver invitationResponseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String type = intent.getStringExtra(Constants.REMOTE_INVITATION_RESPONSE);
            if(type != null){
                if (type.equals(Constants.REMOTE_MSG_INVITATION_CANCELLED)){
                    mensagem("Convite cancelado");
                    finish();
                } else if(type.equals(Constants.REMOTE_MSG_INVITATION_REJECTED)){
                    mensagem("Convite rejeitado");
                    finish();
                }
            }
        }
    };
    
    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                invitationResponseReceiver,
                new IntentFilter(Constants.REMOTE_INVITATION_RESPONSE)
        );
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(
                invitationResponseReceiver
        );
    }
}