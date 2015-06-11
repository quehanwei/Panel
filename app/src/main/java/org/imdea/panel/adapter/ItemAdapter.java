package org.imdea.panel.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import org.imdea.panel.Bluetooth.Global;
import org.imdea.panel.Database.BtMessage;
import org.imdea.panel.R;

import java.util.ArrayList;

public abstract class ItemAdapter extends BaseAdapter {

    private ArrayList<?> items;
    private int R_layout_IdView;
    private Context context;

    public ItemAdapter(Context context, int R_layout_IdView, ArrayList<?> items) {
        super();
        this.context = context;
        this.items = items;
        this.R_layout_IdView = R_layout_IdView;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        if (view == null) {
            try {
                if (((BtMessage) items.get(position)).origin_mac_address.equals(Global.DEVICE_ADDRESS)) {
                    LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = vi.inflate(R.layout.list_item_right, null);
                } else {
                    LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = vi.inflate(R.layout.list_item_left, null);
                }
            } catch (Exception e) {
                LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = vi.inflate(R_layout_IdView, null);
            }

        }

        onEntrada (items.get(position), view);

        return view;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    /** Devuelve cada una de las items con cada una de las vistas a la que debe de ser asociada
     * @param entrada La entrada que será la asociada a la view. La entrada es del tipo del paquete/handler
     * @param view View particular que contendrá los datos del paquete/handler
     */
    public abstract void onEntrada (Object entrada, View view);

}