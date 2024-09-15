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
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

class EntryListScreen extends Screen {
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private final Screen lastScreen;
    private @Nullable EntryList list;
    private final ScreenEntryProvider screenEntries;
    private final EntryCreationContext context;

    public EntryListScreen(Screen screen, Component title, ScreenEntryProvider screenEntries, EntryCreationContext context) {
        super(title);
        this.lastScreen = screen;
        this.screenEntries = screenEntries;
        this.context = context;
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

    public void onClose() {
        this.screenEntries.onExit(this.context);
        var problems = this.context.problems;
        if (!problems.isEmpty()) {
            var issues = String.join("\n", problems.values());
            var screen = new ConfirmScreen(bl -> {
                // Resolve everything in either case
                for (var problem : problems.keySet().stream().toList()) {
                    this.context.resolve(problem);
                }
                if (bl) {
                    this.minecraft.setScreen(this.lastScreen);
                } else {
                    this.minecraft.setScreen(this);
                }
            }, Component.translatable("codecextras.config.issue"), Component.translatable("codecextras.config.issue.message", issues));
            this.minecraft.setScreen(screen);
            return;
        }
        this.minecraft.setScreen(this.lastScreen);
    }

    protected void addEntries() {
        this.screenEntries.addEntries(this.list, this::rebuildWidgets, this);
    }

    final class EntryList extends ContainerObjectSelectionList<Entry> implements ScreenEntryList {
        public EntryList(int i) {
            super(EntryListScreen.this.minecraft, i, EntryListScreen.this.layout.getContentHeight(), EntryListScreen.this.layout.getHeaderHeight(), 25);
        }

        @Override
        public int getRowWidth() {
            return 310;
        }

        @Override
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

        @Override
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

        void clear() {
            super.clearEntries();
        }
    }

    static final class Entry extends ContainerObjectSelectionList.Entry<Entry> {
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
