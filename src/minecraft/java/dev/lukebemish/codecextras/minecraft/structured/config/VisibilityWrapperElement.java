package dev.lukebemish.codecextras.minecraft.structured.config;

import java.util.IdentityHashMap;
import java.util.function.Consumer;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutElement;

public interface VisibilityWrapperElement extends Layout {
    void setVisible(boolean visible);
    boolean visible();

    void setActive(boolean visible);
    boolean active();

    static VisibilityWrapperElement ofInactive(LayoutElement child) {
        return new VisibilityWrapperElement() {
            private boolean visible = true;
            private boolean active = true;

            private final IdentityHashMap<LayoutElement, Boolean> isVisible = new IdentityHashMap<>();

            private void visitChildren(Consumer<AbstractWidget> widgetConsumer, Consumer<VisibilityWrapperElement> wrapperConsumer) {
                switch (child) {
                    case VisibilityWrapperElement wrapperElement -> wrapperConsumer.accept(wrapperElement);
                    case Layout layout -> layout.visitChildren(element -> visitChildren(widgetConsumer, wrapperConsumer));
                    case AbstractWidget widget -> widgetConsumer.accept(widget);
                    default -> {}
                }
            }

            {
                visitWidgets(widget -> {
                    widget.active = false;
                });
            }

            @Override
            public void setX(int i) {
                child.setX(i);
            }

            @Override
            public void setY(int i) {
                child.setY(i);
            }

            @Override
            public int getX() {
                return child.getX();
            }

            @Override
            public int getY() {
                return child.getY();
            }

            @Override
            public int getWidth() {
                return child.getWidth();
            }

            @Override
            public int getHeight() {
                return child.getHeight();
            }

            @Override
            public void visitChildren(Consumer<LayoutElement> consumer) {
                consumer.accept(child);
            }

            @Override
            public void visitWidgets(Consumer<AbstractWidget> consumer) {
                child.visitWidgets(consumer);
            }

            @Override
            public void setVisible(boolean visible) {
                if (visible) {
                    isVisible.forEach((element, wasVisible) -> {
                        if (element instanceof VisibilityWrapperElement wrapper) {
                            wrapper.setVisible(wasVisible);
                        } else if (element instanceof AbstractWidget widget) {
                            widget.visible = wasVisible;
                        }
                    });
                } else {
                    isVisible.clear();
                    visitChildren(widget -> {
                        isVisible.put(widget, widget.visible);
                        widget.visible = false;
                    }, wrapper -> {
                        isVisible.put(wrapper, wrapper.visible());
                        wrapper.setVisible(false);
                    });
                }
                this.visible = visible;
            }

            @Override
            public boolean visible() {
                return visible;
            }

            @Override
            public void setActive(boolean visible) {
                this.active = visible;
            }

            @Override
            public boolean active() {
                return active;
            }
        };
    }

    static VisibilityWrapperElement ofDirect(LayoutElement child) {
        return new VisibilityWrapperElement() {
            private boolean visible = true;
            private boolean active = true;

            private final IdentityHashMap<LayoutElement, Boolean> isVisible = new IdentityHashMap<>();
            private final IdentityHashMap<LayoutElement, Boolean> isActive = new IdentityHashMap<>();

            private void visitChildren(Consumer<AbstractWidget> widgetConsumer, Consumer<VisibilityWrapperElement> wrapperConsumer) {
                switch (child) {
                    case VisibilityWrapperElement wrapperElement -> wrapperConsumer.accept(wrapperElement);
                    case Layout layout -> layout.visitChildren(element -> visitChildren(widgetConsumer, wrapperConsumer));
                    case AbstractWidget widget -> widgetConsumer.accept(widget);
                    default -> {}
                }
            }

            @Override
            public void setX(int i) {
                child.setX(i);
            }

            @Override
            public void setY(int i) {
                child.setY(i);
            }

            @Override
            public int getX() {
                return child.getX();
            }

            @Override
            public int getY() {
                return child.getY();
            }

            @Override
            public int getWidth() {
                return child.getWidth();
            }

            @Override
            public int getHeight() {
                return child.getHeight();
            }

            @Override
            public void visitChildren(Consumer<LayoutElement> consumer) {
                consumer.accept(child);
            }

            @Override
            public void visitWidgets(Consumer<AbstractWidget> consumer) {
                child.visitWidgets(consumer);
            }

            @Override
            public void setVisible(boolean visible) {
                if (visible) {
                    isVisible.forEach((element, wasVisible) -> {
                        if (element instanceof VisibilityWrapperElement wrapper) {
                            wrapper.setVisible(wasVisible);
                        } else if (element instanceof AbstractWidget widget) {
                            widget.visible = wasVisible;
                        }
                    });
                } else {
                    isVisible.clear();
                    visitChildren(widget -> {
                        isVisible.put(widget, widget.visible);
                        widget.visible = false;
                    }, wrapper -> {
                        isVisible.put(wrapper, wrapper.visible());
                        wrapper.setVisible(false);
                    });
                }
                this.visible = visible;
            }

            @Override
            public boolean visible() {
                return visible;
            }

            @Override
            public void setActive(boolean active) {
                if (active) {
                    isActive.forEach((element, wasVisible) -> {
                        if (element instanceof VisibilityWrapperElement wrapper) {
                            wrapper.setActive(wasVisible);
                        } else if (element instanceof AbstractWidget widget) {
                            widget.active = wasVisible;
                        }
                    });
                } else {
                    isActive.clear();
                    visitChildren(widget -> {
                        isActive.put(widget, widget.active);
                        widget.active = false;
                    }, wrapper -> {
                        isActive.put(wrapper, wrapper.active());
                        wrapper.setActive(false);
                    });
                }
                this.active = active;
            }

            @Override
            public boolean active() {
                return active;
            }
        };
    }
}
