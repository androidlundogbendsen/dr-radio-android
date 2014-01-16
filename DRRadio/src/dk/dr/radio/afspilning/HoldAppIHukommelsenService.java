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
import dk.dr.radio.Afspilning_akt;
import dk.dr.radio.R;
import dk.dr.radio.util.Log;

/**
 * Sørger for at app'en holdes i hukommelsen
 * @author j
 */
public class HoldAppIHukommelsenService extends Service {
	/** Service-mekanik. Ligegyldig, da vi kører i samme proces. */
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

  private Notification notification;

  private String PROGRAMNAVN = "Radio";
  /** ID til notifikation i toppen. Skal bare være unikt og det samme altid */
  private static final int NOTIFIKATION_ID = 117;

	@Override
	public void onCreate() {
		Log.d("AfspillerService onCreate!");
	}


  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.d("AfspillerService onStartCommand("+intent+" "+flags+" "+startId);
    handleCommand(intent);
    // We want this service to continue running until it is explicitly
    // stopped, so return sticky.
    return START_STICKY;
  }

  private void handleCommand(Intent intent) {
    Log.d("AfspillerService handleCommand("+intent);
    if (notification == null) {
      notification = new Notification(R.drawable.statusbaricon, null, 0);

      // PendingIntent er til at pege på aktiviteten der skal startes hvis brugeren vælger notifikationen
      notification.contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, Afspilning_akt.class), 0);
      notification.flags |= (Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT);
    }

    String kanalNavn = intent==null?null:intent.getStringExtra("kanalNavn");
    if (kanalNavn==null) kanalNavn="";

    notification.setLatestEventInfo(this, PROGRAMNAVN, kanalNavn, notification.contentIntent);
		startForeground(NOTIFIKATION_ID, notification);
  }



	@Override
	public void onDestroy() {
		Log.d("AfspillerService onDestroy!");
		stopForeground(true);
	}
}
