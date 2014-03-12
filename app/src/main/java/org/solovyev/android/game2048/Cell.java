package org.solovyev.android.game2048;

import org.andengine.entity.IEntity;

import javax.annotation.Nonnull;

public final class Cell {

	static final int START_VALUE = 2;
	static final int NO_VALUE = 0;
	static final int WALL = -1;

	private int value;

	private boolean merged = false;

	@Nonnull
	private IEntity view;

	public Cell(int value) {
		this.value = value;
	}

	@Nonnull
	public static Cell newWall() {
		return new Cell(WALL);
	}

	@Nonnull
	public static Cell newEmpty() {
		return new Cell(NO_VALUE);
	}

	public int getValue() {
		return value;
	}

	void setValue(int value) {
		this.value = value;
	}

	public boolean hasValue() {
		return value > NO_VALUE;
	}

	public boolean isEmpty() {
		return value == NO_VALUE;
	}

	public void setView(@Nonnull IEntity view) {
		this.view = view;
	}

	@Nonnull
	public IEntity getView() {
		return view;
	}

	public boolean isWall() {
		return value == WALL;
	}

	public void merge() {
		assert value > 0;
		value = 2 * value;
		merged = true;
	}

	public boolean isMerged() {
		return merged;
	}

	void unmerge() {
		merged = false;
	}
}
