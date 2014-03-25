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
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import dk.dr.radio.akt.Hovedaktivitet;
import dk.dr.radio.data.DRData;
import dk.dr.radio.diverse.AfspillerIkonOgNotifikation;
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

  @SuppressLint("NewApi")
  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.d("AfspillerService onStartCommand(" + intent + " " + flags + " "
        + startId);


    String kanalNavn = "";
    try {
      kanalNavn = DRData.instans.afspiller.getLydkilde().getKanal().navn;
    } catch (Exception e) {
      Log.rapporterFejl(e);
    } // TODO fjern try-catch efter nogle måneder i drift

    NotificationCompat.Builder b = new NotificationCompat.Builder(this).setSmallIcon(R.drawable.notifikation_ikon)
        .setContentTitle("DR Radio")
        .setContentText(kanalNavn)
        .setOngoing(true)
        .setAutoCancel(false)
        .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, Hovedaktivitet.class), 0));
    // PendingIntent er til at pege på aktiviteten der skal startes hvis
    // brugeren vælger notifikationen


    b.setContent(AfspillerIkonOgNotifikation.lavRemoteViews(AfspillerIkonOgNotifikation.TYPE_notifikation_lille));
    Notification notification = b.build();

    if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)) {
      // A notification's big view appears only when the notification is expanded,
      // which happens when the notification is at the top of the notification drawer,
      // or when the user expands the notification with a gesture.
      // Expanded notifications are available starting with Android 4.1.
      notification.bigContentView = AfspillerIkonOgNotifikation.lavRemoteViews(AfspillerIkonOgNotifikation.TYPE_notifikation_stor);
    }

    notification.flags |= (Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT | Notification.PRIORITY_HIGH | Notification.FLAG_FOREGROUND_SERVICE);
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
    // TODO byg notifikation
  }
}
