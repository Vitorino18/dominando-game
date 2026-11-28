package br.com.dominando.frontend.android;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AutomaticZenRule;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.Settings;
import android.service.notification.Condition;
import android.service.notification.ZenPolicy;
import android.util.Log;
import android.widget.Toast;

@TargetApi(29)
public final class GameNotificationGuard {
    private static final String TAG = "GameNotificationGuard";

    private final Activity activity;
    private final NotificationManager manager;
    private final SharedPreferences preferences;
    private final Uri conditionId;

    private AlertDialog dialog;
    private boolean resumed;

    public GameNotificationGuard(Activity activity) {
        this.activity = activity;

        manager = (NotificationManager) activity.getSystemService(
            Context.NOTIFICATION_SERVICE
        );

        preferences = activity.getSharedPreferences(
            "desceuma_notification_guard", Context.MODE_PRIVATE
        );

        conditionId = Condition.newId(activity)
            .appendPath("game_open")
            .build();
    }

    public void onResume(boolean configure) {
        resumed = true;

        if (manager == null) {
            return;
        }

        if (configure || !preferences.getBoolean("explained", false)) {
            showOptions();
        } else if (preferences.getBoolean("enabled", false)) {
            activate(false);
        }
    }

    public void onPause() {
        resumed = false;
        deactivate();

        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
    }

    private void showOptions() {
        if (dialog != null || activity.isFinishing()) {
            return;
        }

        preferences.edit().putBoolean("explained", true).apply();

        dialog = new AlertDialog.Builder(activity)
            .setTitle("Jogar sem interrupções?")
            .setMessage(
                "Ative o Não perturbe enquanto o Desce Uma estiver "
                    + "aberto para silenciar notificações e chamadas.\n\n"
                    + "Os sons do jogo e os alarmes continuam permitidos. "
                    + "Ao minimizar ou sair, o modo do jogo é desativado.\n\n"
                    + "Na primeira vez, você precisará liberar "
                    + "o acesso nas configurações do celular."
            )
            .setPositiveButton("Ativar", (window, which) -> {
                preferences.edit().putBoolean("enabled", true).apply();

                if (manager.isNotificationPolicyAccessGranted()) {
                    activate(true);
                } else {
                    openPermissionSettings();
                }
            })
            .setNegativeButton("Desativar", (window, which) -> {
                preferences.edit().putBoolean("enabled", false).apply();
                deactivate();
            })
            .create();

        dialog.setOnDismissListener(window -> dialog = null);
        dialog.show();
    }

    private void openPermissionSettings() {
        try {
            activity.startActivity(
                new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            );
        } catch (ActivityNotFoundException error) {
            Toast.makeText(
                activity,
                "Abra as configurações do celular e procure "
                    + "o acesso ao Não perturbe.",
                Toast.LENGTH_LONG
            ).show();
        }
    }

    private void activate(boolean requestedByUser) {
        if (!resumed
            || !preferences.getBoolean("enabled", false)
            || !manager.isNotificationPolicyAccessGranted()) {
            return;
        }

        try {
            String ruleId = preferences.getString("rule_id", null);
            AutomaticZenRule rule = ruleId == null
                ? null : manager.getAutomaticZenRule(ruleId);

            if (rule == null) {
                // Respeita uma regra que o usuário removeu no Android.
                if (ruleId != null && !requestedByUser) {
                    preferences.edit()
                        .putBoolean("enabled", false)
                        .remove("rule_id")
                        .apply();
                    return;
                }

                ZenPolicy policy = new ZenPolicy.Builder()
                    .disallowAllSounds()
                    .allowAlarms(true)
                    .allowMedia(true)
                    .showPeeking(false)
                    .showFullScreenIntent(false)
                    .showLights(false)
                    .build();

                rule = new AutomaticZenRule(
                    "Desce Uma",
                    null,
                    new ComponentName(activity, AndroidLauncher.class),
                    conditionId,
                    policy,
                    NotificationManager.INTERRUPTION_FILTER_PRIORITY,
                    true
                );

                ruleId = manager.addAutomaticZenRule(rule);

                if (ruleId == null) {
                    throw new IllegalStateException(
                        "O Android não criou o modo do jogo."
                    );
                }

                preferences.edit().putString("rule_id", ruleId).apply();
            } else if (!rule.isEnabled()) {
                // Só reativa uma regra desabilitada por uma escolha explícita.
                if (!requestedByUser) {
                    return;
                }

                rule.setEnabled(true);

                if (!manager.updateAutomaticZenRule(ruleId, rule)) {
                    throw new IllegalStateException(
                        "O Android não ativou o modo do jogo."
                    );
                }
            }

            manager.setAutomaticZenRuleState(
                ruleId,
                new Condition(
                    conditionId,
                    "Desce Uma aberto",
                    Condition.STATE_TRUE
                )
            );
        } catch (RuntimeException error) {
            Log.w(TAG, "Não foi possível ativar a proteção.", error);

            Toast.makeText(
                activity,
                "Não foi possível ativar o Não perturbe. "
                    + "O jogo continuará normalmente.",
                Toast.LENGTH_LONG
            ).show();
        }
    }

    private void deactivate() {
        if (manager == null
            || !manager.isNotificationPolicyAccessGranted()) {
            return;
        }

        String ruleId = preferences.getString("rule_id", null);

        if (ruleId == null) {
            return;
        }

        try {
            if (manager.getAutomaticZenRule(ruleId) != null) {
                manager.setAutomaticZenRuleState(
                    ruleId,
                    new Condition(
                        conditionId,
                        "Desce Uma em segundo plano",
                        Condition.STATE_FALSE
                    )
                );
            }
        } catch (RuntimeException error) {
            Log.w(TAG, "Não foi possível desativar o modo do jogo.", error);
        }
    }
}
