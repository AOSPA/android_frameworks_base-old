package com.android.settingslib;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.view.View;
import android.view.Window;

public class ColorPickerDialog extends Dialog {

    private Context mContext;
    private int mColumns = 4;

    private GridView mGridView;
    private ColorPickerDialogAdapter mAdapter;

    private View.OnClickListener mOnCancelListener;
    private View.OnClickListener mOnDefaultListener;
    private View.OnClickListener mOnOkListener;
    private OnColorSelectedListener mOnColorSelectedListener;

    public ColorPickerDialog(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.color_picker_dialog);

        final ColorPickerDialogAdapter adapter = getAdapter();

        mGridView = (GridView) findViewById(R.id.grid_view);
        mGridView.setNumColumns(mColumns);
        mGridView.setAdapter(adapter);
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View v,
                    int position, long id) {
                adapter.setSelectedPosition(position);
                refreshPalette();

                if (mOnColorSelectedListener != null) {
                    mOnColorSelectedListener.onColorSelected(ColorPickerDialog.this,
                        adapter.getSelectedColor());
                }
            }

        });

        if (mOnCancelListener != null) {
            Button cancelButton = (Button) findViewById(R.id.button_cancel);
            cancelButton.setOnClickListener(mOnCancelListener);
        }

        if (mOnDefaultListener != null) {
            Button defaultButton = (Button) findViewById(R.id.button_default);
            defaultButton.setOnClickListener(mOnDefaultListener);
        }

        if (mOnOkListener != null) {
            Button okButton = (Button) findViewById(R.id.button_ok);
            okButton.setOnClickListener(mOnOkListener);
        }
    }

    public int getColumns() {
        return mColumns;
    }

    public void setColumns(int columns) {
        mColumns = columns;
    }

    public void setOnCancelListener(View.OnClickListener listener) {
        mOnCancelListener = listener;
    }

    public void setOnDefaultListener(View.OnClickListener listener) {
        mOnDefaultListener = listener;
    }

    public void setOnOkListener(View.OnClickListener listener) {
        mOnOkListener = listener;
    }

    public void setOnColorSelectedListener(OnColorSelectedListener listener) {
        mOnColorSelectedListener = listener;
    }

    public void setColors(int[] colors, int selectedColor) {
        final ColorPickerDialogAdapter adapter = getAdapter();
        adapter.setColors(colors);
        adapter.setSelectedColor(selectedColor);
        refreshPalette();
    }

    public void setColors(int[] colors) {
        getAdapter().setColors(colors);
    }

    public int getSelectedColor() {
        return getAdapter().getSelectedColor();
    }

    public void setSelectedColor(int selectedColor) {
        getAdapter().setSelectedColor(selectedColor);
        refreshPalette();
    }

    public ColorPickerDialogAdapter getAdapter() {
        if (mAdapter == null) {
            final Resources res = mContext.getResources();

            mAdapter = new ColorPickerDialogAdapter(mContext,
                res.getIntArray(R.array.color_picker_dialog_colors));
        }

        return mAdapter;
    }

    public void setAdapter(ColorPickerDialogAdapter adapter) {
        mAdapter = adapter;
    }

    private void refreshPalette() {
        getAdapter().notifyDataSetChanged();
    }

    public interface OnColorSelectedListener {
        public void onColorSelected(DialogInterface dialog, int color);
    }
}
