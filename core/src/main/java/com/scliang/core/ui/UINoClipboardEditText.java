package com.scliang.core.ui;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2018/5/17.
 */
public class UINoClipboardEditText extends androidx.appcompat.widget.AppCompatEditText {
//    private static final int ID_SELECT_ALL = android.R.id.selectAll;
//    private static final int ID_UNDO = android.R.id.undo;
//    private static final int ID_REDO = android.R.id.redo;
//    private static final int ID_CUT = android.R.id.cut;
//    private static final int ID_COPY = android.R.id.copy;
//    private static final int ID_PASTE = android.R.id.paste;
//    private static final int ID_SHARE = android.R.id.shareText;
//    private static final int ID_PASTE_AS_PLAIN_TEXT = android.R.id.pasteAsPlainText;
//    private static final int ID_REPLACE = android.R.id.replaceText;
//    private static final int ID_ASSIST = android.R.id.textAssist;
//    private static final int ID_AUTOFILL = android.R.id.autofill;

    public UINoClipboardEditText(Context context) {
        super(context);
        init();
    }

    public UINoClipboardEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public UINoClipboardEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setLongClickable(false);
        setTextIsSelectable(false);
        setCustomSelectionActionModeCallback(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            setCustomInsertionActionModeCallback(new ActionMode.Callback() {
                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    return false;
                }

                @Override
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    return false;
                }

                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                    return false;
                }

                @Override
                public void onDestroyActionMode(ActionMode mode) {
                }
            });
        }
    }

    @Override
    public boolean performLongClick() {
//        return super.performLongClick();
        return true;
    }

    @Override
    public boolean performLongClick(float x, float y) {
//        return super.performLongClick(x, y);
        return true;
    }

    @Override
    public boolean onTextContextMenuItem(int id) {
//        return super.onTextContextMenuItem(id);
        return true;
    }
}
