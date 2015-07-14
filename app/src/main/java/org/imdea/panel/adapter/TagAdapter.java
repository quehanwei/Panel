package org.imdea.panel.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import org.imdea.panel.R;

import java.util.ArrayList;

public abstract class TagAdapter extends BaseAdapter {

    private ArrayList<?> items;
    private Context context;

    public TagAdapter(Context context, ArrayList<?> items) {
        super();
        this.context = context;
        this.items = items;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view = vi.inflate(R.layout.row_layout, null);
        onEntrada(items.get(position), view);

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

    /**
     * Devuelve cada una de las items con cada una de las vistas a la que debe de ser asociada
     *
     * @param entrada La entrada que sera la asociada a la view. La entrada es del tipo del paquete/handler
     * @param view    View particular que contendra los datos del paquete/handler
     */
    public abstract void onEntrada(Object entrada, View view);

}