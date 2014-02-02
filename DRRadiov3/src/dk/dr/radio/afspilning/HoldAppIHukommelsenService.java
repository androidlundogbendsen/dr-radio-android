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

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import dk.dr.radio.akt_v3.Hovedaktivitet;
import dk.dr.radio.diverse.AfspillerWidget;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.v3.R;

/**
 * Sørger for at app'en holdes i hukommelsen
 *
 * @author j
 */
public class HoldAppIHukommelsenService extends Service {
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
    Log.d("AfspillerService onStartCommand(" + intent + " " + flags + " " + startId);

    String kanalNavn = intent == null ? null : intent.getStringExtra("kanalNavn");
    if (kanalNavn == null) kanalNavn = "";

    NotificationCompat.Builder b = new NotificationCompat.Builder(this).setSmallIcon(R.drawable.notifikation_ikon).setContentTitle("DR Radio").setContentText(kanalNavn).setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, Hovedaktivitet.class), 0)).setContent(AfspillerWidget.lavRemoteViews());

    //notification = new Notification(R.drawable.notifikation_ikon, null, 0);
    Notification notification = b.build();

    // PendingIntent er til at pege på aktiviteten der skal startes hvis brugeren vælger notifikationen
    notification.flags |= (Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT);


    //notification.setLatestEventInfo(this, "Radio", kanalNavn, notification.contentIntent);
    startForeground(NOTIFIKATION_ID, notification);
    return START_STICKY;
  }


  @Override
  public void onDestroy() {
    Log.d("AfspillerService onDestroy!");
    stopForeground(true);
  }
}
