package net.osmand.plus.base.bottomsheetmenu;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.LayoutRes;
import androidx.core.content.ContextCompat;

import net.osmand.AndroidUtils;
import net.osmand.plus.R;

public class SimpleBottomSheetItem extends BaseBottomSheetItem {

	private Drawable background;
	private Drawable icon;
	private boolean iconHidden;
	private boolean showDivider;
	protected CharSequence title;
	@ColorRes
	protected int titleColorId = INVALID_ID;

	private TextView titleTv;
	private ImageView iconView;
	private View divider;

	public SimpleBottomSheetItem(View customView,
								 @LayoutRes int layoutId,
								 Object tag,
								 boolean disabled,
								 View.OnClickListener onClickListener,
								 int position,
								 Drawable icon,
								 Drawable background,
								 CharSequence title,
								 @ColorRes int titleColorId,
								 boolean iconHidden,
								 boolean showDivider) {
		super(customView, layoutId, tag, disabled, onClickListener, position);
		this.icon = icon;
		this.background = background;
		this.title = title;
		this.titleColorId = titleColorId;
		this.iconHidden = iconHidden;
		this.showDivider = showDivider;
	}

	protected SimpleBottomSheetItem() {

	}

	public void setTitle(String title) {
		this.title = title;
		titleTv.setText(title);
	}

	public void setIcon(Drawable icon) {
		this.icon = icon;
		iconView.setImageDrawable(icon);
	}

	public void setTitleColorId(@ColorRes int titleColorId) {
		this.titleColorId = titleColorId;
		titleTv.setTextColor(ContextCompat.getColor(titleTv.getContext(), titleColorId));
	}

	@Override
	public void inflate(Context context, ViewGroup container, boolean nightMode) {
		super.inflate(context, container, nightMode);
		iconView = ((ImageView) view.findViewById(R.id.icon));
		if (iconView != null) {
			iconView.setImageDrawable(icon);
			iconView.setVisibility(iconHidden ? View.GONE : View.VISIBLE);
		}
		titleTv = (TextView) view.findViewById(R.id.title);
		if (title != null && titleTv != null) {
			titleTv.setText(title);
			if (titleColorId != INVALID_ID) {
				titleTv.setTextColor(ContextCompat.getColor(context, titleColorId));
			}
		}
		if (background != null) {
			AndroidUtils.setBackground(view, background);
		}
		divider = view.findViewById(R.id.divider);
		if (divider != null) {
			divider.setVisibility(showDivider ? View.VISIBLE : View.GONE);
		}
	}

	public static class Builder extends BaseBottomSheetItem.Builder {

		protected Drawable icon;
		protected Drawable background;
		protected CharSequence title;
		@ColorRes
		protected int titleColorId = INVALID_ID;
		protected boolean iconHidden;
		protected boolean showDivider;

		public Builder setIcon(Drawable icon) {
			this.icon = icon;
			return this;
		}

		public Builder setBackground(Drawable icon) {
			this.background = icon;
			return this;
		}

		public Builder setTitle(CharSequence title) {
			this.title = title;
			return this;
		}

		public Builder setTitleColorId(@ColorRes int titleColorId) {
			this.titleColorId = titleColorId;
			return this;
		}

		public Builder setIconHidden(boolean iconHidden) {
			this.iconHidden = iconHidden;
			return this;
		}

		public Builder setShowDivider(boolean showDivider) {
			this.showDivider = showDivider;
			return this;
		}

		public SimpleBottomSheetItem create() {
			return new SimpleBottomSheetItem(customView,
					layoutId,
					tag,
					disabled,
					onClickListener,
					position,
					icon,
					background,
					title,
					titleColorId,
					iconHidden,
					showDivider);
		}
	}
}
