package dk.dr.radio.akt_v3;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import dk.dr.radio.data.DRData;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.v3.R;

import static dk.dr.radio.akt_v3.BasisAktivitet.putString;

public class KanalViewpager_frag extends BasisFragment {


  private ListeAkt.VariabelFaneAdapter faneAdapter;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    setContentView(R.layout.v3_liste_akt, inflater, container);
    faneAdapter = new ListeAkt.VariabelFaneAdapter(getActivity().getSupportFragmentManager());
    Bundle args = new Bundle();


    Log.d("xxx DRData.instans.stamdata.kanalkoder=" + DRData.instans.stamdata.kanalkoder);

    for (String kode : DRData.instans.stamdata.kanalkoder) {
      faneAdapter.tilføj(kode, new Kanal_frag(), putString(args, Kanal_frag.P_kode, kode));
    }

//    faneAdapter.tilføj("P1", new Kanal_frag(), putString(args, Kanal_frag.P_kode, "P1D"));
    //faneAdapter.tilføj("fremtidige", new MineOpkaldFrag(), putString(args, MineOpkaldFrag.P_url, "1"));
//    faneAdapter.tilføj("P3", new Kanal_frag(), putString(args, Kanal_frag.P_url, "http://www.dr.dk/tjenester/mu-apps/schedule/P3/0"));
    ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
    viewPager.setAdapter(faneAdapter);

    return rod;
  }


  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    ((Navigation_akt) activity).sætTitel("KanalViewpager");
  }
}

