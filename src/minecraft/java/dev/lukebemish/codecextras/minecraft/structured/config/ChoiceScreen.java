package dev.lukebemish.codecextras.minecraft.structured.config;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

class ChoiceScreen extends Screen {
    private static final int KEYS_NEED_SEARCH = 10;

    private final Screen lastScreen;
    private final HeaderAndFooterLayout layout;
    private final Consumer<@Nullable String> onClose;
    private @Nullable EntryList list;
    private final List<String> keys;
    private @Nullable String selectedKey;
    private String filter = "";
    private final Button doneButton = Button.builder(CommonComponents.GUI_DONE, (button) -> this.onClose()).width(200).build();

    public ChoiceScreen(Screen screen, Component title, List<String> keys, @Nullable String selectedKey, Consumer<@Nullable String> onClose) {
        super(title);
        this.lastScreen = screen;
        this.keys = keys;
        this.selectedKey = selectedKey;
        this.onClose = onClose;
        if (keys.size() > KEYS_NEED_SEARCH) {
            layout = new HeaderAndFooterLayout(this, HeaderAndFooterLayout.DEFAULT_HEADER_AND_FOOTER_HEIGHT + Button.DEFAULT_HEIGHT + Button.DEFAULT_SPACING, HeaderAndFooterLayout.DEFAULT_HEADER_AND_FOOTER_HEIGHT);
        } else {
            layout = new HeaderAndFooterLayout(this);
        }
    }

    protected void init() {
        this.addHeader();

        this.list = new EntryList();
        this.addContents();
        this.layout.addToContents(this.list);
        this.updateButtonValidity();

        this.addFooter();
        this.layout.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        this.lastScreen.resize(this.minecraft, this.width, this.height);
        this.layout.arrangeElements();
        if (this.list != null) {
            this.list.updateSize(this.width, this.layout);
        }
    }

    public void added() {
        super.added();
    }

    protected void addHeader() {
        var title = new StringWidget(this.title, this.font);
        var layout = LinearLayout.vertical().spacing(Button.DEFAULT_SPACING);
        layout.addChild(title, LayoutSettings.defaults().alignVerticallyMiddle().alignHorizontallyCenter());
        if (keys.size() > KEYS_NEED_SEARCH) {
            var search = new EditBox(this.font, 0, 0, Button.DEFAULT_WIDTH, Button.DEFAULT_HEIGHT, Component.empty());
            search.setValue(this.filter);
            search.setResponder(string -> {
                if (this.list != null) {
                    this.filter = string;
                    this.list.clear();
                    this.addContents();
                    this.repositionElements();
                }
            });
            layout.addChild(search, LayoutSettings.defaults().alignVerticallyMiddle().alignHorizontallyCenter());
        }
        this.layout.addToHeader(layout);
    }

    protected void addContents() {
        this.keys.forEach(key -> {
            if (filter.isBlank() || key.contains(filter)) {
                this.list.addEntry(list.new Entry(key));
            }
        });
        this.list.setSelected(this.list.children().stream()
            .filter(entry -> filter.isBlank() || entry.key.contains(filter))
            .filter(entry -> Objects.equals(entry.key, this.selectedKey)).findFirst().orElse(null)
        );
    }

    protected void addFooter() {
        this.layout.addToFooter(this.doneButton);
    }

    public void onClose() {
        this.onClose.accept(this.selectedKey);
        this.minecraft.setScreen(this.lastScreen);
    }

    private final class EntryList extends ObjectSelectionList<ChoiceScreen.EntryList.Entry> {
        private EntryList() {
            super(ChoiceScreen.this.minecraft, ChoiceScreen.this.width, ChoiceScreen.this.layout.getContentHeight(), ChoiceScreen.this.layout.getHeaderHeight(), 16);
        }

        public void setSelected(EntryList.@Nullable Entry entry) {
            super.setSelected(entry);
            if (entry != null) {
                ChoiceScreen.this.selectedKey = entry.key;
            }

            ChoiceScreen.this.updateButtonValidity();
        }

        @Override
        public int addEntry(Entry entry) {
            return super.addEntry(entry);
        }

        public void clear() {
            this.clearEntries();
        }

        private class Entry extends ObjectSelectionList.Entry<ChoiceScreen.EntryList.Entry> {
            final String key;
            final Component name;

            public Entry(final String key) {
                this.key = key;
                this.name = Component.literal(key);
            }

            public Component getNarration() {
                return Component.translatable("narrator.select", this.name);
            }

            public void render(GuiGraphics guiGraphics, int i, int j, int k, int l, int m, int n, int o, boolean bl, float f) {
                guiGraphics.drawString(ChoiceScreen.this.font, this.name, k + 5, j + 2, 0xFFFFFF);
            }

            public boolean mouseClicked(double d, double e, int i) {
                ChoiceScreen.EntryList.this.setSelected(this);
                return super.mouseClicked(d, e, i);
            }
        }
    }

    private void updateButtonValidity() {
        this.doneButton.active = this.list.getSelected() != null;
    }
}
