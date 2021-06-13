package games.moisoni.google_inapp_billing;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;

public class TextViewOutline extends androidx.appcompat.widget.AppCompatTextView {

    private static final int DEFAULT_OUTLINE_SIZE = 0;
    private static final int DEFAULT_OUTLINE_COLOR = Color.TRANSPARENT;

    private int mOutlineSize;
    private int mOutlineColor;
    private int mTextColor;
    private float mShadowRadius;
    private float mShadowDx;
    private float mShadowDy;
    private int mShadowColor;

    public TextViewOutline(Context context) {
        this(context, null);
    }

    public TextViewOutline(Context context, AttributeSet attrs) {
        super(context, attrs);
        setAttributes(attrs);
    }

    private void setAttributes(AttributeSet attrs) {
        mOutlineSize = DEFAULT_OUTLINE_SIZE;
        mOutlineColor = DEFAULT_OUTLINE_COLOR;

        mTextColor = getCurrentTextColor();
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.TextViewOutline);
            if (a.hasValue(R.styleable.TextViewOutline_outlineSize)) {
                mOutlineSize = (int) a.getDimension(R.styleable.TextViewOutline_outlineSize, DEFAULT_OUTLINE_SIZE);
            }

            if (a.hasValue(R.styleable.TextViewOutline_outlineColor)) {
                mOutlineColor = a.getColor(R.styleable.TextViewOutline_outlineColor, DEFAULT_OUTLINE_COLOR);
            }

            //the reason we take shadow from attributes is because we use API level 15 and only from 16 we have the get methods for the shadow attributes
            if (a.hasValue(R.styleable.TextViewOutline_android_shadowRadius)
                    || a.hasValue(R.styleable.TextViewOutline_android_shadowDx)
                    || a.hasValue(R.styleable.TextViewOutline_android_shadowDy)
                    || a.hasValue(R.styleable.TextViewOutline_android_shadowColor)) {
                mShadowRadius = a.getFloat(R.styleable.TextViewOutline_android_shadowRadius, 0);
                mShadowDx = a.getFloat(R.styleable.TextViewOutline_android_shadowDx, 0);
                mShadowDy = a.getFloat(R.styleable.TextViewOutline_android_shadowDy, 0);
                mShadowColor = a.getColor(R.styleable.TextViewOutline_android_shadowColor, Color.TRANSPARENT);
            }

            a.recycle();
        }

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setPaintToOutline();
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private void setPaintToOutline() {
        Paint paint = getPaint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(mOutlineSize);
        super.setTextColor(mOutlineColor);
        super.setShadowLayer(0, 0, 0, Color.TRANSPARENT);

    }

    private void setPaintToRegular() {
        Paint paint = getPaint();
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(0);
        super.setTextColor(mTextColor);
        super.setShadowLayer(mShadowRadius, mShadowDx, mShadowDy, mShadowColor);
    }


    @Override
    public void setTextColor(int color) {
        super.setTextColor(color);
        mTextColor = color;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        setPaintToOutline();
        super.onDraw(canvas);

        setPaintToRegular();
        super.onDraw(canvas);
    }
}