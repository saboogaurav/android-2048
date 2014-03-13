package org.solovyev.android.games.game2048;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.*;
import org.andengine.engine.camera.Camera;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.andengine.entity.Entity;
import org.andengine.entity.IEntity;
import org.andengine.entity.modifier.IEntityModifier;
import org.andengine.entity.modifier.MoveModifier;
import org.andengine.entity.modifier.ScaleModifier;
import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.background.Background;
import org.andengine.entity.text.AutoWrap;
import org.andengine.entity.text.Text;
import org.andengine.entity.text.TextOptions;
import org.andengine.entity.util.FPSLogger;
import org.andengine.opengl.font.Font;
import org.andengine.opengl.font.FontUtils;
import org.andengine.ui.activity.SimpleBaseGameActivity;
import org.andengine.util.color.Color;
import org.andengine.util.modifier.IModifier;
import org.solovyev.android.Activities;
import org.solovyev.android.menu.*;
import org.solovyev.android.prefs.StringPreference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.andengine.engine.options.ScreenOrientation.PORTRAIT_FIXED;
import static org.andengine.util.HorizontalAlign.CENTER;
import static org.solovyev.android.Activities.restartActivity;
import static org.solovyev.android.games.game2048.CellStyle.newCellStyle;

public class GameActivity extends SimpleBaseGameActivity {

	@Nonnull
	private final StringPreference<String> state = StringPreference.of("state", null);

	@Nonnull
	private final SparseArray<CellStyle> cellStyles = new SparseArray<CellStyle>();

	@Nonnull
	private final CellStyle lastCellStyle = newCellStyle(2048, R.color.cell_text_2048, R.color.cell_bg_2048);

	{
		cellStyles.append(-1, newCellStyle(0, R.color.cell_wall_text, R.color.cell_wall_bg));
		cellStyles.append(0, newCellStyle(0, R.color.cell_text, R.color.cell_bg));
		cellStyles.append(2, newCellStyle(2, R.color.cell_text_2, R.color.cell_bg_2));
		cellStyles.append(4, newCellStyle(4, R.color.cell_text_4, R.color.cell_bg_4));
		cellStyles.append(8, newCellStyle(8, R.color.cell_text_8, R.color.cell_bg_8));
		cellStyles.append(16, newCellStyle(16, R.color.cell_text_16, R.color.cell_bg_16));
		cellStyles.append(32, newCellStyle(32, R.color.cell_text_32, R.color.cell_bg_32));
		cellStyles.append(64, newCellStyle(64, R.color.cell_text_64, R.color.cell_bg_64));
		cellStyles.append(128, newCellStyle(128, R.color.cell_text_128, R.color.cell_bg_128));
		cellStyles.append(256, newCellStyle(256, R.color.cell_text_256, R.color.cell_bg_256));
		cellStyles.append(512, newCellStyle(512, R.color.cell_text_512, R.color.cell_bg_512));
		cellStyles.append(1024, newCellStyle(1024, R.color.cell_text_1024, R.color.cell_bg_1024));
		cellStyles.append(2048, lastCellStyle);
	}

	@Nonnull
	private final Dimensions d = new Dimensions();

	@Nonnull
	private final Game game = Game.newGame();

	@Nonnull
	private final Fonts fonts = new Fonts(this);

	@Nonnull
	private GestureDetector gestureDetector;

	private boolean animating = false;

	@Nullable
	private ActivityMenu<Menu, MenuItem> menu;

	@Nonnull
	private Text scoreText;

	@Override
	protected void onCreateResources() {
		for (int i = 0; i < cellStyles.size(); i++) {
			cellStyles.valueAt(i).loadFont(this, d.cellTextSize);
		}
	}

	@Nonnull
	public Fonts getFonts() {
		return fonts;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		gestureDetector = new GestureDetector(this, new GestureListener());
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		if (!animating && isGameRunning()) {
			gestureDetector.onTouchEvent(ev);
		}
		return super.dispatchTouchEvent(ev);
	}

	@Override
	protected Scene onCreateScene() {
		this.mEngine.registerUpdateHandler(new FPSLogger());

		game.loadState(state.getPreference(App.getPreferences()));

		final Scene scene = new Scene();
		scene.setBackground(new Background(getColor(R.color.bg)));

		final String appName = getString(R.string.app_name);
		final Font titleFont = getFonts().getFont(d.titleSize, R.color.text, appName.length(), true);
		scene.attachChild(new Text(d.title.x, d.title.y, titleFont, appName, getVertexBufferObjectManager()));

		final String score = getString(R.string.score);
		final Font scoreFont = getFonts().getFont(d.textSize, R.color.text, 20);
		scoreText = new Text(d.score.x, d.score.y, scoreFont, score, getVertexBufferObjectManager());
		scoreText.setText(getString(R.string.score, game.getScore().getPoints()));
		scene.attachChild(scoreText);

		scene.attachChild(createBoard());

		final String rules = getString(R.string.rules);
		final Font textFont = getFonts().getFont(d.rulesSize, R.color.text,  30);
		scene.attachChild(new Text(d.rules.x, d.rules.y, textFont, rules, new TextOptions(AutoWrap.WORDS, d.board.width()), getVertexBufferObjectManager()));

		return scene;
	}

	@Nonnull
	private IEntity createBoard() {
		final Entity boardView = new Entity(d.board.left, d.board.top);
		final Rectangle boardRect = new Rectangle(0, 0, d.board.width(), d.board.height(), getVertexBufferObjectManager());
		boardRect.setColor(getColor(R.color.board_bg));
		boardView.attachChild(boardRect);

		final Board board = game.getBoard();
		board.setView(boardView);
		final int size = board.getSize();
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				boardView.attachChild(createNotValueCell(i, j));
			}
		}

		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				final IEntity cell = createValueCell(i, j);
				if (cell != null) {
					boardView.attachChild(cell);
				}
			}
		}

		return boardView;
	}


	@Nullable
	private IEntity createValueCell(int i, int j) {
		final Cell c = game.getBoard().getCell(i, j);
		if (c.hasValue()) {
			return createValueCell(i, j, c);
		} else {
			return null;
		}
	}

	@Nonnull
	private IEntity createValueCell(int i, int j, @Nonnull Cell cell) {
		final Rectangle cellView = createCell(i, j);
		final String cellValue = String.valueOf(cell.getValue());
		final CellStyle cellStyle = cellStyles.get(cell.getValue(), lastCellStyle);
		cellView.setColor(getColor(cellStyle.getBgColorResId()));
		Font cellFont = cellStyle.getFont();
		float textWidth = FontUtils.measureText(cellFont, cellValue);
		float textHeight = cellFont.getLineHeight();
		if (textWidth > d.cellTextSize) {
			cellFont = cellStyle.getFont(this, d.cellTextSize * d.cellTextSize / textWidth);
			textWidth = FontUtils.measureText(cellFont, cellValue);
			textHeight = cellFont.getLineHeight();
		}
		cellView.attachChild(new Text(d.cellSize / 2 - textWidth / 2, d.cellSize / 2 - textHeight * 5 / 12, cellFont, cellValue, new TextOptions(CENTER), getVertexBufferObjectManager()));
		cell.setView(cellView);
		cellView.registerEntityModifier(new ScaleModifier(0.2f, 1f, 1.1f, new IEntityModifier.IEntityModifierListener() {
			@Override
			public void onModifierStarted(IModifier<IEntity> pModifier, IEntity pItem) {
			}

			@Override
			public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {
				cellView.registerEntityModifier(new ScaleModifier(0.2f, 1.1f, 1f));
			}
		}));
		return cellView;
	}

	@Nonnull
	Color getColor(int colorResId) {
		final int color = getResources().getColor(colorResId);
		return new Color(android.graphics.Color.red(color) / 255.f,
				android.graphics.Color.green(color) / 255.f,
				android.graphics.Color.blue(color) / 255.f,
				android.graphics.Color.alpha(color) / 255.f);
	}

	@Nonnull
	private IEntity createNotValueCell(int i, int j) {
		final Cell c = game.getBoard().getCell(i, j);
		if (c.isWall()) {
			final Rectangle cell = createCell(i, j);
			final CellStyle cellStyle = cellStyles.get(c.getValue(), lastCellStyle);
			cell.setColor(getColor(cellStyle.getBgColorResId()));
			return cell;
		} else {
			return createCell(i, j);
		}
	}

	private Rectangle createCell(int i, int j) {
		final Point position = newCellPosition(i, j);
		final Rectangle cell = new Rectangle(position.x, position.y, d.cellSize, d.cellSize, getVertexBufferObjectManager());

		final CellStyle cellStyle = cellStyles.get(0, lastCellStyle);
		cell.setColor(getColor(cellStyle.getBgColorResId()));
		return cell;
	}

	@Nonnull
	private Point newCellPosition(int row, int col) {
		final float x = col * d.cellSize + (col + 1) * d.cellPadding;
		final float y = row * d.cellSize + (row + 1) * d.cellPadding;
		return new Point((int) x, (int) y);
	}

	@Override
	public EngineOptions onCreateEngineOptions() {
		d.calculate(this, game);
		final Camera camera = new Camera(0, 0, d.width, d.height);
		return new EngineOptions(true, PORTRAIT_FIXED, new RatioResolutionPolicy(d.width, d.height), camera);
	}

	@Override
	protected void onPause() {
		if (game.isStateLoaded()) {
			state.putPreference(App.getPreferences(), game.saveState());
		}
		super.onPause();
	}

	private static final class Dimensions {

		private float titleSize;
		private float textSize;
		private float rulesSize;
		private Point title = new Point();
		private Point score = new Point();
		private Point rules = new Point();
		private float padding;
		private float textPadding;
		private final Rect board = new Rect();
		private float cellPadding;
		private float cellSize;
		private float cellTextSize;
		private float width;
		private float height;

		private Dimensions() {
		}

		private void calculate(@Nonnull Activity activity, @Nonnull Game game) {
			final Point displaySize = getDisplaySize(activity);
			width = min(displaySize.x, displaySize.y);
			height = max(displaySize.x, displaySize.y);
			titleSize = 0.08f * height;
			textSize = titleSize / 2;
			rulesSize = textSize * 3 / 4;
			padding = titleSize;
			textPadding = textSize;
			title.y = (int) textPadding;
			score.y = (int) (title.y + textPadding + titleSize);
			calculateBoard();
			final int size = game.getBoard().getSize();
			cellPadding = 0.15f * board.width() / size;
			cellSize = (board.width() - (size + 1) * cellPadding) / size;
			cellTextSize = cellSize * 2 / 3;
			title.x = board.left;
			score.x = board.left;
			rules.x = board.left;
			rules.y = (int) (board.bottom + textPadding);
		}

		private float calculateBoard() {
			final float desiredSize = min(width, height) * 5f / 6f;
			final float boardSize = min(desiredSize, height / 2);
			board.left = (int) (width / 2 - boardSize / 2);
			board.top = (int) (score.y + textSize + textPadding);
			board.right = (int) (board.left + boardSize);
			board.bottom = (int) (board.top + boardSize);
			return boardSize;
		}

		@Nonnull
		private static Point getDisplaySize(@Nonnull Activity activity) {
			final Display display = activity.getWindowManager().getDefaultDisplay();
			final Point displaySize = new Point();
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
				display.getSize(displaySize);
			} else {
				final DisplayMetrics dm = new DisplayMetrics();
				display.getMetrics(dm);

				displaySize.x = dm.widthPixels;
				displaySize.y = dm.heightPixels;
			}
			return displaySize;
		}
	}

	private class GestureListener extends GestureDetector.SimpleOnGestureListener {
		private final ViewConfiguration vc = ViewConfiguration.get(GameActivity.this);
		private final int swipeMinDistance = vc.getScaledPagingTouchSlop();
		private final int swipeThresholdVelocity = vc.getScaledMinimumFlingVelocity();
		private final int swipeMaxOffPath = Integer.MAX_VALUE;

		public boolean onFling(MotionEvent e1, MotionEvent e2, float xVelocity, float yVelocity) {
			final float xDiff = e2.getX() - e1.getX();
			final float yDiff = e2.getY() - e1.getY();
			if (Math.abs(xDiff) > swipeMaxOffPath && Math.abs(yDiff) > swipeMaxOffPath) {
				return false;
			}

			final boolean xVelocityOk = Math.abs(xVelocity) > swipeThresholdVelocity;
			final boolean yVelocityOk = Math.abs(yVelocity) > swipeThresholdVelocity;

			Direction direction;
			if (Math.abs(xDiff) > Math.abs(yDiff)) {
				direction = checkLeftRightSwipes(xDiff, xVelocityOk);
				if (direction == null) {
					checkUpDownSwipes(yDiff, yVelocityOk);
				}
			} else {
				direction = checkUpDownSwipes(yDiff, yVelocityOk);
				if (direction == null) {
					checkLeftRightSwipes(xDiff, xVelocityOk);
				}
			}

			if (direction != null) {
				go(direction);
			}

			return false;
		}

		@Nullable
		private Direction checkLeftRightSwipes(float xDiff, boolean xVelocityOk) {
			if (xDiff > swipeMinDistance && xVelocityOk) {
				return Direction.right;
			} else if (-xDiff > swipeMinDistance && xVelocityOk) {
				return Direction.left;
			}

			return null;
		}

		@Nullable
		private Direction checkUpDownSwipes(float yDiff, boolean yVelocityOk) {
			if (-yDiff > swipeMinDistance && yVelocityOk) {
				return Direction.up;
			} else if (yDiff > swipeMinDistance && yVelocityOk) {
				return Direction.down;
			}

			return null;
		}
	}

	private void go(@Nonnull Direction direction) {
		final CellsAnimationListener cellsAnimationListener = new CellsAnimationListener();

		final List<CellChange.Move> moves = game.go(direction);
		for (CellChange.Move move : moves) {
			final Point from = newCellPosition(move.from.x, move.from.y);
			final Point to = newCellPosition(move.to.x, move.to.y);
			final IEntity cellView = move.cell.getView();
			cellView.registerEntityModifier(new MoveModifier(0.2f, from.x, to.x, from.y, to.y, cellsAnimationListener));
			if (move instanceof CellChange.Move.Merge) {
				final CellChange.Move.Merge merge = (CellChange.Move.Merge) move;
				cellsAnimationListener.merges.add(merge);
			}
		}
	}

	private class CellsAnimationListener implements IEntityModifier.IEntityModifierListener {

		@Nonnull
		private final List<CellChange.Move.Merge> merges = new ArrayList<CellChange.Move.Merge>();

		private int count = 0;

		@Override
		public void onModifierStarted(IModifier<IEntity> pModifier, IEntity pItem) {
			count++;
			animating = true;
		}

		@Override
		public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {
			count--;
			if (count == 0) {
				runOnUpdateThread(new Runnable() {
					@Override
					public void run() {
						final IEntity boardView = game.getBoard().getView();

						scoreText.setText(getString(R.string.score, game.getScore().getPoints()));

						for (CellChange.Move.Merge merge : merges) {
							boardView.detachChild(merge.cell.getView());
							boardView.attachChild(createValueCell(merge.to.x, merge.to.y));
							boardView.detachChild(merge.removedCell.getView());
						}

						final List<CellChange.New> newCells = game.prepareNextTurn();
						for (CellChange.New newCell : newCells) {
							boardView.attachChild(createValueCell(newCell.position.x, newCell.position.y, newCell.cell));
						}
					}
				});
				animating = false;
			}
		}
	}

	private void restartGame() {
		game.reset();
		restartActivity(this);
	}

	private void shareScore() {
		final Intent shareIntent = new Intent();
		shareIntent.setAction(Intent.ACTION_SEND);
		shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_message, game.getScore().getPoints(), App.SHARE_URL));
		shareIntent.setType("text/plain");
		startActivity(shareIntent);
	}

	/*
    **********************************************************************
    *
    *                           MENU
    *
    **********************************************************************
    */

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return this.menu.onPrepareOptionsMenu(this, menu);
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		if (this.menu == null) {
			final List<IdentifiableMenuItem<MenuItem>> items = new ArrayList<IdentifiableMenuItem<MenuItem>>();
			items.add(new RestartMenuItem());
			items.add(new ShareMenuItem());
			this.menu = ListActivityMenu.fromResource(R.menu.menu, items, AndroidMenuHelper.getInstance());
		}
		return this.menu.onCreateOptionsMenu(this, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return this.menu.onOptionsItemSelected(this, item);
	}

	public class RestartMenuItem implements IdentifiableMenuItem<MenuItem> {

		public RestartMenuItem() {
		}

		@Nonnull
		@Override
		public Integer getItemId() {
			return R.id.menu_restart;
		}

		@Override
		public void onClick(@Nonnull MenuItem data, @Nonnull Context context) {
			restartGame();
		}
	}

	public class ShareMenuItem implements IdentifiableMenuItem<MenuItem> {

		public ShareMenuItem() {
		}

		@Nonnull
		@Override
		public Integer getItemId() {
			return R.id.menu_share;
		}

		@Override
		public void onClick(@Nonnull MenuItem data, @Nonnull Context context) {
			shareScore();
		}
	}
}