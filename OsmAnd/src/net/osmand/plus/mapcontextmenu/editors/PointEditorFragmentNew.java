package net.osmand.plus.mapcontextmenu.editors;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputLayout;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.ColorDialogs;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.measurementtool.ExitBottomSheetDialogFragment;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.routepreparationmenu.cards.BaseCard.CardListener;
import net.osmand.plus.track.cards.ColorsCard;
import net.osmand.plus.track.fragments.CustomColorBottomSheet.ColorPickerListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static net.osmand.GPXUtilities.DEFAULT_ICON_NAME;
import static net.osmand.data.FavouritePoint.BackgroundType;
import static net.osmand.data.FavouritePoint.DEFAULT_BACKGROUND_TYPE;
import static net.osmand.data.FavouritePoint.DEFAULT_UI_ICON_ID;
import static net.osmand.plus.myplaces.FavouritesDbHelper.FavoriteGroup.PERSONAL_CATEGORY;
import static net.osmand.plus.myplaces.FavouritesDbHelper.FavoriteGroup.isPersonalCategoryDisplayName;

public abstract class PointEditorFragmentNew extends BaseOsmAndFragment implements ColorPickerListener, CardListener {

	public static final String TAG = PointEditorFragmentNew.class.getSimpleName();

	private View view;
	private EditText nameEdit;
	private TextView addDelDescription;
	private TextView addAddressBtn;
	private TextView addToHiddenGroupInfo;
	private ImageView deleteAddressIcon;
	private boolean cancelled;
	private boolean nightMode;
	@ColorInt
	private int selectedColor;
	private BackgroundType selectedShape = DEFAULT_BACKGROUND_TYPE;
	private ImageView nameIcon;
	private GroupAdapter groupListAdapter;
	private int scrollViewY;
	private RecyclerView groupRecyclerView;
	private OsmandApplication app;
	private View descriptionCaption;
	private View addressCaption;
	private EditText descriptionEdit;
	private EditText addressEdit;
	private int layoutHeightPrevious = 0;

	private IconsCard iconsCard;
	private ColorsCard colorsCard;
	private ShapesCard shapesCard;

	protected boolean skipConfirmationDialog;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();

		requireMyActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			public void handleOnBackPressed() {
				MapActivity mapActivity = getMapActivity();
				if (mapActivity != null) {
					showExitDialog();
				}
			}
		});
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {

		Context context = requireContext();
		nightMode = app.getDaynightHelper().isNightModeForMapControls();
		view = UiUtilities.getInflater(getContext(), nightMode)
				.inflate(R.layout.point_editor_fragment_new, container, false);
		AndroidUtils.addStatusBarPadding21v(context, view);

		final PointEditor editor = getEditor();
		if (editor == null) {
			return view;
		}

		editor.updateLandscapePortrait(requireActivity());
		editor.updateNightMode();

		selectedColor = getPointColor();
		selectedShape = getBackgroundType();

		Toolbar toolbar = view.findViewById(R.id.toolbar);
		toolbar.setBackgroundColor(ContextCompat.getColor(requireContext(),
				nightMode ? R.color.app_bar_color_dark : R.color.list_background_color_light));
		toolbar.setTitle(getToolbarTitle());
		Drawable icBack = app.getUIUtilities().getIcon(AndroidUtils.getNavigationIconResId(app),
				nightMode ? R.color.active_buttons_and_links_text_dark : R.color.description_font_and_bottom_sheet_icons);
		toolbar.setNavigationIcon(icBack);
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(v -> showExitDialog());

		final ScrollView scrollView = view.findViewById(R.id.editor_scroll_view);
		scrollViewY = scrollView.getScrollY();
		scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
			if (scrollViewY != scrollView.getScrollY()) {
				scrollViewY = scrollView.getScrollY();
				hideKeyboard();
				descriptionEdit.clearFocus();
				nameEdit.clearFocus();
				addressEdit.clearFocus();
			}
		});

		int activeColor = ColorUtilities.getActiveColor(context, nightMode);
		ImageView toolbarAction = view.findViewById(R.id.toolbar_action);
		view.findViewById(R.id.background_layout).setBackgroundResource(nightMode
				? R.color.app_bar_color_dark : R.color.list_background_color_light);
		ImageView replaceIcon = view.findViewById(R.id.replace_action_icon);
		replaceIcon.setImageDrawable(getPaintedIcon(R.drawable.ic_action_replace, activeColor));
		ImageView deleteIcon = view.findViewById(R.id.delete_action_icon);
		deleteIcon.setImageDrawable(getPaintedIcon(R.drawable.ic_action_delete_dark, activeColor));
		ImageView groupListIcon = view.findViewById(R.id.group_list_button_icon);
		groupListIcon.setImageDrawable(getPaintedIcon(R.drawable.ic_action_group_select_all, activeColor));
		addToHiddenGroupInfo = view.findViewById(R.id.add_hidden_group_info);
		addToHiddenGroupInfo.setText(getString(R.string.add_hidden_group_info, getString(R.string.shared_string_my_places)));
		View groupList = view.findViewById(R.id.group_list_button);
		groupList.setOnClickListener(v -> {
			FragmentManager fragmentManager = getFragmentManager();
			DialogFragment dialogFragment = createSelectCategoryDialog();
			if (fragmentManager != null && dialogFragment != null) {
				dialogFragment.show(fragmentManager, SelectFavoriteCategoryBottomSheet.class.getSimpleName());
			}
		});
		view.findViewById(R.id.buttons_divider).setVisibility(View.VISIBLE);
		final View saveButton = view.findViewById(R.id.right_bottom_button);
		saveButton.setVisibility(View.VISIBLE);
		saveButton.setOnClickListener(v -> savePressed());

		View cancelButton = view.findViewById(R.id.dismiss_button);
		cancelButton.setOnClickListener(v -> showExitDialog());

		UiUtilities.setupDialogButton(nightMode, cancelButton, UiUtilities.DialogButtonType.SECONDARY, R.string.shared_string_cancel);
		UiUtilities.setupDialogButton(nightMode, saveButton, UiUtilities.DialogButtonType.PRIMARY, R.string.shared_string_save);

		final TextInputLayout nameCaption = view.findViewById(R.id.name_caption);
		nameCaption.setHint(getString(R.string.shared_string_name));

		nameEdit = view.findViewById(R.id.name_edit);
		nameEdit.setText(getNameInitValue());
		boolean emptyNameAllowed = editor.isProcessingTemplate();
		if (!emptyNameAllowed) {
			nameEdit.addTextChangedListener(new TextWatcher() {
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				}

				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
				}

				@Override
				public void afterTextChanged(Editable s) {
					checkEmptyName(s, nameCaption, saveButton);
				}
			});
			checkEmptyName(nameEdit.getText(), nameCaption, saveButton);
		}

		nameIcon = view.findViewById(R.id.name_icon);
		TextView categoryEdit = view.findViewById(R.id.groupName);
		if (categoryEdit != null) {
			AndroidUtils.setTextPrimaryColor(view.getContext(), categoryEdit, nightMode);
			categoryEdit.setText(getCategoryInitValue());
		}

		descriptionEdit = view.findViewById(R.id.description_edit);
		addressEdit = view.findViewById(R.id.address_edit);
		AndroidUtils.setTextPrimaryColor(view.getContext(), descriptionEdit, nightMode);
		AndroidUtils.setTextPrimaryColor(view.getContext(), addressEdit, nightMode);
		AndroidUtils.setHintTextSecondaryColor(view.getContext(), descriptionEdit, nightMode);
		AndroidUtils.setHintTextSecondaryColor(view.getContext(), addressEdit, nightMode);
		if (getDescriptionInitValue() != null) {
			descriptionEdit.setText(getDescriptionInitValue());
		}

		descriptionCaption = view.findViewById(R.id.description);
		addressCaption = view.findViewById(R.id.address);
		addDelDescription = view.findViewById(R.id.description_button);
		addAddressBtn = view.findViewById(R.id.address_button);
		deleteAddressIcon = view.findViewById(R.id.delete_address_icon);
		deleteAddressIcon.setImageDrawable(getPaintedIcon(R.drawable.ic_action_trash_basket_16, activeColor));

		addDelDescription.setTextColor(activeColor);
		addAddressBtn.setTextColor(activeColor);
		Drawable addressIcon = getPaintedIcon(R.drawable.ic_action_location_16, activeColor);
		addAddressBtn.setCompoundDrawablesWithIntrinsicBounds(addressIcon, null, null, null);
		addDelDescription.setOnClickListener(v -> {
			if (descriptionCaption.getVisibility() != View.VISIBLE) {
				descriptionCaption.setVisibility(View.VISIBLE);
				addDelDescription.setText(view.getResources().getString(R.string.delete_description));
				View descriptionEdit = view.findViewById(R.id.description_edit);
				descriptionEdit.requestFocus();
				AndroidUtils.softKeyboardDelayed(getActivity(), descriptionEdit);
			} else {
				descriptionCaption.setVisibility(View.GONE);
				addDelDescription.setText(view.getResources().getString(R.string.add_description));
				AndroidUtils.hideSoftKeyboard(requireActivity(), descriptionEdit);
				descriptionEdit.clearFocus();
			}
			updateDescriptionIcon();
		});
		AndroidUiHelper.updateVisibility(addressCaption, false);

		String addressInitValue = getAddressInitValue();
		if (!Algorithms.isEmpty(addressInitValue)) {
			addressEdit.setText(addressInitValue);
			addAddressBtn.setText(addressInitValue);
			addressEdit.setSelection(addressInitValue.length());
			AndroidUiHelper.updateVisibility(deleteAddressIcon, true);
		} else {
			addAddressBtn.setText(getString(R.string.add_address));
			AndroidUiHelper.updateVisibility(deleteAddressIcon, false);
		}

		deleteAddressIcon.setOnClickListener(v -> {
			addressEdit.setText("");
			addAddressBtn.setText(view.getResources().getString(R.string.add_address));
			AndroidUiHelper.updateVisibility(addressCaption, false);
			AndroidUiHelper.updateVisibility(deleteAddressIcon, false);
		});

		final View addressRow = view.findViewById(R.id.address_row);
		addAddressBtn.setOnClickListener(v -> {
			if (addressCaption.getVisibility() != View.VISIBLE) {
				addressCaption.setVisibility(View.VISIBLE);
				addressEdit.requestFocus();
				addressEdit.setSelection(addressEdit.getText().length());
				AndroidUtils.softKeyboardDelayed(requireActivity(), addressEdit);
				AndroidUiHelper.updateVisibility(addressRow, false);
			} else {
				addressCaption.setVisibility(View.GONE);
				addAddressBtn.setText(getAddressTextValue());
				AndroidUtils.hideSoftKeyboard(requireActivity(), addressEdit);
				addressEdit.clearFocus();
			}
		});
		nameIcon.setImageDrawable(getNameIcon());

		if (app.accessibilityEnabled()) {
			nameCaption.setFocusable(true);
			nameEdit.setHint(R.string.access_hint_enter_name);
		}

		View deleteButton = view.findViewById(R.id.button_delete_container);
		deleteButton.setOnClickListener(v -> deletePressed());

		if (editor.isProcessingTemplate()) {
			View replaceButton = view.findViewById(R.id.button_replace_container);
			AndroidUiHelper.setVisibility(View.GONE, toolbarAction, replaceButton, deleteButton);
		}
		if (editor.isNew()) {
			toolbarAction.setImageDrawable(getPaintedIcon(R.drawable.ic_action_replace, activeColor));
			deleteButton.setVisibility(View.GONE);
			descriptionCaption.setVisibility(View.GONE);
			deleteIcon.setVisibility(View.GONE);
			nameEdit.selectAll();
			nameEdit.requestFocus();
			showKeyboard();
		} else {
			toolbarAction.setImageDrawable(getPaintedIcon(R.drawable.ic_action_delete_dark, activeColor));
			deleteButton.setVisibility(View.VISIBLE);
			deleteIcon.setVisibility(View.VISIBLE);
		}

		toolbarAction.setOnClickListener(view -> {
			if (!editor.isNew) {
				deletePressed();
			}
		});
		createGroupSelector();
		createIconSelector();
		createColorSelector();
		createShapeSelector();
		updateColorSelector(selectedColor);

		scrollView.setOnTouchListener((v, event) -> {
			descriptionEdit.getParent().requestDisallowInterceptTouchEvent(false);
			return false;
		});

		descriptionEdit.setOnTouchListener((v, event) -> {
			descriptionEdit.getParent().requestDisallowInterceptTouchEvent(true);
			return false;
		});
		view.getViewTreeObserver().addOnGlobalLayoutListener(getOnGlobalLayoutListener());
		return view;
	}

	private void updateDescriptionIcon() {
		int iconId;
		if (descriptionCaption.getVisibility() == View.VISIBLE) {
			iconId = R.drawable.ic_action_trash_basket_16;
		} else {
			iconId = R.drawable.ic_action_description_16;
		}
		int activeColor = ColorUtilities.getActiveColorId(nightMode);
		Drawable icon = getIcon(iconId, activeColor);
		addDelDescription.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
	}

	private void checkEmptyName(Editable name, TextInputLayout nameCaption, View saveButton) {
		if (name.toString().trim().isEmpty()) {
			nameCaption.setError(app.getString(R.string.please_provide_point_name_error));
			saveButton.setEnabled(false);
		} else {
			nameCaption.setError(null);
			saveButton.setEnabled(true);
		}
	}

	private ViewTreeObserver.OnGlobalLayoutListener getOnGlobalLayoutListener() {
		return () -> {
			Rect visibleDisplayFrame = new Rect();
			view.getWindowVisibleDisplayFrame(visibleDisplayFrame);
			int layoutHeight = visibleDisplayFrame.bottom;
			if (layoutHeight != layoutHeightPrevious) {
				FrameLayout.LayoutParams rootViewLayout = (FrameLayout.LayoutParams) view.getLayoutParams();
				rootViewLayout.height = layoutHeight;
				view.requestLayout();
				layoutHeightPrevious = layoutHeight;
			}
		};
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		if (skipConfirmationDialog) {
			save(true);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (!descriptionEdit.getText().toString().isEmpty() || descriptionEdit.hasFocus()) {
			descriptionCaption.setVisibility(View.VISIBLE);
			addDelDescription.setText(app.getString(R.string.delete_description));
		} else {
			descriptionCaption.setVisibility(View.GONE);
			addDelDescription.setText(app.getString(R.string.add_description));
		}
		updateDescriptionIcon();
	}

	private void createGroupSelector() {
		groupListAdapter = new GroupAdapter();
		groupRecyclerView = view.findViewById(R.id.group_recycler_view);
		groupRecyclerView.setAdapter(groupListAdapter);
		groupRecyclerView.setLayoutManager(new LinearLayoutManager(app, RecyclerView.HORIZONTAL, false));
		setSelectedItemWithScroll(getCategoryInitValue());
	}

	private void createIconSelector() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			iconsCard = new IconsCard(mapActivity, getIconId(), getPreselectedIconName(), selectedColor);
			iconsCard.setListener(this);
			ViewGroup shapesCardContainer = view.findViewById(R.id.icons_card);
			shapesCardContainer.addView(iconsCard.build(mapActivity));
		}
	}

	private void createColorSelector() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			List<Integer> colors = new ArrayList<>();
			for (int color : ColorDialogs.pallette) {
				colors.add(color);
			}
			int customColor = getPointColor();
			if (!ColorDialogs.isPaletteColor(customColor)) {
				colors.add(customColor);
			}
			colorsCard = new ColorsCard(mapActivity, null, this, selectedColor,
					colors, app.getSettings().CUSTOM_TRACK_COLORS, true);
			colorsCard.setListener(this);
			LinearLayout selectColor = view.findViewById(R.id.select_color);
			selectColor.addView(colorsCard.build(view.getContext()));
		}
	}

	@Override
	public void onColorSelected(Integer prevColor, int newColor) {
		colorsCard.onColorSelected(prevColor, newColor);
		int color = colorsCard.getSelectedColor();
		updateColorSelector(color);
	}

	@Override
	public void onCardLayoutNeeded(@NonNull BaseCard card) {
	}

	@Override
	public void onCardPressed(@NonNull BaseCard card) {
		if (card instanceof IconsCard) {
			setIcon(iconsCard.getSelectedIconId());
			updateNameIcon();
		} else if (card instanceof ColorsCard) {
			int color = ((ColorsCard) card).getSelectedColor();
			updateColorSelector(color);
		} else if (card instanceof ShapesCard) {
			selectedShape = shapesCard.getSelectedShape();
			setBackgroundType(selectedShape);
			updateNameIcon();
			((TextView) view.findViewById(R.id.shape_name)).setText(selectedShape.getNameId());
		}
	}

	@Override
	public void onCardButtonPressed(@NonNull BaseCard card, int buttonIndex) {
	}

	private void updateColorSelector(int color) {
		((TextView) view.findViewById(R.id.color_name)).setText(ColorDialogs.getColorName(color));
		selectedColor = color;
		setColor(color);
		iconsCard.updateSelectedColor(color);
		shapesCard.updateSelectedColor(color);
		updateNameIcon();
	}

	private void createShapeSelector() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			shapesCard = new ShapesCard(mapActivity, selectedShape, selectedColor);
			shapesCard.setListener(this);
			ViewGroup shapesCardContainer = view.findViewById(R.id.shapes_card);
			shapesCardContainer.addView(shapesCard.build(mapActivity));
		}
	}

	@Nullable
	protected String getNameFromIconId(int iconId) {
		return RenderingIcons.getBigIconName(iconId);
	}

	protected int getIconIdByName(String iconName) {
		return RenderingIcons.getBigIconResourceId(iconName);
	}

	protected int getDefaultIconId() {
		try {
			String iconName = getDefaultIconName();
			return getIconIdByName(iconName);
		} catch (Exception e) {
			return DEFAULT_UI_ICON_ID;
		}
	}

	protected String getDefaultIconName() {
		String preselectedIconName = getPreselectedIconName();
		List<String> lastUsedIcons = app.getSettings().LAST_USED_FAV_ICONS.getStringsList();
		if (!Algorithms.isEmpty(preselectedIconName)) {
			return preselectedIconName;
		} else if (!Algorithms.isEmpty(lastUsedIcons)) {
			return lastUsedIcons.get(0);
		}
		return DEFAULT_ICON_NAME;
	}

	protected void addLastUsedIcon(@DrawableRes int iconId) {
		String iconName = RenderingIcons.getBigIconName(iconId);
		if (!Algorithms.isEmpty(iconName)) {
			addLastUsedIcon(iconName);
		}
	}

	protected void addLastUsedIcon(@NonNull String iconName) {
		iconsCard.addLastUsedIcon(iconName);
	}

	private void updateNameIcon() {
		if (nameIcon != null) {
			nameIcon.setImageDrawable(getNameIcon());
		}
	}

	@Nullable
	protected DialogFragment createSelectCategoryDialog() {
		PointEditor editor = getEditor();
		if (editor != null) {
			return SelectFavoriteCategoryBottomSheet.createInstance(editor.getFragmentTag(), getSelectedCategory());
		} else {
			return null;
		}
	}

	public String getSelectedCategory() {
		if (groupListAdapter != null && groupListAdapter.getSelectedItem() != null) {
			return groupListAdapter.getSelectedItem();
		}
		return getCategoryInitValue();
	}

	@Nullable
	protected AddNewFavoriteCategoryBottomSheet createAddCategoryDialog() {
		PointEditor editor = getEditor();
		if (editor != null) {
			return AddNewFavoriteCategoryBottomSheet.createInstance(editor.getFragmentTag(), getCategories(),
					!editor.getFragmentTag().equals(FavoritePointEditor.TAG));
		} else {
			return null;
		}
	}

	@Override
	public void onDestroyView() {
		PointEditor editor = getEditor();
		if (!wasSaved() && editor != null && !editor.isNew() && !cancelled) {
			save(false);
		}
		super.onDestroyView();
	}

	@Override
	public int getStatusBarColorId() {
		View view = getView();
		if (view != null && !nightMode) {
			view.setSystemUiVisibility(view.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
		}
		return ColorUtilities.getListBgColorId(nightMode);
	}

	@Override
	protected boolean isFullScreenAllowed() {
		return true;
	}

	private void showKeyboard() {
		FragmentActivity activity = getActivity();
		if (!skipConfirmationDialog && activity != null) {
			InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
			if (inputMethodManager != null) {
				inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
			}
		}
	}

	private void hideKeyboard() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
			if (inputMethodManager != null) {
				View currentFocus = activity.getCurrentFocus();
				if (currentFocus != null) {
					IBinder windowToken = currentFocus.getWindowToken();
					if (windowToken != null) {
						inputMethodManager.hideSoftInputFromWindow(windowToken, 0);
					}
				}
			}
		}
	}

	private void savePressed() {
		save(true);
	}

	private void deletePressed() {
		delete(true);
	}

	public void setCategory(String name, int color) {
		setSelectedItemWithScroll(name);
		updateColorSelector(color);
		AndroidUiHelper.updateVisibility(addToHiddenGroupInfo, !isCategoryVisible(name));
	}

	@SuppressLint("NotifyDataSetChanged")
	private void setSelectedItemWithScroll(String name) {
		groupListAdapter.fillGroups();
		groupListAdapter.setSelectedItemName(name);
		groupListAdapter.notifyDataSetChanged();
		int position = 0;
		PointEditor editor = getEditor();
		if (editor != null) {
			position = groupListAdapter.items.size() == groupListAdapter.getItemPosition(name) + 1
					? groupListAdapter.getItemPosition(name) + 1
					: groupListAdapter.getItemPosition(name);
		}
		groupRecyclerView.scrollToPosition(position);
	}

	protected String getLastUsedGroup() {
		return "";
	}

	protected String getDefaultCategoryName() {
		return getString(R.string.shared_string_none);
	}

	@Nullable
	protected MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}

	public void dismiss() {
		dismiss(false);
	}

	public void dismiss(boolean includingMenu) {
		hideKeyboard();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			MapContextMenu mapContextMenu = mapActivity.getContextMenu();
			if (includingMenu) {
				mapActivity.getSupportFragmentManager().popBackStack();
				mapContextMenu.close();
			} else {
				mapActivity.getSupportFragmentManager().popBackStack();
				if (!mapContextMenu.isVisible() && mapContextMenu.isActive()) {
					mapContextMenu.show();
				}
			}
		}
	}

	protected abstract boolean wasSaved();

	protected abstract void save(boolean needDismiss);

	protected abstract void delete(boolean needDismiss);

	@Nullable
	public abstract PointEditor getEditor();

	public abstract String getToolbarTitle();

	@ColorInt
	public abstract int getCategoryColor(String category);

	public abstract int getCategoryPointsCount(String category);

	public abstract void setColor(int color);

	public abstract void setBackgroundType(BackgroundType backgroundType);

	public abstract void setIcon(int iconId);

	public abstract String getNameInitValue();

	public abstract String getCategoryInitValue();

	public abstract String getDescriptionInitValue();

	public abstract String getAddressInitValue();

	public abstract Drawable getNameIcon();

	public abstract int getDefaultColor();

	public abstract int getPointColor();

	public abstract BackgroundType getBackgroundType();

	public abstract int getIconId();

	@Nullable
	public abstract String getPreselectedIconName();

	public abstract Set<String> getCategories();

	protected boolean isCategoryVisible(String name) {
		return true;
	}

	String getNameTextValue() {
		EditText nameEdit = view.findViewById(R.id.name_edit);
		return nameEdit.getText().toString().trim();
	}

	String getCategoryTextValue() {
		RecyclerView recyclerView = view.findViewById(R.id.group_recycler_view);
		if (recyclerView.getAdapter() != null) {
			String name = ((GroupAdapter) recyclerView.getAdapter()).getSelectedItem();
			if (isPersonalCategoryDisplayName(requireContext(), name)) {
				return PERSONAL_CATEGORY;
			}
			if (name.equals(getDefaultCategoryName())) {
				return "";
			}
			return name;
		}
		return "";
	}

	String getDescriptionTextValue() {
		EditText descriptionEdit = view.findViewById(R.id.description_edit);
		String res = descriptionEdit.getText().toString().trim();
		return Algorithms.isEmpty(res) ? null : res;
	}

	String getAddressTextValue() {
		EditText addressEdit = view.findViewById(R.id.address_edit);
		String res = addressEdit.getText().toString().trim();
		return Algorithms.isEmpty(res) ? null : res;
	}

	protected Drawable getPaintedIcon(int iconId, int color) {
		return getPaintedContentIcon(iconId, color);
	}

	public void showExitDialog() {
		hideKeyboard();
		if (!wasSaved()) {
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null){
				ExitBottomSheetDialogFragment.showInstance(mapActivity.getSupportFragmentManager(), this, getString(R.string.exit_without_saving_warning));
			}
		} else {
			exitEditing();
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == ExitBottomSheetDialogFragment.REQUEST_CODE){
			if (resultCode == ExitBottomSheetDialogFragment.EXIT_RESULT_CODE){
				exitEditing();
			} else if (resultCode == ExitBottomSheetDialogFragment.SAVE_RESULT_CODE) {
				savePressed();
			}
		}
	}

	public void exitEditing() {
		cancelled = true;
		dismiss();
	}

	class GroupAdapter extends RecyclerView.Adapter<GroupsViewHolder> {

		private static final int VIEW_TYPE_FOOTER = 1;
		private static final int VIEW_TYPE_CELL = 0;
		List<String> items = new ArrayList<>();

		void setSelectedItemName(String selectedItemName) {
			this.selectedItemName = selectedItemName;
		}

		String selectedItemName;

		GroupAdapter() {
			fillGroups();
		}

		private void fillGroups() {
			items.clear();
			items.addAll(getCategories());
		}

		@NonNull
		@Override
		public GroupsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			Context context = parent.getContext();
			View view = LayoutInflater.from(context)
					.inflate(R.layout.point_editor_group_select_item, parent, false);
			int activeColor = ColorUtilities.getActiveColor(context, nightMode);
			if (viewType != VIEW_TYPE_CELL) {
				Drawable iconAdd = getPaintedIcon(R.drawable.ic_action_add, activeColor);
				((ImageView) view.findViewById(R.id.groupIcon)).setImageDrawable(iconAdd);
				((TextView) view.findViewById(R.id.groupName)).setText(R.string.add_group);
				GradientDrawable rectContourDrawable = (GradientDrawable) AppCompatResources.getDrawable(app,
						R.drawable.bg_select_group_button_outline);
				if (rectContourDrawable != null) {
					int strokeColor = ColorUtilities.getStrokedButtonsOutlineColor(context, nightMode);
					rectContourDrawable.setStroke(AndroidUtils.dpToPx(app, 1), strokeColor);
					((ImageView) view.findViewById(R.id.outlineRect)).setImageDrawable(rectContourDrawable);
				}
			}
			((TextView) view.findViewById(R.id.groupName)).setTextColor(activeColor);
			return new GroupsViewHolder(view);
		}

		@Override
		public void onBindViewHolder(@NonNull final GroupsViewHolder holder, int position) {
			if (position == items.size()) {
				holder.groupButton.setOnClickListener(view -> {
					FragmentManager fragmentManager = getFragmentManager();
					DialogFragment dialogFragment = createAddCategoryDialog();
					if (fragmentManager != null && dialogFragment != null) {
						dialogFragment.show(fragmentManager, SelectFavoriteCategoryBottomSheet.class.getSimpleName());
					}
				});
			} else {
				holder.groupButton.setOnClickListener(view -> {
					int previousSelectedPosition = getItemPosition(selectedItemName);
					selectedItemName = items.get(holder.getAdapterPosition());
					updateColorSelector(getCategoryColor(selectedItemName));
					AndroidUiHelper.updateVisibility(addToHiddenGroupInfo, !isCategoryVisible(selectedItemName));
					notifyItemChanged(holder.getAdapterPosition());
					notifyItemChanged(previousSelectedPosition);
				});
				final String group = items.get(position);
				holder.groupName.setText(group);
				holder.pointsCounter.setText(String.valueOf(getCategoryPointsCount(group)));
				int strokeColor;
				int strokeWidth;
				if (selectedItemName != null && selectedItemName.equals(items.get(position))) {
					strokeColor = ColorUtilities.getActiveColor(app, nightMode);
					strokeWidth = 2;
				} else {
					strokeColor = ContextCompat.getColor(app, nightMode ? R.color.stroked_buttons_and_links_outline_dark
							: R.color.stroked_buttons_and_links_outline_light);
					strokeWidth = 1;
				}
				GradientDrawable rectContourDrawable = (GradientDrawable) AppCompatResources.getDrawable(app,
						R.drawable.bg_select_group_button_outline);
				if (rectContourDrawable != null) {
					rectContourDrawable.setStroke(AndroidUtils.dpToPx(app, strokeWidth), strokeColor);
					holder.groupButton.setImageDrawable(rectContourDrawable);
				}
				int color;
				int iconID;
				if (isCategoryVisible(group)) {
					int categoryColor = getCategoryColor(group);
					color = categoryColor == 0 ? getDefaultColor() : categoryColor;
					iconID = R.drawable.ic_action_folder;
					holder.groupName.setTypeface(null, Typeface.NORMAL);
				} else {
					color = ContextCompat.getColor(app, R.color.text_color_secondary_light);
					iconID = R.drawable.ic_action_hide;
					holder.groupName.setTypeface(null, Typeface.ITALIC);
				}
				holder.groupIcon.setImageDrawable(UiUtilities.tintDrawable(
						AppCompatResources.getDrawable(app, iconID), color));
			}
			AndroidUtils.setBackground(app, holder.groupButton, nightMode, R.drawable.ripple_solid_light_6dp,
					R.drawable.ripple_solid_dark_6dp);
		}

		@Override
		public int getItemViewType(int position) {
			return (position == items.size()) ? VIEW_TYPE_FOOTER : VIEW_TYPE_CELL;
		}

		@Override
		public int getItemCount() {
			return items == null ? 0 : items.size() + 1;
		}

		String getSelectedItem() {
			return selectedItemName;
		}

		int getItemPosition(String name) {
			return items.indexOf(name);
		}
	}

	static class GroupsViewHolder extends RecyclerView.ViewHolder {

		final TextView pointsCounter;
		final TextView groupName;
		final ImageView groupIcon;
		final ImageView groupButton;

		GroupsViewHolder(View itemView) {
			super(itemView);
			pointsCounter = itemView.findViewById(R.id.counter);
			groupName = itemView.findViewById(R.id.groupName);
			groupIcon = itemView.findViewById(R.id.groupIcon);
			groupButton = itemView.findViewById(R.id.outlineRect);
		}
	}
}
