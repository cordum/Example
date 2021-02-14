package ru.project.controller;

import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
//Deniska
@Controller
public class OpenProjectController implements Initializable {
    private final ApplicationContext context;

    public Dialog<Project> dialog;

    public TableView<ProjectLoader.PreviewProject> projectView;

    public ButtonType applyButtonType;
    public ButtonType createButtonType;
    public ButtonType removeButtonType;

    private ProjectLoader projectLoader;
    private ProjectLoader.PreviewProject selectedProject;

    public OpenProjectController(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        dialog.getDialogPane().lookupButton(applyButtonType).addEventFilter(ActionEvent.ACTION, this::onApply);
        dialog.getDialogPane().lookupButton(removeButtonType).addEventFilter(ActionEvent.ACTION, this::removeProject);
        dialog.getDialogPane().lookupButton(createButtonType).addEventFilter(ActionEvent.ACTION, this::createProject);
        dialog.setResultConverter(this::resultConverter);

        projectView.getSelectionModel().selectedItemProperty().addListener(this::selectedProjectChanged);
        projectView.setRowFactory(this::tableViewRowFactory);

        dialog.getDialogPane().lookupButton(removeButtonType).setDisable(true);

        projectLoader = new ProjectLoader();
        projectView.getItems().addAll(projectLoader.findAllProjects());
        projectView.getSelectionModel().selectFirst();
    }

    private TableRow<ProjectLoader.PreviewProject> tableViewRowFactory(TableView<ProjectLoader.PreviewProject> tableView) {
        TableRow<ProjectLoader.PreviewProject> row = new TableRow<>();

        row.setOnMouseClicked(event -> {
            if (row.isEmpty())
                return;

            if (event.getClickCount() < 2)
                return;

            ((Button) dialog.getDialogPane().lookupButton(applyButtonType)).fire();
        });

        return row;
    }

    private void selectedProjectChanged(ObservableValue observable, ProjectLoader.PreviewProject old, ProjectLoader.PreviewProject val) {
        selectedProject = val;
        dialog.getDialogPane().lookupButton(removeButtonType).setDisable(val == null);
    }

    private void removeProject(ActionEvent event) {
        event.consume();

        if (selectedProject == null)
            return;

        Alert alert = prepareAlert(
                Alert.AlertType.CONFIRMATION,
                "Deleting a project",
                "Do you really want to delete the project?",
                null,
                ButtonType.OK,
                new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE)
        );

        ButtonType result = alert.showAndWait().orElse(ButtonType.CANCEL);

        if (!result.getButtonData().isDefaultButton())
            return;

        try {
            projectLoader.removeProject(selectedProject);
            projectView.getItems().remove(selectedProject);
        } catch (IOException e) {
            e.printStackTrace();

            Alert error = prepareAlert(
                    Alert.AlertType.ERROR,
                    "Error",
                    "Project deletion error",
                    "The project was not deleted"
            );
            error.show();
        }
    }

    private void createProject(ActionEvent event) {
        event.consume();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/EditProject.fxml"));
            loader.setControllerFactory(context::getBean);
            Dialog<Project> dialog = loader.load();
            dialog.initOwner(this.dialog.getOwner());
            Project project = dialog.showAndWait().orElse(null);
            if (project == null)
                return;

            projectLoader.saveProject(project);
            projectView.getItems().clear();
            projectView.getItems().addAll(projectLoader.findAllProjects());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isValid() {
        return selectedProject != null;
    }


    private Project resultConverter(ButtonType buttonType) {
        if (!buttonType.getButtonData().isDefaultButton())
            return null;

        try {
            return projectLoader.loadProject(selectedProject);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void onApply(ActionEvent event) {
        if (!isValid())
            event.consume();
    }

    private Alert prepareAlert(Alert.AlertType type, String title, String header, String content, ButtonType... buttons) {
        Alert alert = new Alert(type, content, buttons);

        alert.initOwner(dialog.getOwner());

        alert.setTitle(title);
        alert.setHeaderText(header);

        return alert;
    }
}
