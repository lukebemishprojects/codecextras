package dev.lukebemish.codecextras.minecraft.structured.config;

import com.ibm.icu.text.Collator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

class DispatchPickScreen extends Screen {
    private final Screen lastScreen;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private final Consumer<@Nullable String> onClose;
    private @Nullable EntryList list;
    private final List<String> keys;
    private @Nullable String selectedKey;
    private final Button doneButton = Button.builder(CommonComponents.GUI_DONE, (button) -> this.onClose()).width(200).build();

    public DispatchPickScreen(Screen screen, Component title, List<String> keys, @Nullable String selectedKey, Consumer<@Nullable String> onClose) {
        super(title);
        this.lastScreen = screen;
        this.keys = keys;
        this.selectedKey = selectedKey;
        this.onClose = onClose;
    }

    protected void init() {
        this.addTitle();
        this.addContents();
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

    protected void addTitle() {
        this.layout.addTitleHeader(this.title, this.font);
    }

    protected void addContents() {
        this.list = new EntryList();
        this.list.setSelected(this.list.children().stream().filter((entry) -> Objects.equals(entry.key, this.selectedKey)).findFirst().orElse(null));
        this.layout.addToContents(this.list);
        this.updateButtonValidity();
    }

    protected void addFooter() {
        this.layout.addToFooter(this.doneButton);
    }

    public void onClose() {
        this.onClose.accept(this.selectedKey);
        this.minecraft.setScreen(this.lastScreen);
    }

    private final class EntryList extends ObjectSelectionList<DispatchPickScreen.EntryList.Entry> {
        private EntryList() {
            super(DispatchPickScreen.this.minecraft, DispatchPickScreen.this.width, DispatchPickScreen.this.layout.getContentHeight(), DispatchPickScreen.this.layout.getHeaderHeight(), 16);
            Collator collator = Collator.getInstance(Locale.getDefault());
            DispatchPickScreen.this.keys.forEach(key -> {
                this.addEntry(new Entry(key));
            });
        }

        public void setSelected(EntryList.@Nullable Entry entry) {
            super.setSelected(entry);
            if (entry != null) {
                DispatchPickScreen.this.selectedKey = entry.key;
            }

            DispatchPickScreen.this.updateButtonValidity();
        }

        private class Entry extends ObjectSelectionList.Entry<DispatchPickScreen.EntryList.Entry> {
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
                guiGraphics.drawString(DispatchPickScreen.this.font, this.name, k + 5, j + 2, 0xFFFFFF);
            }

            public boolean mouseClicked(double d, double e, int i) {
                DispatchPickScreen.EntryList.this.setSelected(this);
                return super.mouseClicked(d, e, i);
            }
        }
    }

    private void updateButtonValidity() {
        this.doneButton.active = this.list.getSelected() != null;
    }
}
