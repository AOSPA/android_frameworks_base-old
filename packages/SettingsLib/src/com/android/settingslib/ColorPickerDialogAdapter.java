package com.android.settingslib;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

public class ColorPickerDialogAdapter extends BaseAdapter {

    private Context mContext;
    private int[] mColors;
    private int mRowHeight = 200;
    private int mSelectedPosition = 0;
    private int mSelectedImageResourceId = 0;
    private Integer mSelectedImageColorFilter;

    public ColorPickerDialogAdapter(Context context, int[] colors) {
        mContext = context;
        mColors = colors;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;

        if (convertView == null) {
            imageView = new ImageView(mContext);
            imageView.setLayoutParams(new GridView.LayoutParams(
                GridView.LayoutParams.FILL_PARENT, mRowHeight));
            imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

            if (mSelectedImageColorFilter != null) {
                imageView.setColorFilter(mSelectedImageColorFilter);
            }
        } else {
            imageView = (ImageView) convertView;

            if (position != mSelectedPosition) {
                imageView.setImageResource(0);
            }
        }

        imageView.setBackgroundColor(mColors[position]);
        if (position == mSelectedPosition) {
            imageView.setImageResource(mSelectedImageResourceId);
        }

        return imageView;
    }

    @Override
    public int getCount() {
        return mColors.length;
    }

    @Override
    public Object getItem(int position) {
        return mColors[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public int[] getColors() {
        return mColors;
    }

    public void setColors(int[] colors) {
        mColors = colors;
    }

    public int getRowHeight() {
        return mRowHeight;
    }

    public void setRowHeight(int rowHeight) {
        mRowHeight = rowHeight;
    }

    public void setSelectedPosition(int position) {
        mSelectedPosition = position;
    }

    public int getSelectedPosition() {
        return mSelectedPosition;
    }

    public void setSelectedImageResourceId(int resourceId) {
        mSelectedImageResourceId = resourceId;
    }

    public int getSelectedImageResourceId() {
        return mSelectedImageResourceId;
    }

    public void setSelectedImageColorFilter(int selectedImageColorFilter) {
        mSelectedImageColorFilter = selectedImageColorFilter;
    }

    public Integer setSelectedImageColorFilter() {
        return mSelectedImageColorFilter;
    }

    public int getSelectedColor() {
        return mColors[mSelectedPosition];
    }

    public void setSelectedColor(int color) {
        for(int i = 0; i < mColors.length; i++) {
            if (mColors[i] == color) {
                setSelectedPosition(i);
                return;
            }
        }
    }

}
