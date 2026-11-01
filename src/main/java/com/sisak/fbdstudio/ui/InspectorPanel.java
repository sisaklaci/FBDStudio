package com.sisak.fbdstudio.ui;

import com.sisak.fbdstudio.i18n.Messages;
import com.sisak.fbdstudio.library.BlockLibrary;
import com.sisak.fbdstudio.library.BlockTypeDefinition;
import com.sisak.fbdstudio.model.BlockInstance;
import com.sisak.fbdstudio.model.PinDefinition;
import com.sisak.fbdstudio.model.POU;
import com.sisak.fbdstudio.ui.commands.PropertyCommand;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/** Shows and edits the properties of the currently selected block instance. */
public class InspectorPanel extends VBox {

    private final BlockLibrary library = BlockLibrary.getInstance();
    private final UndoManager undo;
    private final Runnable onChanged;
    private POU pou;
    private BlockInstance current;

    private final Label header = new Label();
    private final Label titleLabel = new Label();
    private final GridPane grid = new GridPane();

    public InspectorPanel(UndoManager undo, Runnable onChanged) {
        this.undo = undo;
        this.onChanged = onChanged;
        setSpacing(8);
        setPadding(new Insets(8));
        grid.setHgap(6);
        grid.setVgap(6);
        header.setStyle("-fx-font-weight: bold;");
        titleLabel.setStyle("-fx-font-weight: bold;");
        getChildren().addAll(header, titleLabel, grid);

        retranslate();
        Messages.addListener(this::retranslate);
    }

    private void retranslate() {
        header.setText(Messages.get("inspector.title"));
        setSelection(current);
    }

    public void setPou(POU pou) {
        this.pou = pou;
    }

    public void setSelection(BlockInstance instance) {
        this.current = instance;
        grid.getChildren().clear();
        if (instance == null) {
            titleLabel.setText(Messages.get("inspector.noSelection"));
            return;
        }
        BlockTypeDefinition type = library.get(instance.getTypeId());
        if (type == null) {
            titleLabel.setText(instance.getTypeId() + " " + Messages.get("inspector.unknownType"));
            return;
        }
        titleLabel.setText(type.getDisplayName());

        int row = 0;
        grid.add(new Label(Messages.get("inspector.instanceName")), 0, row);
        TextField nameField = new TextField(instance.getInstanceName());
        nameField.setOnAction(e -> commitName(nameField.getText()));
        nameField.focusedProperty().addListener((o, was, is) -> {
            if (!is) commitName(nameField.getText());
        });
        grid.add(nameField, 1, row++);

        if (type.isVariadicInputs()) {
            grid.add(new Label(Messages.get("inspector.pinCount")), 0, row);
            int currentCount = instance.getExtraPinCount() > 0 ? instance.getExtraPinCount() : type.getMinInputs();
            Spinner<Integer> spinner = new Spinner<>();
            spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(type.getMinInputs(), type.getMaxInputs(), currentCount));
            spinner.setEditable(true);
            spinner.valueProperty().addListener((obs, oldV, newV) -> {
                int old = instance.getExtraPinCount() > 0 ? instance.getExtraPinCount() : type.getMinInputs();
                undo.perform(new PropertyCommand<>("Change pin count", old, newV, instance::setExtraPinCount, onChanged));
            });
            grid.add(spinner, 1, row++);
        }

        for (PinDefinition pin : type.getInputs()) {
            grid.add(new Label(pin.getName() + " (" + pin.getType().iecName() + "):"), 0, row);
            String currentLiteral = instance.getPinLiterals().getOrDefault(pin.getName(), pin.getDefaultLiteral());
            TextField field = new TextField(currentLiteral);
            field.setOnAction(e -> commitLiteral(pin.getName(), field.getText()));
            field.focusedProperty().addListener((o, was, is) -> {
                if (!is) commitLiteral(pin.getName(), field.getText());
            });
            grid.add(field, 1, row++);
        }
    }

    private void commitName(String newName) {
        if (current == null || newName.equals(current.getInstanceName())) return;
        String old = current.getInstanceName();
        undo.perform(new PropertyCommand<>("Rename instance", old, newName, current::setInstanceName, onChanged));
    }

    private void commitLiteral(String pinName, String newValue) {
        if (current == null) return;
        String old = current.getPinLiterals().get(pinName);
        if (newValue.equals(old)) return;
        undo.perform(new PropertyCommand<>("Set " + pinName, old, newValue,
                v -> current.getPinLiterals().put(pinName, v), onChanged));
    }
}
