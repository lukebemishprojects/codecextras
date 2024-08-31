package dev.lukebemish.codecextras.minecraft.structured.config;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.EqualSpacingLayout;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

abstract class EntryListScreen extends Screen {
    protected final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    protected final Screen lastScreen;
    protected @Nullable EntryList list;

    public EntryListScreen(Screen screen, Component title) {
        super(title);
        this.lastScreen = screen;
    }

    protected void init() {
        this.addTitle();
        this.addContents();
        this.addFooter();
        this.layout.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
    }

    protected void addTitle() {
        this.layout.addTitleHeader(this.title, this.font);
    }

    protected void addContents() {
        if (this.list == null) {
            this.list = new EntryList(this.width);
        } else {
            this.list.clear();
        }
        this.layout.addToContents(this.list);
        this.addEntries();
    }

    protected void addFooter() {
        this.layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, (button) -> this.onClose()).width(200).build());
    }

    protected void repositionElements() {
        this.layout.arrangeElements();
        if (this.list != null) {
            this.list.updateSize(this.width, this.layout);
        }
    }

    protected abstract void onExit();

    public void onClose() {
        this.onExit();
        this.minecraft.setScreen(this.lastScreen);
    }

    protected abstract void addEntries();

    protected final class EntryList extends ContainerObjectSelectionList<Entry> {
        public EntryList(int i) {
            super(EntryListScreen.this.minecraft, i, EntryListScreen.this.layout.getContentHeight(), EntryListScreen.this.layout.getHeaderHeight(), 25);
        }

        @Override
        public int getRowWidth() {
            return 310;
        }

        public void addPair(LayoutElement left, LayoutElement right) {
            var layout = new EqualSpacingLayout(Button.DEFAULT_WIDTH*2+EntryListScreen.Entry.SPACING, 0, EqualSpacingLayout.Orientation.HORIZONTAL);
            var leftLayout = new FrameLayout(Button.DEFAULT_WIDTH, 0);
            leftLayout.addChild(left, LayoutSettings.defaults().alignVerticallyMiddle().alignHorizontallyCenter());
            layout.addChild(leftLayout, LayoutSettings.defaults().alignVerticallyMiddle());
            var rightLayout = new FrameLayout(Button.DEFAULT_WIDTH, 0);
            rightLayout.addChild(right, LayoutSettings.defaults().alignVerticallyMiddle().alignHorizontallyCenter());
            layout.addChild(rightLayout, LayoutSettings.defaults().alignVerticallyMiddle());
            this.addEntry(new EntryListScreen.Entry(layout, EntryListScreen.this));
        }

        public void addSingle(LayoutElement layoutElement) {
            var layout = new FrameLayout(Button.DEFAULT_WIDTH*2+EntryListScreen.Entry.SPACING, 0);
            layout.addChild(layoutElement, LayoutSettings.defaults().alignVerticallyMiddle().alignHorizontallyCenter());
            this.addEntry(new EntryListScreen.Entry(layout, EntryListScreen.this));
        }

        @Override
        public void updateSize(int i, HeaderAndFooterLayout headerAndFooterLayout) {
            super.updateSize(i, headerAndFooterLayout);
            for (var entry : this.children()) {
                entry.layout.arrangeElements();
            }
        }

        public void clear() {
            super.clearEntries();
        }
    }

    protected static final class Entry extends ContainerObjectSelectionList.Entry<Entry> {
        private final Layout layout;
        private final List<? extends NarratableEntry> narratables;
        private final List<? extends GuiEventListener> listeners;
        private final Screen screen;

        public static final int SPACING = 10;

        private Entry(Layout layout, Screen screen) {
            this.layout = layout;
            List<AbstractWidget> childWidgets = new ArrayList<>();
            layout.visitWidgets(childWidgets::add);
            this.narratables = childWidgets;
            this.listeners = childWidgets;
            this.screen = screen;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return this.narratables;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int i, int j, int k, int l, int m, int n, int o, boolean bl, float f) {
            int q = this.screen.width / 2 - (Button.DEFAULT_WIDTH + SPACING / 2);

            layout.setPosition(q, j);
            layout.visitWidgets((widget) -> widget.render(guiGraphics, n, o, f));
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return this.listeners;
        }
    }
}
