package org.imdea.panel.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import org.imdea.panel.Database.BtMessage;
import org.imdea.panel.R;

import java.util.ArrayList;

public abstract class ItemAdapter extends BaseAdapter {

    private ArrayList<?> items;
    private Context context;

    public ItemAdapter(Context context, ArrayList<?> items) {
        super();
        this.context = context;
        this.items = items;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        while (view == null) {

            BtMessage item = ((BtMessage) items.get(position));

            if (item.isMine) {
                view = vi.inflate(R.layout.list_item_right, null);
            } else {
                view = vi.inflate(R.layout.list_item_left, null);

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