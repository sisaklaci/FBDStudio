package com.sisak.fbdstudio.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sisak.fbdstudio.i18n.Messages;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Optional;

/** Lists the bundled example projects (resources/examples/manifest.json) for File > Open Example. */
public final class ExampleGalleryDialog {

    private static final double DIALOG_WIDTH = 760;
    private static final double DIALOG_HEIGHT = 480;

    public record Entry(String file, String title, String description) {
    }

    private ExampleGalleryDialog() {
    }

    public static Optional<URL> chooseExample(Stage owner) {
        List<Entry> entries = loadManifest();
        if (entries.isEmpty()) {
            return Optional.empty();
        }
        Dialog<Entry> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle(Messages.get("examples.dialog.title"));
        dialog.setResizable(true);
        dialog.getDialogPane().setPrefSize(DIALOG_WIDTH, DIALOG_HEIGHT);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ListView<Entry> list = new ListView<>();
        list.setPrefSize(DIALOG_WIDTH - 40, DIALOG_HEIGHT - 80);
        list.getItems().addAll(entries);
        list.setCellFactory(v -> new ListCell<>() {
            @Override
            protected void updateItem(Entry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label title = new Label(localizedTitle(item));
                    title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
                    Label desc = new Label(localizedDescription(item));
                    desc.setWrapText(true);
                    desc.setMaxWidth(DIALOG_WIDTH - 90);
                    VBox box = new VBox(3, title, desc);
                    box.setMaxWidth(Double.MAX_VALUE);
                    setPrefWidth(0); // let the ListView own the width instead of the widest cell
                    setGraphic(box);
                }
            }
        });
        list.getSelectionModel().selectFirst();
        dialog.getDialogPane().setContent(list);
        dialog.setResultConverter(bt -> bt == ButtonType.OK ? list.getSelectionModel().getSelectedItem() : null);

        return dialog.showAndWait()
                .map(entry -> ExampleGalleryDialog.class.getResource("/examples/" + entry.file()));
    }

    /**
     * Looked up live from the current language ({@code example.<id>.title/description}, where
     * {@code <id>} is the file name without its {@code .fbdproj} extension); falls back to the
     * English text from manifest.json if a translation is ever missing for the current locale.
     */
    private static String localizedTitle(Entry entry) {
        return localized("example." + idOf(entry) + ".title", entry.title());
    }

    private static String localizedDescription(Entry entry) {
        return localized("example." + idOf(entry) + ".description", entry.description());
    }

    private static String idOf(Entry entry) {
        String f = entry.file();
        return f.endsWith(".fbdproj") ? f.substring(0, f.length() - ".fbdproj".length()) : f;
    }

    private static String localized(String key, String fallback) {
        String translated = Messages.get(key);
        return translated.equals(key) ? fallback : translated;
    }

    private static List<Entry> loadManifest() {
        try (InputStream in = ExampleGalleryDialog.class.getResourceAsStream("/examples/manifest.json")) {
            if (in == null) return List.of();
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(in, mapper.getTypeFactory().constructCollectionType(List.class, Entry.class));
        } catch (IOException e) {
            return List.of();
        }
    }
}
