package br.com.dominando.frontend.android;

import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

import br.com.dominando.frontend.DesceUmaGame;

public class AndroidLauncher extends AndroidApplication {
    private GameNotificationGuard notificationGuard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AndroidApplicationConfiguration configuration =
            new AndroidApplicationConfiguration();

        configuration.useImmersiveMode = true;
        initialize(new DesceUmaGame(), configuration);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            notificationGuard = new GameNotificationGuard(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (notificationGuard != null) {
            boolean configure =
                NotificationManager.ACTION_AUTOMATIC_ZEN_RULE.equals(
                    getIntent().getAction()
                );

            if (configure) {
                getIntent().setAction(Intent.ACTION_MAIN);
            }

            notificationGuard.onResume(configure);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onPause() {
        if (notificationGuard != null) {
            notificationGuard.onPause();
        }

        super.onPause();
    }
}
