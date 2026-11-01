package com.sisak.fbdstudio.ui;

import com.sisak.fbdstudio.i18n.Messages;
import com.sisak.fbdstudio.library.BlockCategory;
import com.sisak.fbdstudio.library.BlockLibrary;
import com.sisak.fbdstudio.library.BlockTypeDefinition;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TitledPane;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;

import java.util.List;

/** Searchable, categorized palette of draggable block types. */
public class BlockPaletteView extends VBox {

    private final Label title = new Label();
    private final TextField search = new TextField();
    private final VBox categoriesBox = new VBox(4);
    private final BlockLibrary library = BlockLibrary.getInstance();

    public BlockPaletteView() {
        setSpacing(6);
        setPadding(new Insets(6));

        search.textProperty().addListener((obs, old, val) -> rebuild(val));

        ScrollPane scroll = new ScrollPane(categoriesBox);
        scroll.setFitToWidth(true);
        scroll.setPrefWidth(230);

        getChildren().addAll(title, search, scroll);
        retranslate();

        Messages.addListener(this::retranslate);
    }

    private void retranslate() {
        title.setText(Messages.get("palette.title"));
        search.setPromptText(Messages.get("palette.search"));
        rebuild(search.getText());
    }

    private void rebuild(String filter) {
        categoriesBox.getChildren().clear();
        String f = filter == null ? "" : filter.trim().toLowerCase();
        for (BlockCategory category : BlockCategory.values()) {
            if (category == BlockCategory.USER) continue;
            List<BlockTypeDefinition> defs = library.byCategory(category).stream()
                    .filter(d -> f.isEmpty() || d.getId().toLowerCase().contains(f)
                            || d.getDisplayName().toLowerCase().contains(f))
                    .toList();
            if (defs.isEmpty()) continue;

            VBox list = new VBox(3);
            for (BlockTypeDefinition def : defs) {
                list.getChildren().add(blockRow(def));
            }
            TitledPane pane = new TitledPane(category.displayName() + (category == BlockCategory.EXTENDED ? "" : " (IEC)"), list);
            pane.setExpanded(!f.isEmpty());
            categoriesBox.getChildren().add(pane);
        }
    }

    private javafx.scene.Node blockRow(BlockTypeDefinition def) {
        Label label = new Label(def.getDisplayName());
        label.setMaxWidth(Double.MAX_VALUE);
        label.setPadding(new Insets(4, 6, 4, 6));
        label.setStyle("-fx-background-color: #f1f4f9; -fx-background-radius: 4; -fx-border-color: #c6cdd8; -fx-border-radius: 4;");
        Tooltip.install(label, new Tooltip(def.getDescription()));
        label.setOnDragDetected(e -> {
            javafx.scene.input.Dragboard db = label.startDragAndDrop(TransferMode.COPY);
            ClipboardContent content = new ClipboardContent();
            content.putString(def.getId());
            db.setContent(content);
            e.consume();
        });
        return label;
    }
}
