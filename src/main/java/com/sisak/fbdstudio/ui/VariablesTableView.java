package com.sisak.fbdstudio.ui;

import com.sisak.fbdstudio.i18n.Messages;
import com.sisak.fbdstudio.model.DataType;
import com.sisak.fbdstudio.model.POU;
import com.sisak.fbdstudio.model.Variable;
import com.sisak.fbdstudio.model.VariableKind;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.converter.DefaultStringConverter;

/** Editable table of a POU's variable declarations (VAR_INPUT/OUTPUT/IN_OUT/VAR/VAR_TEMP). */
public class VariablesTableView extends VBox {

    private final Label title = new Label();
    private final TableView<Variable> table = new TableView<>();
    private final TableColumn<Variable, String> nameCol = new TableColumn<>();
    private final TableColumn<Variable, VariableKind> kindCol = new TableColumn<>();
    private final TableColumn<Variable, DataType> typeCol = new TableColumn<>();
    private final TableColumn<Variable, String> initCol = new TableColumn<>();
    private final TableColumn<Variable, String> commentCol = new TableColumn<>();
    private final Button add = new Button();
    private final Button remove = new Button();
    private final Runnable onChanged;
    private POU pou;

    public VariablesTableView(Runnable onChanged) {
        this.onChanged = onChanged;
        setSpacing(6);
        setPadding(new Insets(6));
        table.setEditable(true);
        table.setPrefHeight(220);

        nameCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getName()));
        nameCol.setCellFactory(TextFieldTableCell.forTableColumn(new DefaultStringConverter()));
        nameCol.setOnEditCommit(e -> {
            e.getRowValue().setName(e.getNewValue());
            onChanged.run();
        });

        kindCol.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getKind()));
        kindCol.setCellFactory(ComboBoxTableCell.forTableColumn(FXCollections.observableArrayList(VariableKind.values())));
        kindCol.setOnEditCommit(e -> {
            e.getRowValue().setKind(e.getNewValue());
            onChanged.run();
        });

        typeCol.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getType()));
        typeCol.setCellFactory(ComboBoxTableCell.forTableColumn(FXCollections.observableArrayList(DataType.values())));
        typeCol.setOnEditCommit(e -> {
            e.getRowValue().setType(e.getNewValue());
            onChanged.run();
        });

        initCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getInitialValue()));
        initCol.setCellFactory(TextFieldTableCell.forTableColumn(new DefaultStringConverter()));
        initCol.setOnEditCommit(e -> {
            e.getRowValue().setInitialValue(e.getNewValue());
            onChanged.run();
        });

        commentCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getComment()));
        commentCol.setCellFactory(TextFieldTableCell.forTableColumn(new DefaultStringConverter()));
        commentCol.setOnEditCommit(e -> {
            e.getRowValue().setComment(e.getNewValue());
            onChanged.run();
        });

        table.getColumns().addAll(nameCol, kindCol, typeCol, initCol, commentCol);

        add.setOnAction(e -> {
            if (pou == null) return;
            Variable v = new Variable("NEW_VAR" + (pou.getVariables().size() + 1), DataType.BOOL, VariableKind.VAR);
            pou.getVariables().add(v);
            refresh();
            onChanged.run();
        });
        remove.setOnAction(e -> {
            Variable sel = table.getSelectionModel().getSelectedItem();
            if (sel != null && pou != null) {
                pou.getVariables().remove(sel);
                refresh();
                onChanged.run();
            }
        });
        HBox buttons = new HBox(6, add, remove);

        getChildren().addAll(title, table, buttons);

        retranslate();
        Messages.addListener(this::retranslate);
    }

    private void retranslate() {
        title.setText(Messages.get("variables.title"));
        title.setStyle("-fx-font-weight: bold;");
        nameCol.setText(Messages.get("variables.col.name"));
        kindCol.setText(Messages.get("variables.col.kind"));
        typeCol.setText(Messages.get("variables.col.type"));
        initCol.setText(Messages.get("variables.col.initial"));
        commentCol.setText(Messages.get("variables.col.comment"));
        add.setText(Messages.get("variables.add"));
        remove.setText(Messages.get("variables.remove"));
    }

    public void setPou(POU pou) {
        this.pou = pou;
        refresh();
    }

    public void refresh() {
        ObservableList<Variable> items = pou == null ? FXCollections.observableArrayList()
                : FXCollections.observableArrayList(pou.getVariables());
        table.setItems(items);
    }
}
