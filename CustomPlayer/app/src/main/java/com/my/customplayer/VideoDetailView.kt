package com.my.customplayer

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import android.widget.LinearLayout
import kotlin.math.abs

/**
 * VideoDetailView 中包含VideoView（播放器）和DetailView（视频相关的详细信息）
 */
class VideoDetailView : LinearLayout, View.OnTouchListener {
    // 底部导航栏
    private lateinit var mBottomNav: LinearLayout
    private lateinit var mActivity: Activity
    private lateinit var mVideoDetailViewWrapper: VideoDetailViewWrapper
    private lateinit var mVideoView: View // 播放器
    private lateinit var mDetailView: View // 播放器下的详细信息
    private lateinit var mControlBarDivider: View // 播放页面收起停留在底部时，需要一条分割线
    private var touchSlop: Int = 0 // 移动的有效距离
    private var mDownY = 0; // 记录按下时的Y坐标
    private var mLastY = 0 // 记录滑动过程中上一次的Y坐标
    private var dy = 0 // 上一次滑动的差值
    private var mScreenHeightWithOutStatusBar = 0 // 屏幕高度
    private lateinit var tracker: VelocityTracker // 记录手指滑动的速度

    // 最大尺寸与最小尺寸
    private val mVideoViewMaxWidth = screenWidth;
    private val mVideoViewMaxHeight = dp2px(200f);
    private val mVideoViewMinWidth = dp2px(100f);
    private val mVideoViewMinHeight = dp2px(50f);
    private val mBottomNavHeight = dp2px(50f)

    // VideoDetailView可以滑动的范围
    private var mScrollRange = 0
    var state: State = State.EXIT

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    override fun onFinishInflate() {
        super.onFinishInflate()
        init()
    }

    // 初始化
    private fun init() {
        mActivity = context as Activity
        mVideoView = findViewById<View>(R.id.video_view)
        mDetailView = findViewById<View>(R.id.detail_view)
        mControlBarDivider = findViewById<View>(R.id.control_bar_divider)
        mVideoDetailViewWrapper = VideoDetailViewWrapper()
        setOnTouchListener(this)
        touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        // 屏幕高度（不包含状态栏高度）
        mScreenHeightWithOutStatusBar =
            getScreenHeight(context) - getStatusBarHeight(mActivity.window)

        mScrollRange = mScreenHeightWithOutStatusBar - mVideoViewMinHeight - mBottomNavHeight
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        var dDownY = 0
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mDownY = event.rawY.toInt()
                tracker = VelocityTracker.obtain()
            }
            MotionEvent.ACTION_MOVE -> {
                val y = event.rawY.toInt()
                tracker.addMovement(event)
                dDownY = y - mDownY // 距离按下处的差值
                dy = event.rawY.toInt() - mLastY // 上一次滑动的差值
                if (abs(dDownY) < touchSlop) return true // 如果滑动距离小于最小有效距离，返回
                move(dy)
                mLastY = y
            }
            MotionEvent.ACTION_UP -> {
                if (state == State.COLLAPSED) expand() // 点击事件处理，收起之后如果点击，直接展开

                val y = event.rawY.toInt()
                dDownY = y - mDownY
                if (abs(dDownY) < touchSlop) return true // 有可能出现，用户点击按下，但并没有滑动的情况
                // 释放之后，应该自动回弹，或者抛掷
                tracker.computeCurrentVelocity(100) // units 每秒处理多少个像素
                val yVelocity = abs(tracker.yVelocity) // 获取Y轴的速度
                tracker.clear()
                tracker.recycle()
                // 标记状态
                confirmState(dy < 0, yVelocity) // 判断滑动方向
            }
        }
        return true
    }


    // 向上滑动
    private fun move(dy: Int) {
        if (dy < 0 && this.y == 0f) return // 如果已经到顶部，不能继续向上滑动
        if (dy > 0 && this.y == mScrollRange.toFloat()) return // 如果已经到底部，不能继续向下滑动
        this.state = State.OPENING
        // 移动，先改变translationY值，因为后续会用到这个值
        // 先对滑动之后的Y坐标进行判断，如果会滑出边界，修正回来
        when {
            this.translationY + dy < 0 -> {
                this.translationY = 0f
            }
            this.translationY + dy > mScrollRange -> {
                this.translationY = mScrollRange.toFloat()
            }
            else -> {
                this.translationY += dy // 安全区间
            }
        }
        val videoViewParams = mVideoView.layoutParams // 播放器布局参数
        val videoDetailViewParams = this.layoutParams // 外层VideoDetailView布局参数
        // 通过外层移动距离的百分比，来控制其它控件
        // 与方向无关，这样就能够让向上移动和向下移动，共用同一套代码逻辑
        val ratio = 1 - (this.translationY / mScrollRange)// 外层Y坐标即为移动的距离
        mBottomNav.translationY = mBottomNavHeight * ratio // 底部导航栏移动
        // 播放器宽度以4倍速度增长
        val w = mVideoViewMinWidth + (mVideoViewMaxWidth * ratio * 4).toInt()
        if (w < mVideoViewMaxWidth) {
            videoViewParams.width = w
        } else {
            videoViewParams.width = mVideoViewMaxWidth // 不超过指定值
        }
        // 播放器宽度以3倍速度增长
        val h = mVideoViewMinHeight + (mVideoViewMaxHeight * ratio * 3).toInt()
        if (h < mVideoViewMaxHeight) {
            videoViewParams.height = h
        } else {
            videoViewParams.height = mVideoViewMaxHeight // 不超过指定值
        }
        // 外层高度=屏幕高度-当前Y坐标-底部导航栏所占空间
        videoDetailViewParams.height =
            (mScreenHeightWithOutStatusBar - this.translationY - (mBottomNavHeight - mBottomNav.translationY)).toInt()
        this.layoutParams = videoDetailViewParams
        mVideoView.layoutParams = videoViewParams

        mDetailView.alpha = ratio // 视频详情透明度跟随滑动变化
        mControlBarDivider.alpha = 1 - ratio * 4 // 分割线以非常快的速度不可见
    }

    // 根据Y坐标来标记状态
    private fun confirmState(up: Boolean, yVelocity: Float) {
        if (up) {
            // 往上滑，滑动速度达标，或者滑动距离超过20%，就继续展开
            if (yVelocity > 100 || this.y < (1 - 0.20) * mScrollRange) completeExpand()
            else completeCollapse()
        } else {
            // 往下滑，超过20%，就继续关闭
            if (yVelocity > 100 || this.y > 0.2 * mScrollRange) completeCollapse()
            else completeExpand()
        }

    }

    // 继续完成展开
    private fun completeExpand() {
        state = State.OPENED
        val videoViewParams = mVideoView.layoutParams // 播放器布局参数
        val videoDetailViewParams = this.layoutParams // 外层VideoDetailView布局参数

        AnimatorSet().apply {
            playTogether(
                // 底部导航栏向下移动退出
                ObjectAnimator.ofFloat(
                    mBottomNav,
                    "translationY",
                    mBottomNav.translationY,
                    mBottomNavHeight.toFloat()
                ),
                // 播放页面向上移动进入
                ObjectAnimator.ofFloat(
                    this@VideoDetailView,
                    "translationY",
                    this@VideoDetailView.translationY,
                    0f
                ),
                // 播放页面高度变大
                ObjectAnimator.ofInt(
                    mVideoDetailViewWrapper,
                    "VideoDetailViewHeight",
                    videoDetailViewParams.height,
                    mScreenHeightWithOutStatusBar
                ),
                // 播放器高度变大
                ObjectAnimator.ofInt(
                    mVideoDetailViewWrapper,
                    "VideoViewHeight",
                    videoViewParams.height,
                    mVideoViewMaxHeight
                ),
                // 播放器宽度变大
                ObjectAnimator.ofInt(
                    mVideoDetailViewWrapper,
                    "VideoViewWidth",
                    videoViewParams.width,
                    mVideoViewMaxWidth
                ),
                // 详细内容透明度，逐渐可见
                ObjectAnimator.ofFloat(
                    mDetailView,
                    "alpha",
                    mDetailView.alpha,
                    1f
                )
            )
            duration = 200
            start()
        }
    }

    // 展开，从底部收起状态弹出展开
    private fun expand() {
        state = State.OPENED
        mControlBarDivider.alpha = 0f //分割线不可见
        AnimatorSet().apply {
            playTogether(
                // 底部导航栏向下移动退出
                ObjectAnimator.ofFloat(
                    mBottomNav,
                    "translationY",
                    0f,
                    mBottomNavHeight.toFloat()
                ),
                // 播放页面向上移动进入
                ObjectAnimator.ofFloat(
                    this@VideoDetailView,
                    "translationY",
                    this@VideoDetailView.translationY,
                    0f
                ),
                // 播放页面高度变大
                ObjectAnimator.ofInt(
                    mVideoDetailViewWrapper,
                    "VideoDetailViewHeight",
                    mVideoViewMinHeight,
                    mScreenHeightWithOutStatusBar
                ),
                // 播放器高度变大
                ObjectAnimator.ofInt(
                    mVideoDetailViewWrapper,
                    "VideoViewHeight",
                    mVideoViewMinHeight,
                    mVideoViewMaxHeight
                ),
                // 播放器宽度变大
                ObjectAnimator.ofInt(
                    mVideoDetailViewWrapper,
                    "VideoViewWidth",
                    mVideoViewMinWidth,
                    mVideoViewMaxWidth
                ),
                // 详细内容透明度，逐渐可见
                ObjectAnimator.ofFloat(
                    mDetailView,
                    "alpha",
                    0f,
                    1f
                )
            )
            duration = 500
            start()
        }
    }

    // 打开
    fun open() {
        mBottomNav = mActivity.findViewById<LinearLayout>(R.id.bottom_nav)
        if (state == State.COLLAPSED) { // 如果当前为收起状态，直接展开
            expand()
            return
        }
        state = State.OPENED
        mControlBarDivider.alpha = 0f // 分割线不可见
        AnimatorSet().apply {
            this@VideoDetailView.visibility = View.VISIBLE
            playTogether(
                // 底部导航栏向下移动退出
                ObjectAnimator.ofFloat(
                    mBottomNav,
                    "translationY",
                    0f,
                    mBottomNavHeight.toFloat()
                ),
                // 播放页面向上移动进入
                ObjectAnimator.ofFloat(
                    this@VideoDetailView,
                    "translationY",
                    mScreenHeightWithOutStatusBar.toFloat(),
                    0f
                ),
                // 详细内容透明度，逐渐可见
                ObjectAnimator.ofFloat(
                    mDetailView,
                    "alpha",
                    0f,
                    1f
                )
            )
            duration = 500
            start()
        }
    }

    // 折叠
    private fun completeCollapse() {
        state = State.COLLAPSED
        val videoViewParams = mVideoView.layoutParams // 播放器布局参数
        val videoDetailViewParams = this.layoutParams // 外层VideoDetailView布局参数
        AnimatorSet().apply {
            playTogether(
                // 底部导航栏向上移动进入
                ObjectAnimator.ofFloat(
                    mBottomNav,
                    "translationY",
                    mBottomNav.translationY,
                    0f
                ),
                // 播放页面向下移动退出
                ObjectAnimator.ofFloat(
                    this@VideoDetailView,
                    "translationY",
                    this@VideoDetailView.translationY,
                    mScrollRange.toFloat()
                ),
                // 播放页面高度变小
                ObjectAnimator.ofInt(
                    mVideoDetailViewWrapper,
                    "VideoDetailViewHeight",
                    videoDetailViewParams.height,
                    mVideoViewMinHeight
                ),
                // 播放器高度变小
                ObjectAnimator.ofInt(
                    mVideoDetailViewWrapper,
                    "VideoViewHeight",
                    videoViewParams.height,
                    mVideoViewMinHeight
                ),
                // 播放器宽度变小
                ObjectAnimator.ofInt(
                    mVideoDetailViewWrapper,
                    "VideoViewWidth",
                    videoViewParams.width,
                    mVideoViewMinWidth
                ),
                // 详细内容透明度，逐渐不可见
                ObjectAnimator.ofFloat(
                    mDetailView,
                    "alpha",
                    mDetailView.alpha,
                    0f
                )
            )
            duration = 200
            start()
        }.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                mControlBarDivider.alpha = 1f // 分割线可见
            }
        })
    }

    // 折叠
    fun collapse() {
        if (state != State.OPENED) return
        state = State.COLLAPSED
        AnimatorSet().apply {
            playTogether(
                // 底部导航栏向上移动进入
                ObjectAnimator.ofFloat(
                    mBottomNav,
                    "translationY",
                    mBottomNavHeight.toFloat(),
                    0f
                ),
                // 播放页面向下移动退出
                ObjectAnimator.ofFloat(
                    this@VideoDetailView,
                    "translationY",
                    0f,
                    mScrollRange.toFloat()
                ),
                // 播放页面高度变小
                ObjectAnimator.ofInt(
                    mVideoDetailViewWrapper,
                    "VideoDetailViewHeight",
                    getScreenHeight(mActivity),
                    mVideoViewMinHeight
                ),
                // 播放器高度变小
                ObjectAnimator.ofInt(
                    mVideoDetailViewWrapper,
                    "VideoViewHeight",
                    mVideoViewMaxHeight,
                    mVideoViewMinHeight
                ),
                // 播放器宽度变小
                ObjectAnimator.ofInt(
                    mVideoDetailViewWrapper,
                    "VideoViewWidth",
                    mVideoViewMaxWidth,
                    mVideoViewMinWidth
                ),
                // 详细内容透明度，逐渐不可见
                ObjectAnimator.ofFloat(
                    mDetailView,
                    "alpha",
                    1f,
                    0f
                ),
                // 分割线逐渐可见
                ObjectAnimator.ofFloat(
                    mControlBarDivider,
                    "alpha",
                    0f,
                    1f
                )
            )
            duration = 500
            start()
        }.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                mControlBarDivider.alpha = 1f // 分割线可见
            }
        })


    }

    // 此类为VideoDetailView 的一个包装类，用来改变VideoDetailView的尺寸等
    inner class VideoDetailViewWrapper() {
        // 外层页面的高度设置
        fun setVideoDetailViewHeight(height: Int) {
            this@VideoDetailView.layoutParams.height = height
        }

        fun getVideoDetailViewHeight() = this@VideoDetailView.layoutParams.height

        // 播放器高度的设置
        fun setVideoViewHeight(height: Int) {
            // 改变mVideoView大小，要先获取LayoutParams再设置，否则无法生效
            val params = mVideoView.layoutParams
            params.height = height
            mVideoView.layoutParams = params
        }

        fun getVideoViewHeight() = mVideoView.layoutParams.height

        // 播放器宽度的设置
        fun setVideoViewWidth(width: Int) {
            // 改变mVideoView大小，要先获取LayoutParams再设置，否则无法生效
            val params = mVideoView.layoutParams
            if (width == screenWidth) {
                // 填充父容器
                params.width = FrameLayout.LayoutParams.MATCH_PARENT
            } else {
                params.width = width
            }
            mVideoView.layoutParams = params
        }

        fun getVideoViewWidth(): Int {
            val params = mVideoView.layoutParams
            return if (params.width < 0) {
                screenWidth
            } else {
                params.width
            }
        }
    }

    enum class State {
        EXIT, // 退出
        OPENED, // 已打开
        OPENING, // 正在打开中
        COLLAPSED // 已折叠
    }
}