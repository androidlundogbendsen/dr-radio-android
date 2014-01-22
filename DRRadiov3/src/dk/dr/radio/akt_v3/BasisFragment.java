/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.dr.radio.akt_v3;

import android.app.Activity;
import android.database.Cursor;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.androidquery.AQuery;

import dk.dr.radio.diverse.Log;

/**
 * @author j
 */
//public class BasisFragment extends DialogFragment {
public class BasisFragment extends Fragment {

  protected AQuery aq;
  protected View rod;

  protected void setContentView(int layoutRes, LayoutInflater inflater, ViewGroup container) {
    Log.d("Viser fragment " + this);
    rod = inflater.inflate(layoutRes, container, false);
    aq = new AQuery(rod);
  }

  protected View findViewById(int viewId) {
    return rod.findViewById(viewId);
  }


  public static final int LINKFARVE = 0xff00458f;

  Activity ths;


  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    ths = activity;
  }

  Cursor managedCursor = null;

  protected void startManagingCursor(Cursor cursor) {
    managedCursor = cursor;
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    if (managedCursor != null) managedCursor.close();
    rod = null;
    aq = null;
  }


}
