/*
 * Copyright (C) 2013 ParanoidAndroid Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.statusbar.phone;

import android.view.DragEvent;
import android.view.View;
import android.view.View.OnDragListener;
import android.util.Log;

class QuickSettingsDragListener implements OnDragListener {

    @Override
    public boolean onDrag(View v, DragEvent event) {
        QuickSettingsTileView draggedView
                = (QuickSettingsTileView) event.getLocalState();
        QuickSettingsTileView droppedView = (QuickSettingsTileView) v;
        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                break;
            case DragEvent.ACTION_DRAG_ENTERED:
                break;
            case DragEvent.ACTION_DRAG_EXITED:
                break;
            case DragEvent.ACTION_DROP:
                float x1 = draggedView.getX();
                float y1 = draggedView.getY();

                draggedView.setX(droppedView.getX());
                draggedView.setY(droppedView.getY());
                droppedView.setX(x1);
                droppedView.setY(y1);
                break;
            case DragEvent.ACTION_DRAG_ENDED:
                break;
            default:
                break;
        }
        return true;
    }

}
