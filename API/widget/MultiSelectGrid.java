/*
 * 通用多选网格控件（可复用）
 *
 * 相册级交互：
 * - 勾选框常显：未选中=描边圆环，选中=实心 ✓
 * - 仅当按下点落在勾选框上才进入拖动连选（按起始框状态连选/连取消）
 * - 点格子主体：浏览态打开预览，多选态切换勾选
 * - 长按格子：进入多选并选中该格（Touch 自实现，不用 OnLongClickListener）
 */

interface MultiGridBinder {
    void bindImage(int index, ImageView image);
}

interface MultiGridListener {
    void onItemClick(int index, boolean selectionMode);
    void onSelectionChanged(int selectedCount, boolean selectionMode);
}

class MultiSelectGrid {
    final Activity activity;
    final boolean isDark;
    final int primaryColor;
    final int ringColor;
    final int onSurfaceColor;
    final int onSurfaceVariantColor;
    final LinearLayout root;
    final LinearLayout rowsHost;
    final LinearLayout actionBar;
    final TextView countView;
    final TextView cancelView;
    final TextView secondaryView;
    final TextView batchView;

    int columns = 3;
    int itemCount = 0;
    boolean selectionMode = false;
    final List selected = new ArrayList();
    final List cells = new ArrayList();
    final List checkViews = new ArrayList();
    final List imageViews = new ArrayList();
    MultiGridBinder binder = null;
    MultiGridListener listener = null;
    Runnable batchRunnable = null;
    Runnable secondaryRunnable = null;
    int cellSize = 0;
    LinearLayout currentRow = null;
    int cellsInCurrentRow = 0;
    int dragStartIndex = -1;
    boolean dragAnchor = false;

    MultiSelectGrid(Activity activity, boolean isDark) {
        this.activity = activity;
        this.isDark = isDark;
        this.primaryColor = pc(getSettingsThemeColor(activity, "primary"));
        this.ringColor = pc(isDark ? "#88FFFFFF" : "#66000000");
        this.onSurfaceColor = pc(getSettingsThemeColor(activity, "on_surface"));
        this.onSurfaceVariantColor = pc(getSettingsThemeColor(activity, "on_surface_variant"));
        this.root = new LinearLayout(activity);
        this.root.setOrientation(LinearLayout.VERTICAL);
        this.root.setPadding(dp(activity, 10), dp(activity, 6), dp(activity, 10), dp(activity, 6));

        this.rowsHost = new LinearLayout(activity);
        this.rowsHost.setOrientation(LinearLayout.VERTICAL);
        this.root.addView(this.rowsHost);

        this.actionBar = new LinearLayout(activity);
        this.actionBar.setOrientation(LinearLayout.HORIZONTAL);
        this.actionBar.setGravity(Gravity.CENTER_VERTICAL);
        this.actionBar.setPadding(dp(activity, 8), dp(activity, 8), dp(activity, 8), dp(activity, 8));
        this.actionBar.setBackground(roundRect(getAdaptiveCardBg(activity), dp(activity, 14)));
        this.actionBar.setVisibility(View.GONE);

        this.countView = new TextView(activity);
        this.countView.setText("已选 0");
        this.countView.setTextSize(13);
        this.countView.setTextColor(onSurfaceColor);
        this.countView.setPadding(dp(activity, 6), 0, dp(activity, 8), 0);
        this.actionBar.addView(this.countView);

        this.cancelView = createButton(activity, "取消", onSurfaceVariantColor, Color.TRANSPARENT, 12f, 10, 10, 6, false);
        this.cancelView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { setSelectionMode(false); }
        });
        this.actionBar.addView(this.cancelView);

        this.secondaryView = createButton(activity, "批量保存", onSurfaceColor, pc(isDark ? "#22FFFFFF" : "#11000000"), 12f, 10, 12, 6, false);
        LinearLayout.LayoutParams secondaryParams = new LinearLayout.LayoutParams(-2, -2);
        secondaryParams.leftMargin = dp(activity, 6);
        this.secondaryView.setLayoutParams(secondaryParams);
        this.secondaryView.setVisibility(View.GONE);
        this.secondaryView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (secondaryRunnable == null) return;
                if (getSelectedCount() == 0) {
                    qqToast(1, "请先选择头像");
                    return;
                }
                secondaryRunnable.run();
            }
        });
        this.actionBar.addView(this.secondaryView);

        this.batchView = createButton(activity, "批量删除", pc("#FFFFFFFF"), pc("#FFE53935"), 12f, 10, 14, 6, false);
        LinearLayout.LayoutParams batchParams = new LinearLayout.LayoutParams(0, -2, 1f);
        batchParams.leftMargin = dp(activity, 6);
        this.batchView.setLayoutParams(batchParams);
        this.batchView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (batchRunnable == null) return;
                if (getSelectedCount() == 0) {
                    qqToast(1, "请先选择头像");
                    return;
                }
                batchRunnable.run();
            }
        });
        this.actionBar.addView(this.batchView);

        this.root.addView(this.actionBar);
    }

    LinearLayout getView() { return root; }

    void setBinder(MultiGridBinder value) { this.binder = value; }

    void setListener(MultiGridListener value) { this.listener = value; }

    void setBatchAction(Runnable runnable) { this.batchRunnable = runnable; }

    void setSecondaryAction(Runnable runnable) {
        this.secondaryRunnable = runnable;
        secondaryView.setVisibility(runnable == null ? View.GONE : View.VISIBLE);
    }

    void setColumns(int value) {
        if (value < 1) value = 1;
        this.columns = value;
    }

    int getCount() { return itemCount; }

    int getCellSize() {
        if (cellSize > 0) return cellSize;
        int width = activity.getResources().getDisplayMetrics().widthPixels;
        int padding = dp(activity, 20) + dp(activity, 8) * (columns - 1) + dp(activity, 20);
        cellSize = Math.max(dp(activity, 64), (width - padding) / columns);
        return cellSize;
    }

    boolean isSelectionMode() { return selectionMode; }

    List getSelected() { return new ArrayList(selected); }

    int getSelectedCount() { return selected.size(); }

    void setItems(int count) {
        if (count < 0) count = 0;
        this.itemCount = count;
        pruneSelection(count);
        rebuild();
    }

    void appendItems(int total) {
        if (total <= itemCount) {
            setItems(total);
            return;
        }
        for (int i = itemCount; i < total; i++) addCell(i);
        itemCount = total;
        refreshAllCellVisuals();
        resetActionBarVisibility();
    }

    void pruneSelection(int count) {
        List keep = new ArrayList();
        for (int i = 0; i < selected.size(); i++) {
            Integer key = (Integer) selected.get(i);
            if (key.intValue() < count) keep.add(key);
        }
        selected.clear();
        selected.addAll(keep);
    }

    void rebuild() {
        resetRows();
        for (int i = 0; i < itemCount; i++) addCell(i);
        refreshAllCellVisuals();
        resetActionBarVisibility();
    }

    void resetRows() {
        rowsHost.removeAllViews();
        cells.clear();
        checkViews.clear();
        imageViews.clear();
        currentRow = null;
        cellsInCurrentRow = 0;
    }

    void addCell(int index) {
        if (currentRow == null || cellsInCurrentRow >= columns) {
            currentRow = new LinearLayout(activity);
            currentRow.setOrientation(LinearLayout.HORIZONTAL);
            currentRow.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
            rowParams.topMargin = dp(activity, 8);
            rowsHost.addView(currentRow, rowParams);
            cellsInCurrentRow = 0;
        }
        int size = getCellSize();
        View cell = buildCell(index, size);
        LinearLayout.LayoutParams cellParams = new LinearLayout.LayoutParams(size, size);
        if (cellsInCurrentRow > 0) cellParams.leftMargin = dp(activity, 8);
        currentRow.addView(cell, cellParams);
        cellsInCurrentRow++;
    }

    View buildCell(final int index, int size) {
        final FrameLayout cell = new FrameLayout(activity);
        cell.setBackground(roundRect(getAdaptiveCardBg(activity), dp(activity, 12)));

        ImageView image = new ImageView(activity);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setBackground(roundRect(pc(isDark ? "#33FFFFFF" : "#11000000"), dp(activity, 12)));
        cell.addView(image, new FrameLayout.LayoutParams(-1, -1));
        imageViews.add(image);
        if (binder != null) binder.bindImage(index, image);

        final TextView check = new TextView(activity);
        check.setTextSize(12);
        check.setTextColor(pc("#FFFFFFFF"));
        check.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams checkParams = new FrameLayout.LayoutParams(dp(activity, 22), dp(activity, 22));
        checkParams.gravity = Gravity.TOP | Gravity.RIGHT;
        checkParams.topMargin = dp(activity, 4);
        checkParams.rightMargin = dp(activity, 4);
        cell.addView(check, checkParams);
        checkViews.add(check);

        cell.setOnTouchListener(new View.OnTouchListener() {
            int downX = 0;
            int downY = 0;
            long downTime = 0;
            boolean dragging = false;
            boolean longFired = false;
            boolean moved = false;
            Runnable longTask = null;

            void cancelLongTask() {
                if (longTask != null && uiHandler != null) uiHandler.removeCallbacks(longTask);
                longTask = null;
            }

            boolean inCheckbox(float x, float y) {
                int edge = dp(activity, 30);
                return x >= cell.getWidth() - edge && y <= edge;
            }

            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    downX = (int) event.getRawX();
                    downY = (int) event.getRawY();
                    downTime = System.currentTimeMillis();
                    moved = false;
                    longFired = false;
                    dragging = false;
                    if (selectionMode && inCheckbox(event.getX(), event.getY())) {
                        dragging = true;
                        dragStartIndex = index;
                        dragAnchor = !selected.contains(Integer.valueOf(index));
                        setIndexSelected(index, dragAnchor);
                    } else {
                        final int idx = index;
                        longTask = new Runnable() {
                            public void run() {
                                longTask = null;
                                if (moved || selectionMode) return;
                                longFired = true;
                                setSelectionMode(true);
                                setIndexSelected(idx, true);
                                if (listener != null) listener.onSelectionChanged(getSelectedCount(), selectionMode);
                            }
                        };
                        if (uiHandler != null) uiHandler.postDelayed(longTask, 420);
                    }
                    return true;
                }
                if (action == MotionEvent.ACTION_MOVE) {
                    if (Math.abs(event.getRawX() - downX) > dp(activity, 10)
                            || Math.abs(event.getRawY() - downY) > dp(activity, 10)) {
                        moved = true;
                        cancelLongTask();
                    }
                    if (dragging && dragStartIndex >= 0) {
                        int current = cellIndexAt(event.getRawX(), event.getRawY());
                        if (current >= 0) applyDragRange(dragStartIndex, current);
                    }
                    return true;
                }
                if (action == MotionEvent.ACTION_UP) {
                    cancelLongTask();
                    if (dragging) {
                        dragging = false;
                        dragStartIndex = -1;
                        if (listener != null) listener.onSelectionChanged(getSelectedCount(), selectionMode);
                        return true;
                    }
                    if (!longFired && !moved) {
                        if (selectionMode) {
                            toggle(index);
                            if (listener != null) listener.onSelectionChanged(getSelectedCount(), selectionMode);
                        } else if (listener != null) {
                            listener.onItemClick(index, false);
                        }
                    }
                    return true;
                }
                if (action == MotionEvent.ACTION_CANCEL) {
                    cancelLongTask();
                    dragging = false;
                    dragStartIndex = -1;
                }
                return true;
            }
        });
        cells.add(cell);
        return cell;
    }

    int cellIndexAt(float rawX, float rawY) {
        android.graphics.Rect bounds = new android.graphics.Rect();
        int hitX = (int) rawX;
        int hitY = (int) rawY;
        for (int i = 0; i < cells.size(); i++) {
            View cell = (View) cells.get(i);
            if (cell.getGlobalVisibleRect(bounds) && bounds.contains(hitX, hitY)) return i;
        }
        return -1;
    }

    void applyDragRange(int start, int current) {
        int lo = start < current ? start : current;
        int hi = start < current ? current : start;
        for (int i = lo; i <= hi; i++) setIndexSelected(i, dragAnchor);
    }

    void setIndexSelected(int index, boolean on) {
        if (index < 0 || index >= itemCount) return;
        Integer key = Integer.valueOf(index);
        if (on == selected.contains(key)) return;
        if (on) selected.add(key);
        else selected.remove(key);
        refreshCellVisual(index);
        countView.setText("已选 " + getSelectedCount());
    }

    void toggle(int index) {
        setIndexSelected(index, !selected.contains(Integer.valueOf(index)));
    }

    void refreshCellVisual(int index) {
        if (index < 0 || index >= cells.size()) return;
        View cell = (View) cells.get(index);
        TextView check = (TextView) checkViews.get(index);
        boolean isSelected = selected.contains(Integer.valueOf(index));
        if (isSelected) {
            check.setText("✓");
            GradientDrawable filled = new GradientDrawable();
            filled.setShape(GradientDrawable.OVAL);
            filled.setColor(primaryColor);
            check.setBackground(filled);
            check.setVisibility(View.VISIBLE);
            cell.setAlpha(0.62f);
        } else {
            check.setText("");
            GradientDrawable ring = new GradientDrawable();
            ring.setShape(GradientDrawable.OVAL);
            ring.setColor(0x00000000);
            ring.setStroke(dp(activity, 2), ringColor);
            check.setBackground(ring);
            check.setVisibility(View.VISIBLE);
            cell.setAlpha(1f);
        }
    }

    void refreshAllCellVisuals() {
        for (int i = 0; i < cells.size(); i++) refreshCellVisual(i);
        countView.setText("已选 " + getSelectedCount());
    }

    void resetActionBarVisibility() {
        actionBar.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
    }

    void setSelectionMode(boolean on) {
        this.selectionMode = on;
        if (!on) selected.clear();
        refreshAllCellVisuals();
        resetActionBarVisibility();
        if (listener != null) listener.onSelectionChanged(getSelectedCount(), selectionMode);
    }

    void selectAll() {
        setSelectionMode(true);
        selected.clear();
        for (int i = 0; i < itemCount; i++) selected.add(Integer.valueOf(i));
        refreshAllCellVisuals();
        if (listener != null) listener.onSelectionChanged(getSelectedCount(), selectionMode);
    }

    void invertSelection() {
        if (!selectionMode) return;
        List next = new ArrayList();
        for (int i = 0; i < itemCount; i++) {
            if (!selected.contains(Integer.valueOf(i))) next.add(Integer.valueOf(i));
        }
        selected.clear();
        selected.addAll(next);
        refreshAllCellVisuals();
        if (listener != null) listener.onSelectionChanged(getSelectedCount(), selectionMode);
    }

    void refreshItem(int index) {
        if (index < 0 || index >= imageViews.size()) return;
        if (binder != null) binder.bindImage(index, (ImageView) imageViews.get(index));
    }
}
