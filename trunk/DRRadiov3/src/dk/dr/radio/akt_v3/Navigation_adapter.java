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
    elem.add(new MenuElement(4, null, aq(R.layout.nav_elem_overskrift)));
    aq.id(R.id.tekst).text("Kanaloversigt");
    elem.add(new MenuElement(1, null, aq(R.layout.nav_elem_adskiller)));

    elem.add(new MenuElement(1, null, aq(R.layout.nav_elem_overskrift)));
    aq.id(R.id.tekst).text("(fjernes):");
    elem.add(new MenuElement(1, null, aq(R.layout.nav_elem_overskrift)));
    aq.id(R.id.tekst).text("xHØR LIVE RADIOx");
    elem.add(new MenuElement(2, "P1D", aq(R.layout.nav_elem_kanal)));
    aq.id(R.id.billede).image(R.drawable.kanal_p1d);
    elem.add(new MenuElement(2, "P2D", aq(R.layout.nav_elem_kanal)));
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


  public void vælgMenu(FragmentActivity akt, int position) {
    MenuElement e = elem.get(position);
    Bundle b = new Bundle();
    Fragment f;

    if (e.type == 4) {
      f = new KanalViewpager_frag();
    } else if (e.type == 2) {
      f = new Kanal_frag();
      b.putString(Kanal_frag.P_kode, e.data);
    } else {
      App.kortToast("Ikke implementeret");
      f = new Kanal_frag();
      b.putString(Kanal_frag.P_kode, "P3");
    }

    f.setArguments(b);
    FragmentManager fragmentManager = akt.getSupportFragmentManager();
    fragmentManager.beginTransaction().replace(R.id.indhold_frag, f).commit();

  }


  @Override
  public int getCount() {
    return elem.size();
  }

  @Override
  public int getViewTypeCount() {
    return 10;
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

  static class MenuElement {
    final int type;
    final String data;
    final View layout;

    MenuElement(int type, String data, View layout) {
      this.type = type;
      this.data = data;
      this.layout = layout;
    }
  }

}


