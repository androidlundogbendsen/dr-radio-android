/**
 DR Radio 2 is developed by Jacob Nordfalk, Hanafi Mughrabi and Frederik Aagaard.
 Some parts of the code are loosely based on Sveriges Radio Play for Android.

 DR Radio 2 for Android is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License version 2 as published by
 the Free Software Foundation.

 DR Radio 2 for Android is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along with
 DR Radio 2 for Android.  If not, see <http://www.gnu.org/licenses/>.

 */
package dk.dr.radio.afspilning;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import dk.dr.radio.akt.Hovedaktivitet;
import dk.dr.radio.data.DRData;
import dk.dr.radio.diverse.AfspillerIkonOgNotifikation;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.v3.R;

/**
 * Sørger for at app'en holdes i hukommelsen
 *
 * @author j
 */
public class HoldAppIHukommelsenService extends Service implements Runnable {
  /**
   * Service-mekanik. Ligegyldig, da vi kører i samme proces.
   */
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  /**
   * ID til notifikation i toppen. Skal bare være unikt og det samme altid
   */
  private static final int NOTIFIKATION_ID = 117;

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.d("AfspillerService onStartCommand(" + intent + " " + flags + " "
        + startId);

    Notification notification = AfspillerIkonOgNotifikation.lavNotification(this);
    startForeground(NOTIFIKATION_ID, notification);
    return START_STICKY;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    DRData.instans.afspiller.observatører.add(this);
  }

  @Override
  public void onDestroy() {
    DRData.instans.afspiller.observatører.remove(this);
    stopForeground(true);
  }

  @Override
  public void run() {
    Notification notification = AfspillerIkonOgNotifikation.lavNotification(this);
    App.notificationManager.notify(NOTIFIKATION_ID, notification);
  }
}
