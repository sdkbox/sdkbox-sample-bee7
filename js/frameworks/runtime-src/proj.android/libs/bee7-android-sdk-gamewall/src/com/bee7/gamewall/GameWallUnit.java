package com.bee7.gamewall;


import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.bee7.sdk.publisher.GameWallConfiguration;
import com.bee7.sdk.publisher.appoffer.AppOffer;
import com.bee7.sdk.publisher.appoffer.AppOfferWithResult;

/**
 * Base abstract class for all gamewall units
 */
public abstract class GameWallUnit extends LinearLayout {

    protected int index;
    protected int indexV;
    protected int column;

    public GameWallUnit(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GameWallUnit(Context context, int index, int indexV, int column) {
        super(context);
        this.index = index;
        this.indexV = indexV;
        this.column = column;
    }

    /**
     * If this method is called on a game wall unit with one app offer the appOfferId is ignored and
     * unit's appOffer is returned.
     *
     * If It's called on a unit with multiple app offers, correct appOffer is returned. If no app
     * offer is found it returns null.
     */
    public abstract AppOffer getAppOffer(String appOfferId);

    public abstract AppOfferWithResult getAppOfferWithResult(String appOfferId);

    /**
     * Updates game wall unit with appOffer
     */
    public abstract void update(AppOffer appOffer);

    /**
     * Updates game wall unit with location parameters
     *
     * @param index
     * @param indexV
     * @param column
     */
    public void update(int index, int indexV, int column) {
        this.index = index;
        this.indexV = indexV;
        this.column = column;
    }

    /**
     * @return column number
     */
    public int getColumn() {
        return column;
    }

    /**
     * @return index of view in column
     */
    public int getIndex() {
        return index;
    }
}
