package dk.dr.radio.akt_v3;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.androidquery.AQuery;

import java.util.ArrayList;

import dk.dr.radio.diverse.App;
import dk.dr.radio.v3.R;


class MenuElement {
  final int type;
  final String data;
  final View layout;

  MenuElement(int type, String data, View layout) {
    this.type = type;
    this.data = data;
    this.layout = layout;
  }
}

/**
 * Created by j on 19-01-14.
 */
public class Navigation_adapter extends BasisAdapter {
  private final LayoutInflater layoutInflater;
  private AQuery aq;
  ArrayList<MenuElement> elem = new ArrayList<MenuElement>();

  private View aq(int nav_elem_soeg) {

    View v = layoutInflater.inflate(nav_elem_soeg, null);
    aq = new AQuery(v);
    return v;
  }

  public Navigation_adapter(Context themedContext) {
    layoutInflater = (LayoutInflater) themedContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    aq = new AQuery(themedContext);
    elem.add(new MenuElement(0, null, aq(R.layout.nav_elem_soeg)));
    elem.add(new MenuElement(1, null, aq(R.layout.nav_elem_adskiller)));
    elem.add(new MenuElement(1, null, aq(R.layout.nav_elem_overskrift)));
    aq.id(R.id.tekst).text("HØR LIVE RADIO");
    elem.add(new MenuElement(2, "P1", aq(R.layout.nav_elem_kanal)));
    aq.id(R.id.billede).image(R.drawable.kanal_p1d);
    elem.add(new MenuElement(2, "P2", aq(R.layout.nav_elem_kanal)));
    aq.id(R.id.billede).image(R.drawable.kanal_p2d);
    elem.add(new MenuElement(2, "P3", aq(R.layout.nav_elem_kanal)));
    aq.id(R.id.billede).image(R.drawable.kanal_p3);
    elem.add(new MenuElement(2, "P4", aq(R.layout.nav_elem_kanal)));
    aq.id(R.id.billede).image(R.drawable.kanal_p4).id(R.id.p4åbn).visible();
    elem.add(new MenuElement(3, "P4K", aq(R.layout.nav_elem_kanaltekst)));
    aq.id(R.id.tekst).text("P4 København");
    elem.add(new MenuElement(3, "P4S", aq(R.layout.nav_elem_kanaltekst)));
    aq.id(R.id.tekst).text("P4 Sjælland");
    elem.add(new MenuElement(1, null, aq(R.layout.nav_elem_adskiller)));
    elem.add(new MenuElement(3, null, aq(R.layout.nav_elem_overskrift)));
    aq.id(R.id.tekst).text("Senest lyttede");
    elem.add(new MenuElement(3, null, aq(R.layout.nav_elem_overskrift)));
    aq.id(R.id.tekst).text("Dine favoritprogrammer");
    elem.add(new MenuElement(3, null, aq(R.layout.nav_elem_overskrift)));
    aq.id(R.id.tekst).text("Downloadede udsendelser");
    elem.add(new MenuElement(3, null, aq(R.layout.nav_elem_overskrift)));
    aq.id(R.id.tekst).text("Alle programmer A-Å");
    elem.add(new MenuElement(1, null, aq(R.layout.nav_elem_adskiller)));
    elem.add(new MenuElement(3, null, aq(R.layout.nav_elem_overskrift)));
    aq.id(R.id.tekst).text("Kontakt / info / om");
  }


  @Override
  public int getCount() {
    return elem.size();
  }

  @Override
  public int getViewTypeCount() {
    return 4;
  }

  @Override
  public boolean isEnabled(int position) {
    return elem.get(position).type >= 2;
  }

  @Override
  public int getItemViewType(int position) {
    return elem.get(position).type;
  }


  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    return elem.get(position).layout;
  }


  public void vælgMenu(FragmentActivity akt, int position) {

    // update the main content by replacing fragments
    Fragment f;
    if (position == 1) {
      App.kortToast("P1");
      f = new Kanal_frag();
      Bundle b = new Bundle();
      b.putString(Kanal_frag.P_navn, "P1");
      b.putString(Kanal_frag.P_url, "http://www.dr.dk/tjenester/mu-apps/schedule/P1D/0");
      f.setArguments(b);
    } else if (position == 3) {
      App.kortToast("P3");
      f = new Kanal_frag();
      Bundle b = new Bundle();
      b.putString(Kanal_frag.P_navn, "P3");
      b.putString(Kanal_frag.P_url, "http://www.dr.dk/tjenester/mu-apps/schedule/P3/0");
      f.setArguments(b);
    } else {
      f = new KanalViewpager_frag();
      Bundle b = new Bundle();
      f.setArguments(b);
    }

    FragmentManager fragmentManager = akt.getSupportFragmentManager();
    fragmentManager.beginTransaction().replace(R.id.indhold_frag, f).commit();

  }
}


