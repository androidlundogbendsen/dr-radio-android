
package dk.dr.radio.diverse;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class DatabaseHelper extends SQLiteOpenHelper {
  public static final int DATABASEVERSION = 1;

  public enum Hentede {
    downloadId, slug ;

    //public static String OPRET = _id + " integer PRIMARY KEY autoincrement, "+slug;
    public static String OPRET = slug+" PRIMARY KEY, "+ downloadId;
    public static String TABEL = "Hentede";
  };

    DatabaseHelper() {
        super(App.instans, "DRRadio.db", null, DATABASEVERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE "+Hentede.TABEL+" ("+Hentede.OPRET+");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(this+ " Opgraderer fra version " + oldVersion + " til " + newVersion + "");
        db.execSQL("DROP TABLE IF EXISTS Hentede");
        onCreate(db);
    }
}
