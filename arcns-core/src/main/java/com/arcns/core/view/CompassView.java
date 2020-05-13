package com.arcns.core.view;

import android.animation.PropertyValuesHolder;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

import com.arcns.core.R;


/**
 * 指南针View
 */

public class CompassView extends View {
    private Canvas mCanvas;
    private Context mContext;
    //View矩形的宽度
    private int width;
    //指南针圆心点坐标
    private int mCenterX;
    private int mCenterY;
    //外圆半径
    private int mOutSideRadius;
    //外接圆半径
    private int mCircumRadius;
    //指南针文字大小空间高度
    private int mCurrentDirectionTextHeight;

    // 颜色
    private int mInnerShaderCenterColor;
    private int mInnerShaderEdgeColor;
    private int mCurrentDirectionTextColor;
    private int mCurrentDegreeTextColor;
    private int mDirectionLineMainColor;
    private int mDirectionLineMinorColor;
    private int mDirectionDegreeTextColor;
    private int mDirectionNorthTextColor;
    private int mDirectionESWTextColor;
    private int mOuterRing1Color;
    private int mOuterRing2Color;
    private int mOuterRing3Color;
    private int mContourCircleColor;
    private int mContourCircleOffsetColor;
    // 数值
    private float mDirectionNeswTextSize;
    private float mDirectionDegreeTextSize;
    private float mCurrentDirectionTextSize;
    private float mCurrentDegreeTextSize;

    //内心圆是一个颜色辐射渐变的圆
    private Shader mInnerShader;
    private Paint mInnerPaint;
    // 外圈笔
    private Paint mCompassOuterRingPaint1, mCompassOuterRingPaint2, mCompassOuterRingPaint3;
    private Paint mOuterTrianglePaint;//三角笔
    // 轮廓圈笔
    private Paint mCompassContourCircleOffsetPaint;//偏转角度
    private Paint mCompassContourCirclePaint;
    private Paint mCompassContourCircleTrianglePaint; //三角形
    // 方向
    private Paint mDirectionNorthTextPaint;//北
    private Paint mDirectionESWTextPaint;//东西南
    private Paint mDirectionDegreeTextPaint;//其他方向度数
    private Paint mDirectionLineMinorPaint;//次要刻度
    private Paint mDirectionLineMainPaint;//主要刻度
    //指南针上面的当前方向文字笔
    private Paint mCurrentDirectionTextPaint;
    //中心的度数文字笔
    private Paint mCurrentDegreeTextPaint;
    //指南针上面文字的外接矩形,用来测文字大小让文字居中
    private Rect mTextRect;
    //外圈小三角形的Path
    private Path mOutsideTriangle;
    //外接圆小三角形的Path
    private Path mCircumTriangle;

    //NESW文字外接矩形
    private Rect mPositionRect;
    //两位数的
    private Rect mSencondRect;
    //三位数的
    private Rect mThirdRect;
    //圆心数字矩形
    private Rect mCenterTextRect;

    //定义个点击属性动画
    private ValueAnimator mValueAnimator;
    // camera绕X轴旋转的角度
    private float mCameraRotateX;
    // camera绕Y轴旋转的角度
    private float mCameraRotateY;
    //camera最大旋转角度
    private float mMaxCameraRotate = 10;

    // camera绕X轴旋转的角度
    private float mCameraTranslateX;
    // camera绕Y轴旋转的角度
    private float mCameraTranslateY;
    //camera最大旋转角度
    private float mMaxCameraTranslate;
    //camera矩阵
    private Matrix mCameraMatrix;
    //设置camera
    private Camera mCamera;

    private float val = 0f;
    private float valCompare;

    //方位文字
    private String text = "北";

    // 是否开启触摸模式
    private Boolean isTouch = false;

    public float getVal() {
        return val;
    }

    public void setVal(float val) {
        if (this.val == val) {
            return;
        }
        this.val = val;
        invalidate();
    }

    public Boolean getIsTouch() {
        return isTouch;
    }

    public void setIsTouch(boolean isTouch) {
        this.isTouch = isTouch;
    }

    public CompassView(Context context) {
        this(context, null);
    }

    public CompassView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CompassView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CompassView);
        // 从属性中获取颜色参数
        mInnerShaderCenterColor = typedArray.getColor(
                R.styleable.CompassView_compass_inner_shader_center_color,
                context.getResources().getColor(R.color.compass_inner_shader_center_color)
        );
        mInnerShaderEdgeColor = typedArray.getColor(
                R.styleable.CompassView_compass_inner_shader_edge_color,
                context.getResources().getColor(R.color.compass_inner_shader_edge_color)
        );
        mCurrentDirectionTextColor = typedArray.getColor(
                R.styleable.CompassView_compass_current_direction_text_color,
                context.getResources().getColor(R.color.compass_current_direction_text_color)
        );
        mCurrentDegreeTextColor = typedArray.getColor(
                R.styleable.CompassView_compass_current_degree_text_color,
                context.getResources().getColor(R.color.compass_current_degree_text_color)
        );
        mDirectionLineMainColor = typedArray.getColor(
                R.styleable.CompassView_compass_direction_line_main_color,
                context.getResources().getColor(R.color.compass_direction_line_main_color)
        );
        mDirectionLineMinorColor = typedArray.getColor(
                R.styleable.CompassView_compass_direction_line_minor_color,
                context.getResources().getColor(R.color.compass_direction_line_minor_color)
        );
        mDirectionDegreeTextColor = typedArray.getColor(
                R.styleable.CompassView_compass_direction_degree_text_color,
                context.getResources().getColor(R.color.compass_direction_degree_text_color)
        );
        mDirectionNorthTextColor = typedArray.getColor(
                R.styleable.CompassView_compass_direction_north_text_color,
                context.getResources().getColor(R.color.compass_direction_north_text_color)
        );
        mDirectionESWTextColor = typedArray.getColor(
                R.styleable.CompassView_compass_direction_esw_text_color,
                context.getResources().getColor(R.color.compass_direction_esw_text_color)
        );
        mOuterRing1Color = typedArray.getColor(
                R.styleable.CompassView_compass_outer_ring_1_color,
                context.getResources().getColor(R.color.compass_outer_ring_1_color)
        );
        mOuterRing2Color = typedArray.getColor(
                R.styleable.CompassView_compass_outer_ring_2_color,
                context.getResources().getColor(R.color.compass_outer_ring_2_color)
        );
        mOuterRing3Color = typedArray.getColor(
                R.styleable.CompassView_compass_outer_ring_3_color,
                context.getResources().getColor(R.color.compass_outer_ring_3_color)
        );
        mContourCircleColor = typedArray.getColor(
                R.styleable.CompassView_compass_contour_circle_color,
                context.getResources().getColor(R.color.compass_contour_circle_color)
        );
        mContourCircleOffsetColor = typedArray.getColor(
                R.styleable.CompassView_compass_contour_circle_offset_color,
                context.getResources().getColor(R.color.compass_contour_circle_offset_color)
        );
        // 从属性中获取数值参数
        mDirectionNeswTextSize = typedArray.getDimension(
                R.styleable.CompassView_compass_direction_nesw_text_size,
                context.getResources().getDimension(R.dimen.compass_direction_nesw_text_size)
        );
        mDirectionDegreeTextSize = typedArray.getDimension(
                R.styleable.CompassView_compass_direction_degree_text_size,
                context.getResources().getDimension(R.dimen.compass_direction_degree_text_size)
        );
        mCurrentDirectionTextSize = typedArray.getDimension(
                R.styleable.CompassView_compass_current_direction_text_size,
                context.getResources().getDimension(R.dimen.compass_current_direction_text_size)
        );
        mCurrentDegreeTextSize = typedArray.getDimension(
                R.styleable.CompassView_compass_current_degree_text_size,
                context.getResources().getDimension(R.dimen.compass_current_degree_text_size)
        );
        mCurrentDirectionTextHeight = (int) typedArray.getDimension(
                R.styleable.CompassView_compass_current_direction_text_height,
                context.getResources().getDimension(R.dimen.compass_current_direction_text_height)
        );

        // 初始化画笔
        mCompassOuterRingPaint1 = new Paint();
        mCompassOuterRingPaint1.setStyle(Paint.Style.STROKE);
        mCompassOuterRingPaint1.setAntiAlias(true);
        mCompassOuterRingPaint1.setColor(mOuterRing1Color);

        mCompassOuterRingPaint2 = new Paint();
        mCompassOuterRingPaint2.setStyle(Paint.Style.STROKE);
        mCompassOuterRingPaint2.setAntiAlias(true);
        mCompassOuterRingPaint2.setColor(mOuterRing2Color);

        mCompassOuterRingPaint3 = new Paint();
        mCompassOuterRingPaint3.setStyle(Paint.Style.STROKE);
        mCompassOuterRingPaint3.setAntiAlias(true);
        mCompassOuterRingPaint3.setColor(mOuterRing3Color);


        mDirectionLineMainPaint = new Paint();
        mDirectionLineMainPaint.setStyle(Paint.Style.STROKE);
        mDirectionLineMainPaint.setAntiAlias(true);
        mDirectionLineMainPaint.setColor(mDirectionLineMainColor);

        mDirectionLineMinorPaint = new Paint();
        mDirectionLineMinorPaint.setStyle(Paint.Style.FILL);
        mDirectionLineMinorPaint.setAntiAlias(true);
        mDirectionLineMinorPaint.setColor(mDirectionLineMinorColor);

        mCurrentDirectionTextPaint = new Paint();
        mCurrentDirectionTextPaint.setStyle(Paint.Style.FILL);
        mCurrentDirectionTextPaint.setAntiAlias(true);
        mCurrentDirectionTextPaint.setTextSize(mCurrentDirectionTextSize);
        mCurrentDirectionTextPaint.setFakeBoldText(true);
        mCurrentDirectionTextPaint.setColor(mCurrentDirectionTextColor);


        mOuterTrianglePaint = new Paint();
        mOuterTrianglePaint.setStyle(Paint.Style.FILL);
        mOuterTrianglePaint.setAntiAlias(true);
        mOuterTrianglePaint.setColor(mOuterRing2Color);

        mTextRect = new Rect();
        mOutsideTriangle = new Path();
        mCircumTriangle = new Path();

        mDirectionNorthTextPaint = new Paint();
        mDirectionNorthTextPaint.setStyle(Paint.Style.FILL);
        mDirectionNorthTextPaint.setAntiAlias(true);
        mDirectionNorthTextPaint.setTextSize(mDirectionNeswTextSize);
        mDirectionNorthTextPaint.setFakeBoldText(true);
        mDirectionNorthTextPaint.setColor(mDirectionNorthTextColor);

        mDirectionESWTextPaint = new Paint();
        mDirectionESWTextPaint.setStyle(Paint.Style.FILL);
        mDirectionESWTextPaint.setAntiAlias(true);
        mDirectionESWTextPaint.setTextSize(mDirectionNeswTextSize);
        mDirectionESWTextPaint.setFakeBoldText(true);
        mDirectionESWTextPaint.setColor(mDirectionESWTextColor);

        mDirectionDegreeTextPaint = new Paint();
        mDirectionDegreeTextPaint.setStyle(Paint.Style.FILL);
        mDirectionDegreeTextPaint.setAntiAlias(true);
        mDirectionDegreeTextPaint.setTextSize(mDirectionDegreeTextSize);
        mDirectionDegreeTextPaint.setFakeBoldText(true);
        mDirectionDegreeTextPaint.setColor(mDirectionDegreeTextColor);

        mPositionRect = new Rect();
        mCenterTextRect = new Rect();

        mCurrentDegreeTextPaint = new Paint();
        mCurrentDegreeTextPaint.setStyle(Paint.Style.FILL);
        mCurrentDegreeTextPaint.setAntiAlias(true);
        mCurrentDegreeTextPaint.setTextSize(mCurrentDegreeTextSize);
        mCurrentDegreeTextPaint.setFakeBoldText(true);
        mCurrentDegreeTextPaint.setColor(mCurrentDegreeTextColor);


        mSencondRect = new Rect();
        mThirdRect = new Rect();

        mInnerPaint = new Paint();
        mInnerPaint.setStyle(Paint.Style.FILL);
        mInnerPaint.setAntiAlias(true);

        mCompassContourCircleOffsetPaint = new Paint();
        mCompassContourCircleOffsetPaint.setStyle(Paint.Style.STROKE);
        mCompassContourCircleOffsetPaint.setAntiAlias(true);
        mCompassContourCircleOffsetPaint.setColor(mContourCircleOffsetColor);

        mCompassContourCirclePaint = new Paint();
        mCompassContourCirclePaint.setStyle(Paint.Style.STROKE);
        mCompassContourCirclePaint.setAntiAlias(true);
        mCompassContourCirclePaint.setColor(mContourCircleColor);


        mCompassContourCircleTrianglePaint = new Paint();
        mCompassContourCircleTrianglePaint.setStyle(Paint.Style.FILL);
        mCompassContourCircleTrianglePaint.setAntiAlias(true);
        mCompassContourCircleTrianglePaint.setColor(mContourCircleOffsetColor);

        mCameraMatrix = new Matrix();
        mCamera = new Camera();

    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mCanvas = canvas;
        //设置Camera矩阵 实现3D效果
        set3DMetrix();
        //画文字
        drawText();
        //画指南针外圈
        drawCompassOutSide();
        //画指南针外接圆
        drawCompassCircum();
        //画内部渐变颜色圆
        drawInnerCricle();
        //画指南针内部刻度
        drawCompassDegreeScale();
        //画圆心数字
        drawCenterText();
    }


    /**
     * 设置camera相关
     */
    private void set3DMetrix() {
        mCameraMatrix.reset();
        mCamera.save();
        mCamera.rotateX(mCameraRotateX);
        mCamera.rotateY(mCameraRotateY);
        mCamera.getMatrix(mCameraMatrix);
        mCamera.restore();
        //camera默认旋转是View左上角为旋转中心
        //所以动作之前要，设置矩阵位置 -mTextHeight-mOutSideRadius
        mCameraMatrix.preTranslate(-getWidth() / 2, -getHeight() / 2);
        //动作之后恢复位置
        mCameraMatrix.postTranslate(getWidth() / 2, getHeight() / 2);
        mCanvas.concat(mCameraMatrix);
    }

    private void drawInnerCricle() {
        mInnerShader = new RadialGradient(width / 2, mOutSideRadius + mCurrentDirectionTextHeight, mCircumRadius - 40, mInnerShaderEdgeColor,
                mInnerShaderCenterColor, Shader.TileMode.CLAMP);
        mInnerPaint.setShader(mInnerShader);
        mCanvas.drawCircle(width / 2, mOutSideRadius + mCurrentDirectionTextHeight, mCircumRadius - 40, mInnerPaint);

    }

    private void drawCenterText() {
        String centerText = String.valueOf((int) val + "°");
        mCurrentDegreeTextPaint.getTextBounds(centerText, 0, centerText.length(), mCenterTextRect);
        int centerTextWidth = mCenterTextRect.width();
        int centerTextHeight = mCenterTextRect.height();
        mCanvas.drawText(centerText, width / 2 - centerTextWidth / 2, mCurrentDirectionTextHeight + mOutSideRadius + centerTextHeight / 5, mCurrentDegreeTextPaint);

    }

    private void drawCompassDegreeScale() {
        mCanvas.save();
        //获取N文字的宽度
        mDirectionNorthTextPaint.getTextBounds("N", 0, 1, mPositionRect);
        int mPositionTextWidth = mPositionRect.width();
        int mPositionTextHeight = mPositionRect.height();
        //获取W文字宽度,因为W比较宽 所以要单独获取
        mDirectionNorthTextPaint.getTextBounds("W", 0, 1, mPositionRect);
        int mWPositionTextWidth = mPositionRect.width();
        int mWPositionTextHeight = mPositionRect.height();
        //获取小刻度，两位数的宽度
        mDirectionDegreeTextPaint.getTextBounds("30", 0, 1, mSencondRect);
        int mSencondTextWidth = mSencondRect.width();
        int mSencondTextHeight = mSencondRect.height();
        //获取小刻度，3位数的宽度
        mDirectionDegreeTextPaint.getTextBounds("30", 0, 1, mThirdRect);
        int mThirdTextWidth = mThirdRect.width();
        int mThirdTextHeight = mThirdRect.height();

        mCanvas.rotate(-val, width / 2, mOutSideRadius + mCurrentDirectionTextHeight);


        //画刻度线
        for (int i = 0; i < 240; i++) {

            if (i == 0 || i == 60 || i == 120 || i == 180) {
                mCanvas.drawLine(getWidth() / 2, mCurrentDirectionTextHeight + mOutSideRadius - mCircumRadius + 10,
                        getWidth() / 2, mCurrentDirectionTextHeight + mOutSideRadius - mCircumRadius + 30, mDirectionLineMainPaint);
            } else {
                mCanvas.drawLine(getWidth() / 2, mCurrentDirectionTextHeight + mOutSideRadius - mCircumRadius + 10,
                        getWidth() / 2, mCurrentDirectionTextHeight + mOutSideRadius - mCircumRadius + 30, mDirectionLineMinorPaint);
            }
            if (i == 0) {
                mCanvas.drawText("N", this.width / 2 - mPositionTextWidth / 2, mCurrentDirectionTextHeight + mOutSideRadius - mCircumRadius + 40 + mPositionTextHeight, mDirectionNorthTextPaint);
            } else if (i == 60) {
                mCanvas.drawText("E", this.width / 2 - mPositionTextWidth / 2, mCurrentDirectionTextHeight + mOutSideRadius - mCircumRadius + 40 + mPositionTextHeight, mDirectionESWTextPaint);
            } else if (i == 120) {
                mCanvas.drawText("S", this.width / 2 - mPositionTextWidth / 2, mCurrentDirectionTextHeight + mOutSideRadius - mCircumRadius + 40 + mPositionTextHeight, mDirectionESWTextPaint);
            } else if (i == 180) {
                mCanvas.drawText("W", this.width / 2 - mWPositionTextWidth / 2, mCurrentDirectionTextHeight + mOutSideRadius - mCircumRadius + 40 + mWPositionTextHeight, mDirectionESWTextPaint);
            } else if (i == 20) {
                mCanvas.drawText("30", this.width / 2 - mSencondTextWidth / 2, mCurrentDirectionTextHeight + mOutSideRadius - mCircumRadius + 40 + mSencondTextHeight, mDirectionDegreeTextPaint);
            } else if (i == 40) {
                mCanvas.drawText("60", this.width / 2 - mSencondTextWidth / 2, mCurrentDirectionTextHeight + mOutSideRadius - mCircumRadius + 40 + mSencondTextHeight, mDirectionDegreeTextPaint);
            } else if (i == 80) {
                mCanvas.drawText("120", this.width / 2 - mThirdTextWidth / 2, mCurrentDirectionTextHeight + mOutSideRadius - mCircumRadius + 40 + mThirdTextHeight, mDirectionDegreeTextPaint);
            } else if (i == 100) {
                mCanvas.drawText("150", this.width / 2 - mThirdTextWidth / 2, mCurrentDirectionTextHeight + mOutSideRadius - mCircumRadius + 40 + mThirdTextHeight, mDirectionDegreeTextPaint);
            } else if (i == 140) {
                mCanvas.drawText("210", this.width / 2 - mThirdTextWidth / 2, mCurrentDirectionTextHeight + mOutSideRadius - mCircumRadius + 40 + mThirdTextHeight, mDirectionDegreeTextPaint);
            } else if (i == 160) {
                mCanvas.drawText("240", this.width / 2 - mThirdTextWidth / 2, mCurrentDirectionTextHeight + mOutSideRadius - mCircumRadius + 40 + mThirdTextHeight, mDirectionDegreeTextPaint);
            } else if (i == 200) {
                mCanvas.drawText("300", this.width / 2 - mThirdTextWidth / 2, mCurrentDirectionTextHeight + mOutSideRadius - mCircumRadius + 40 + mThirdTextHeight, mDirectionDegreeTextPaint);
            } else if (i == 220) {
                mCanvas.drawText("330", this.width / 2 - mThirdTextWidth / 2, mCurrentDirectionTextHeight + mOutSideRadius - mCircumRadius + 40 + mThirdTextHeight, mDirectionDegreeTextPaint);
            }
            mCanvas.rotate(1.5f, mCenterX, mOutSideRadius + mCurrentDirectionTextHeight);
        }
        mCanvas.restore();

    }

    /**
     * 指南针外接圆，和外部圆换道理差不多
     */
    private void drawCompassCircum() {
        mCanvas.save();
        //外接圆小三角形的高度
        int mTriangleHeight = (mOutSideRadius - mCircumRadius) / 2;

        mCanvas.rotate(-val, width / 2, mOutSideRadius + mCurrentDirectionTextHeight);
        mCircumTriangle.moveTo(width / 2, mTriangleHeight + mCurrentDirectionTextHeight);
        //内接三角形的边长,简单数学运算
        float mTriangleSide = (float) ((mTriangleHeight / (Math.sqrt(3))) * 2);
        mCircumTriangle.lineTo(width / 2 - mTriangleSide / 2, mCurrentDirectionTextHeight + mTriangleHeight * 2);
        mCircumTriangle.lineTo(width / 2 + mTriangleSide / 2, mCurrentDirectionTextHeight + mTriangleHeight * 2);
        mCircumTriangle.close();
        mCanvas.drawPath(mCircumTriangle, mCompassContourCircleTrianglePaint);
        mCanvas.drawArc(new RectF(width / 2 - mCircumRadius, mCurrentDirectionTextHeight + mOutSideRadius - mCircumRadius,
                width / 2 + mCircumRadius, mCurrentDirectionTextHeight + mOutSideRadius + mCircumRadius), -85, 350, false, mCompassContourCirclePaint);
//        mCanvas.drawArc(width / 2 - mCircumRadius, mTextHeight + mOutSideRadius - mCircumRadius,
//                width / 2 + mCircumRadius, mTextHeight + mOutSideRadius + mCircumRadius, -85, 350, false, mCompassContourCirclePaint);
        mCompassContourCirclePaint.setStrokeWidth(8f);
        mCompassContourCircleOffsetPaint.setStrokeWidth(8f);
        if (val <= 180) {
            valCompare = val;
//            mCanvas.drawArc(width / 2 - mCircumRadius, mTextHeight + mOutSideRadius - mCircumRadius,
//                    width / 2 + mCircumRadius, mTextHeight + mOutSideRadius + mCircumRadius, -85, valCompare, false, mCompassContourCircleOffsetPaint);
            mCanvas.drawArc(new RectF(width / 2 - mCircumRadius, mCurrentDirectionTextHeight + mOutSideRadius - mCircumRadius,
                    width / 2 + mCircumRadius, mCurrentDirectionTextHeight + mOutSideRadius + mCircumRadius), -85, valCompare, false, mCompassContourCircleOffsetPaint);
        } else {
            valCompare = 360 - val;
//            mCanvas.drawArc(width / 2 - mCircumRadius, mTextHeight + mOutSideRadius - mCircumRadius,
//                    width / 2 + mCircumRadius, mTextHeight + mOutSideRadius + mCircumRadius, -95, -valCompare, false, mCompassContourCircleOffsetPaint);
            mCanvas.drawArc(new RectF(width / 2 - mCircumRadius, mCurrentDirectionTextHeight + mOutSideRadius - mCircumRadius,
                    width / 2 + mCircumRadius, mCurrentDirectionTextHeight + mOutSideRadius + mCircumRadius), -95, -valCompare, false, mCompassContourCircleOffsetPaint);
        }

        mCanvas.restore();
    }

    /**
     * 指南针外部可简单分为两部分
     * 1、用Path实现小三角形
     * 2、两个圆弧
     */
    private void drawCompassOutSide() {
        mCanvas.save();
        //小三角形的高度
        int mTriangleHeight = 40;
        //定义Path画小三角形
        mOutsideTriangle.moveTo(width / 2, mCurrentDirectionTextHeight - mTriangleHeight);
        //小三角形的边长
        float mTriangleSide = 46.18f;
        //画出小三角形
        mOutsideTriangle.lineTo(width / 2 - mTriangleSide / 2, mCurrentDirectionTextHeight);
        mOutsideTriangle.lineTo(width / 2 + mTriangleSide / 2, mCurrentDirectionTextHeight);
        mOutsideTriangle.close();
        mCanvas.drawPath(mOutsideTriangle, mOuterTrianglePaint);

        //画圆弧
        mCompassOuterRingPaint1.setStrokeWidth((float) 8);
        mCompassOuterRingPaint2.setStrokeWidth((float) 8);
        mCompassOuterRingPaint3.setStrokeWidth((float) 8);
        mOuterTrianglePaint.setStrokeWidth((float) 8);
        mDirectionLineMinorPaint.setStrokeWidth((float) 3);
        mDirectionLineMainPaint.setStrokeWidth((float) 8);
        mDirectionLineMinorPaint.setStyle(Paint.Style.STROKE);
//        mCanvas.drawArc(width / 2 - mOutSideRadius, mTextHeight, width / 2 + mOutSideRadius, mTextHeight + mOutSideRadius * 2, -80, 120, false, mCompassOuterRingPaint2);
//        mCanvas.drawArc(width / 2 - mOutSideRadius, mTextHeight, width / 2 + mOutSideRadius, mTextHeight + mOutSideRadius * 2, 40, 20, false, mCompassOuterRingPaint1);
//        mCanvas.drawArc(width / 2 - mOutSideRadius, mTextHeight, width / 2 + mOutSideRadius, mTextHeight + mOutSideRadius * 2, -100, -20, false, mCompassOuterRingPaint2);
//        mCanvas.drawArc(width / 2 - mOutSideRadius, mTextHeight, width / 2 + mOutSideRadius, mTextHeight + mOutSideRadius * 2, -120, -120, false, mCompassOuterRingPaint3);


        mCanvas.drawArc(new RectF(width / 2 - mOutSideRadius, mCurrentDirectionTextHeight, width / 2 + mOutSideRadius, mCurrentDirectionTextHeight + mOutSideRadius * 2), -80, 120, false, mCompassOuterRingPaint2);
        mCanvas.drawArc(new RectF(width / 2 - mOutSideRadius, mCurrentDirectionTextHeight, width / 2 + mOutSideRadius, mCurrentDirectionTextHeight + mOutSideRadius * 2), 40, 20, false, mCompassOuterRingPaint1);
        mCanvas.drawArc(new RectF(width / 2 - mOutSideRadius, mCurrentDirectionTextHeight, width / 2 + mOutSideRadius, mCurrentDirectionTextHeight + mOutSideRadius * 2), -100, -20, false, mCompassOuterRingPaint2);
        mCanvas.drawArc(new RectF(width / 2 - mOutSideRadius, mCurrentDirectionTextHeight, width / 2 + mOutSideRadius, mCurrentDirectionTextHeight + mOutSideRadius * 2), -120, -120, false, mCompassOuterRingPaint3);
        mCanvas.restore();
    }

    private void drawText() {
        if (val <= 15 || val >= 345) {
            text = "北";
        } else if (val > 15 && val <= 75) {
            text = "东北";
        } else if (val > 75 && val <= 105) {
            text = "东";
        } else if (val > 105 && val <= 165) {
            text = "东南";
        } else if (val > 165 && val <= 195) {
            text = "南";
        } else if (val > 195 && val <= 255) {
            text = "西南";
        } else if (val > 255 && val <= 285) {
            text = "西";
        } else if (val > 285 && val < 345) {
            text = "西北";
        }

        mCurrentDirectionTextPaint.getTextBounds(text, 0, text.length(), mTextRect);
        //文字宽度
        int mTextWidth = mTextRect.width();
        //让文字水平居中显示
        mCanvas.drawText(text, width / 2 - mTextWidth / 2, mCurrentDirectionTextHeight / 2, mCurrentDirectionTextPaint);

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        width = Math.min(widthSize, heightSize);
        if (widthMode == MeasureSpec.UNSPECIFIED) {
            width = heightSize;
        } else if (heightMode == MeasureSpec.UNSPECIFIED) {
            width = widthSize;
        }
        //为指南针上面的文字预留空间，定为1/3边长
//        mCurrentDirectionTextHeight = width / 3;
        //设置圆心点坐标
        mCenterX = width / 2;
        mCenterY = width / 2 + mCurrentDirectionTextHeight;
        //外部圆的外径
        mOutSideRadius = width * 3 / 8;
        //外接圆的半径
        mCircumRadius = mOutSideRadius * 4 / 5;
        //camera最大平移距离
        mMaxCameraTranslate = 0.02f * mOutSideRadius;
        setMeasuredDimension(width, width + width / 3);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isTouch) {
            return false;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (mValueAnimator != null && mValueAnimator.isRunning()) {
                    mValueAnimator.cancel();
                }
                //3D 效果让Camera旋转,获取旋转偏移大小
                getCameraRotate(event);
                //获取平移大小
                getCameraTranslate(event);
                break;
            case MotionEvent.ACTION_MOVE:
                //3D 效果让Camera旋转,获取旋转偏移大小
                getCameraRotate(event);
                //获取平移大小
                getCameraTranslate(event);
                break;
            case MotionEvent.ACTION_UP:
                //松开手 复原动画
                startRestore();
                break;
        }
        return true;
    }

    private void startRestore() {
        final String cameraRotateXName = "cameraRotateX";
        final String cameraRotateYName = "cameraRotateY";
        final String canvasTranslateXName = "canvasTranslateX";
        final String canvasTranslateYName = "canvasTranslateY";
        PropertyValuesHolder cameraRotateXHolder =
                PropertyValuesHolder.ofFloat(cameraRotateXName, mCameraRotateX, 0);
        PropertyValuesHolder cameraRotateYHolder =
                PropertyValuesHolder.ofFloat(cameraRotateYName, mCameraRotateY, 0);
        PropertyValuesHolder canvasTranslateXHolder =
                PropertyValuesHolder.ofFloat(canvasTranslateXName, mCameraTranslateX, 0);
        PropertyValuesHolder canvasTranslateYHolder =
                PropertyValuesHolder.ofFloat(canvasTranslateYName, mCameraTranslateY, 0);
        mValueAnimator = ValueAnimator.ofPropertyValuesHolder(cameraRotateXHolder,
                cameraRotateYHolder, canvasTranslateXHolder, canvasTranslateYHolder);
        mValueAnimator.setInterpolator(new TimeInterpolator() {
            @Override
            public float getInterpolation(float input) {
                float f = 0.571429f;
                return (float) (Math.pow(2, -2 * input) * Math.sin((input - f / 4) * (2 * Math.PI) / f) + 1);
            }
        });
        mValueAnimator.setDuration(1000);
        mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mCameraRotateX = (float) animation.getAnimatedValue(cameraRotateXName);
                mCameraRotateY = (float) animation.getAnimatedValue(cameraRotateYName);
                mCameraTranslateX = (float) animation.getAnimatedValue(canvasTranslateXName);
                mCameraTranslateX = (float) animation.getAnimatedValue(canvasTranslateYName);
            }
        });
        mValueAnimator.start();
    }

    /**
     * 获取Camera，平移大小
     *
     * @param event
     */
    private void getCameraTranslate(MotionEvent event) {
        float translateX = (event.getX() - getWidth() / 2);
        float translateY = (event.getY() - getHeight() / 2);
        //求出此时位移的大小与半径之比
        float[] percentArr = getPercent(translateX, translateY);
        //最终位移的大小按比例匀称改变
        mCameraTranslateX = percentArr[0] * mMaxCameraTranslate;
        mCameraTranslateY = percentArr[1] * mMaxCameraTranslate;
    }

    /**
     * 让Camera旋转,获取旋转偏移大小
     *
     * @param event
     */
    private void getCameraRotate(MotionEvent event) {
        float mRotateX = -(event.getY() - (getHeight()) / 2);
        float mRotateY = (event.getX() - getWidth() / 2);
        //求出旋转大小与半径之比
        float[] percentArr = getPercent(mRotateX, mRotateY);
        mCameraRotateX = percentArr[0] * mMaxCameraRotate;
        mCameraRotateY = percentArr[1] * mMaxCameraRotate;
    }

    /**
     * 获取比例
     *
     * @param mCameraRotateX
     * @param mCameraRotateY
     * @return
     */
    private float[] getPercent(float mCameraRotateX, float mCameraRotateY) {
        float[] percentArr = new float[2];
        float percentX = mCameraRotateX / width;
        float percentY = mCameraRotateY / width;
        //处理一下比例值
        if (percentX > 1) {
            percentX = 1;
        } else if (percentX < -1) {
            percentX = -1;
        }
        if (percentY > 1) {
            percentY = 1;
        } else if (percentY < -1) {
            percentY = -1;
        }
        percentArr[0] = percentX;
        percentArr[1] = percentY;
        return percentArr;
    }

    public void toggleVisibility() {
        if (getVisibility() == View.VISIBLE) {
            setVisibility(GONE);
        } else {
            setVisibility(VISIBLE);
        }
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (visibility == VISIBLE) {
            registerSensor();
        } else {
            unregisterSensor();
        }
    }

    private SensorManager mSensorManager;
    private SensorEventListener mSensorEventListener;
    private LifecycleOwner mLifecycleOwner = null;
    private OnLifecycleListener mOnLifecycleListener = null;

    /**
     * 设置声明周期
     *
     * @param fragment
     */
    public void setLifecycleOwner(@Nullable Fragment fragment) {
        setLifecycleOwner(fragment.getViewLifecycleOwner());
    }

    /**
     * 设置声明周期
     *
     * @param lifecycleOwner
     */
    public void setLifecycleOwner(@Nullable LifecycleOwner lifecycleOwner) {
        if (mLifecycleOwner == lifecycleOwner) {
            return;
        }
        if (mLifecycleOwner != null) {
            mLifecycleOwner.getLifecycle().removeObserver(mOnLifecycleListener);
        }
        mLifecycleOwner = lifecycleOwner;
        if (lifecycleOwner != null) {
            if (mOnLifecycleListener == null) {
                mOnLifecycleListener = new OnLifecycleListener();
            }
            lifecycleOwner.getLifecycle().addObserver(mOnLifecycleListener);
            registerSensor();
        }
    }

    /**
     * 注册传感器
     */
    public void registerSensor() {
        if (mSensorManager != null && mSensorEventListener != null) {
            return;
        }
        mSensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
        mSensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                setVal(event.values[0]);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };
        mSensorManager.registerListener(mSensorEventListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_GAME);
    }

    /**
     * 解除注册传感器
     */
    public void unregisterSensor() {
        if (mSensorManager == null || mSensorEventListener == null) {
            return;
        }
        mSensorManager.unregisterListener(mSensorEventListener);
        mSensorManager = null;
        mSensorEventListener = null;
    }


    @Override
    protected void onDetachedFromWindow() {
        unregisterSensor();
        super.onDetachedFromWindow();
    }

    /**
     * 生命周期事件
     */
    class OnLifecycleListener implements LifecycleObserver {
        private OnLifecycleListener() {
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        public void onDestroy() {
            unregisterSensor();
        }
    }
}
