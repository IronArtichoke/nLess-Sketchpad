package ironartichoke.sketchpad;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.text.Html;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wnafee.vector.compat.ResourcesCompat;

import ironartichoke.sketchpad.util.AppearanceUtils;

/**
 * A view that guides the user through the tutorial.
 * <br\><br\>
 * The user presses the arrow button or the specified icon to advance through the steps.
 * The view moves around to accent various features.
 * Pressing the Back button will close the tutorial.
 * The tutorial appears automatically when the user launches the app for the first time.
 * Afterwards, it may be accessed from the main menu.
 */
public class TutorialView extends LinearLayout
{
	/** A reference to the context from which this view was created. */
	private Context ctx;

	/** The current tutorial step. */
	private int step;
	/** The list of view titles corresponding to each step. */
	private String[] stepTitles;
	/** The list of view messages corresponding to each step. */
	private String[] stepMessages;

	/** The <code>TextView</code> displaying the title. */
	private TextView title;
	/** The <code>TextView</code> displaying the message. */
	private TextView message;
	/** The button to go to the next step. */
	private ImageView arrowNext;
	/** The button to go back to the previous step. */
	private ImageView arrowPrevious;

	/** The <code>NotepadActivity</code> that the view belongs to. */
	private NotepadActivity activity;
	/** A flag indicating whether the view has been measured.
	 * @see #onMeasure(int, int) */
	private boolean isMeasured = false;
	/** A flag indicating whether the user has requested to go to the next or the previous step. */
	private boolean isStepUpdateRequested = false;

	/** The constant for positioning the view above another view. */
	private static final int ALIGN_ABOVE = 0;
	/** The constant for positioning the view below another view. */
	private static final int ALIGN_BELOW = ~ALIGN_ABOVE;
	/** The constant for positioning the view to the left of another view. */
	private static final int ALIGN_TO_LEFT = 1;
	/** The constant for positioning the view to the right of another view. */
	private static final int ALIGN_TO_RIGHT = ~ALIGN_TO_LEFT;
	/** The constant for aligning the top edge of the view with the top edge of another view. */
	private static final int ALIGN_EDGE_TOP = 2;
	/** The constant for aligning the bottom edge of the view with the bottom edge of another view. */
	private static final int ALIGN_EDGE_BOTTOM = ~ALIGN_EDGE_TOP;
	/** The constant for aligning the left edge of the view with the left edge of another view. */
	private static final int ALIGN_EDGE_LEFT = 3;
	/** The constant for aligning the right edge of the view with the right edge of another view. */
	private static final int ALIGN_EDGE_RIGHT = ~ALIGN_EDGE_LEFT;

	/** The margin in pixels between the view and the other view specified in {@link #align(int, int, int, int)}. */
	private static final int MARGIN = 8;

	/**
	 * @see View#View(Context)
	 */
	public TutorialView(Context context)
	{
		super(context);
		ctx = context;
	}

	/**
	 * @see View#View(Context, AttributeSet)
	 */
	public TutorialView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		ctx = context;
	}

	/**
	 * @see View#View(Context, AttributeSet, int)
	 */
	public TutorialView(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
		ctx = context;
	}

	/**
	 * Sets the reference to the <code>NotepadActivity</code> that this view belongs to.
	 * @param activity The <code>NotepadActivity</code>.
	 */
	public void setActivityReference(NotepadActivity activity)
	{
		this.activity = activity;
	}

	/**
	 * Starts the tutorial by resetting to the first step and making the view visible.
	 */
	public void startTutorial()
	{
		setStep(0);
		setVisibility(VISIBLE);
		setAlpha(1f);

		post(new Runnable()
		{
			@Override
			public void run()
			{
				goToStep(0);
			}
		});
	}

	@Override
	protected void onAttachedToWindow()
	{
		super.onAttachedToWindow();

		Resources res = getContext().getResources();
		stepTitles = res.getStringArray(R.array.tutorial_titles);
		stepMessages = res.getStringArray(R.array.tutorial_messages);
		stepMessages[0] = String.format(stepMessages[0], getContext().getString(R.string.app_name));

		title = (TextView) findViewById(R.id.textview_tutorial_title);
		message = (TextView) findViewById(R.id.textview_tutorial_message);

		arrowNext = (ImageView) findViewById(R.id.imageview_tutorial_next);
		arrowNext.setImageDrawable(ResourcesCompat.getDrawable(ctx, R.drawable.arrow_right));
		arrowNext.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				nextStep();
			}
		});

		arrowPrevious = (ImageView) findViewById(R.id.imageview_tutorial_previous);
		arrowPrevious.setImageDrawable(ResourcesCompat.getDrawable(ctx, R.drawable.arrow_left));
		arrowPrevious.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				previousStep();
			}
		});

		updateAppearance(res);
	}

	/**
	 * Updates the view's appearance to reflect the current theme.
	 * @param res A <code>Resources</code> instance.
	 */
	@SuppressWarnings("deprecated")
	public void updateAppearance(Resources res)
	{
		setBackgroundDrawable(res.getDrawable(AppearanceUtils.isThemeDark() ? R.drawable.window_rounded_dark : R.drawable.window_rounded));
		message.setTextColor(res.getColor(AppearanceUtils.isThemeDark() ? R.color.text_white : R.color.text_black));
	}

	/**
	 * Gets the current tutorial step.
	 * @return The current step.
	 */
	public int getStep()
	{
		return step;
	}

	/**
	 * Sets the current step value.
	 * This should be called from {@link #goToStep(int)} or else nothing visible will happen.
	 * @param step The new step value.
	 */
	private void setStep(int step)
	{
		this.step = step;
	}

	/**
	 * Moves to the specified step of the tutorial.
	 * @param step The step to move to.
	 */
	private void goToStep(int step)
	{
		setStep(step);
		title.setText(stepTitles[step]);
		message.setText(Html.fromHtml(stepMessages[step]));

		isStepUpdateRequested = true;
		invalidate();
	}

	/**
	 * Moves to the next step of the tutorial.
	 */
	public void nextStep()
	{
		if (step < stepTitles.length - 1)
		{
			goToStep(step + 1);
		}
		else
		{
			closeTutorial();
		}
	}

	/**
	 * Moves to the previous step of the tutorial.
	 */
	private void previousStep()
	{
		if (step > 0) goToStep(step - 1);
	}

	/**
	 * Gets the value of the {@link #isMeasured} flag.
	 * @return {@link #isMeasured}.
	 */
	private boolean isMeasured()
	{
		return isMeasured;
	}

	/**
	 * Sets the {@link #isMeasured} flag.
	 * @param isMeasured The new value of {@link #isMeasured}.
	 */
	private void setMeasured(boolean isMeasured)
	{
		this.isMeasured = isMeasured;
	}

	/**
	 * Closes the tutorial and hides the view.
	 */
	public void closeTutorial()
	{
		ObjectAnimator fadeOut = ObjectAnimator.ofFloat(this, "alpha", 0f);
		fadeOut.setInterpolator(new DecelerateInterpolator());
		fadeOut.addListener(new AnimatorListenerAdapter()
		{
			@Override
			public void onAnimationEnd(Animator animation)
			{
				setVisibility(GONE);
				activity.closeTutorial();
			}
		});
		fadeOut.start();
		Prefs.edit().putBoolean(R.string.pref_tutorial_done, true).apply();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		setMeasured(true);
		if (isStepUpdateRequested)
		{
			isStepUpdateRequested = false;
			post(new Runnable()
			{
				@Override
				public void run()
				{
					update();
				}
			});
		}
	}

	/**
	 * Updates the contents and the position of the view to reflect the current tutorial step.
	 * Requires {@link #isMeasured()} to be true.
	 */
	public void update()
	{
		if (!isMeasured()) return;

		arrowPrevious.setVisibility(VISIBLE);
		arrowNext.setVisibility(VISIBLE);

		switch (step)
		{
			case 0: // Start of tutorial
			{
				center();
				arrowPrevious.setVisibility(GONE);
				break;
			}
			case 1: // Drawing
			{
				activity.closeStrokeSettings();
				align(ALIGN_TO_LEFT, R.id.include_toolbar_draw, ALIGN_EDGE_TOP, R.id.include_toolbar_draw);
				break;
			}
			case 2: // Stroke properties
			{
				this.bringToFront();
				align(ALIGN_EDGE_LEFT, R.id.view, ALIGN_EDGE_TOP, R.id.view);
				break;
			}
			case 3: // Camera
			{
				activity.closeStrokeSettings();
				align(ALIGN_EDGE_LEFT, R.id.view, ALIGN_EDGE_TOP, R.id.view);
				break;
			}
			case 4: // How large is the canvas?
			{
				align(ALIGN_EDGE_LEFT, R.id.view, ALIGN_EDGE_TOP, R.id.view);
				break;
			}
			case 5: // What if I get lost?
			{
				align(ALIGN_EDGE_LEFT, R.id.include_camera, ALIGN_ABOVE, R.id.include_camera);
				break;
			}
			case 6: // The sheet menu
			{
				activity.closeSheetMenu();
				arrowNext.setVisibility(GONE);
				align(ALIGN_TO_LEFT, R.id.include_toolbar_main, ALIGN_EDGE_BOTTOM, R.id.include_toolbar_main);
				break;
			}
			case 7: // The sheet menu, continued
			{
				align(ALIGN_EDGE_RIGHT, R.id.include_toolbar_main, ALIGN_EDGE_BOTTOM, R.id.include_toolbar_main);
				this.bringToFront();
				break;
			}
			case 8: // The main menu
			{
				activity.closeSheetMenu();
				activity.closeMainMenu();
				arrowNext.setVisibility(GONE);
				align(ALIGN_TO_LEFT, R.id.include_toolbar_main, ALIGN_EDGE_BOTTOM, R.id.include_toolbar_main);
				break;
			}
			case 9: // The main menu, continued
			{
				align(ALIGN_TO_LEFT, R.id.include_menu, ALIGN_EDGE_BOTTOM, R.id.include_menu);
				break;
			}
			case 10: // End of tutorial
			{
				activity.closeMainMenu();
				center();
				break;
			}
		}
	}

	/**
	 * Positions the view in the center of the screen.
	 */
	private void center()
	{
		int rootWidth = ((View) getParent()).getWidth();
		int rootHeight = ((View) getParent()).getHeight();

		int x = rootWidth / 2 - getWidth() / 2;
		int y = rootHeight / 2 - getHeight() / 2;

		moveTo(x, y);
	}

	/**
	 * Aligns the view relative to up to two views.
	 * @param horizontal The <code>ALIGN</code> constant on the horizontal axis.
	 * @param anchorViewIdH The ID of the view to align with on the horizontal axis.
	 * @param vertical The <code>ALIGN</code> constant on the vertical axis.
	 * @param anchorViewIdV The ID of the view to align with on the vertical axis.
	 * @see #ALIGN_ABOVE
	 * @see #ALIGN_BELOW
	 * @see #ALIGN_TO_LEFT
	 * @see #ALIGN_TO_RIGHT
	 * @see #ALIGN_EDGE_TOP
	 * @see #ALIGN_EDGE_BOTTOM
	 * @see #ALIGN_EDGE_LEFT
	 * @see #ALIGN_EDGE_RIGHT
	 */
	private void align(int horizontal, int anchorViewIdH, int vertical, int anchorViewIdV)
	{
		if (AppearanceUtils.isLeftHanded())
		{
			horizontal = ~horizontal;
		}

		View anchorViewH = ((View) getParent()).findViewById(anchorViewIdH);
		View anchorViewV = ((View) getParent()).findViewById(anchorViewIdV);

		if (anchorViewH == null) throw new IllegalArgumentException("Horizontal anchor view not found (ID=" + anchorViewIdH + ").");
		if (anchorViewV == null) throw new IllegalArgumentException("Vertical anchor view not found (ID=" + anchorViewIdV + ").");

		int x, y;

		switch (horizontal)
		{
			case ALIGN_TO_LEFT:
			{
				x = anchorViewH.getLeft() - getWidth() - MARGIN; break;
			}
			case ALIGN_TO_RIGHT:
			{
				x = anchorViewH.getRight() + MARGIN; break;
			}
			case ALIGN_EDGE_LEFT:
			{
				x = anchorViewH.getLeft() + MARGIN; break;
			}
			case ALIGN_EDGE_RIGHT:
			{
				x = anchorViewH.getRight() - getWidth() - MARGIN; break;
			}
			default:
			{
				throw new IllegalArgumentException("Horizontal align value invalid; received " + horizontal + ".");
			}
		}

		switch (vertical)
		{
			case ALIGN_ABOVE:
			{
				y = anchorViewV.getTop() - getHeight() - MARGIN; break;
			}
			case ALIGN_BELOW:
			{
				y = anchorViewV.getBottom() + MARGIN; break;
			}
			case ALIGN_EDGE_TOP:
			{
				y = anchorViewV.getTop() + MARGIN; break;
			}
			case ALIGN_EDGE_BOTTOM:
			{
				y = anchorViewV.getBottom() - getHeight() - MARGIN; break;
			}
			default:
			{
				throw new IllegalArgumentException("Vertical align value invalid; received " + horizontal + ".");
			}
		}

		moveTo(x, y);
	}

	/**
	 * Moves the view to the given position.
	 * @param x The X coordinate.
	 * @param y The Y coordinate.
	 */
	private void moveTo(int x, int y)
	{
		ObjectAnimator animatorX = ObjectAnimator.ofFloat(this, "translationX", x);
		ObjectAnimator animatorY = ObjectAnimator.ofFloat(this, "translationY", y);

		AnimatorSet animatorSet = new AnimatorSet();
		animatorSet.play(animatorX).with(animatorY);
		animatorSet.setInterpolator(new DecelerateInterpolator());

		animatorSet.start();
	}
}
