package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import java.util.ArrayList;
import java.util.HashMap;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;

public class Controller implements Initializable {

@FXML
ListView<String> chatList;
@FXML
ListView<String> chatHint;
@FXML
ListView<Message> chatContentList;
@FXML
TextArea inputArea;
@FXML
Label currentUsername;
@FXML
Label currentOnlineCnt;
HashMap<String, ObservableList<Message>> content = new HashMap<>();
ArrayList<String> onlineUsers;
String username;
ArrayList<AtomicReference<String>> chatTo;

String currentGroupName;
ObservableList<String> userList = FXCollections.observableArrayList();
ObservableList<String> userHint = FXCollections.observableArrayList();
private Client client;

@Override
public void initialize(URL url, ResourceBundle resourceBundle) {

    client = new Client(this);
    onlineUsers = new ArrayList<>();
    chatList.setItems(userList);
    chatHint.setItems(userHint);
    chatContentList.setCellFactory(new MessageCellFactory());

    //add listener to chatList
    chatList.getSelectionModel().selectedItemProperty().addListener(
        (observableValue, oldValue, newValue) -> {
            String[] chat = newValue.split(",");
            ArrayList<AtomicReference<String>> to = new ArrayList<>();
            for (String temp : chat) {
                to.add(new AtomicReference<>(temp));
            }
            currentGroupName = newValue;
            chatTo = to;
            chatContentList.setItems(content.get(newValue));

            userHint.set(userList.indexOf(newValue),"");
        });

    Dialog<String> dialog = new TextInputDialog();
    dialog.setTitle("Login");
    dialog.setHeaderText(null);
    dialog.setContentText("Username:");

    Optional<String> input = dialog.showAndWait();

    if (input.isPresent() && !input.get().isEmpty()) {
        username = input.get();
        client.getClientThread().connect();

        //stop for a moment so that the client can finish its work
        try {
            Thread.sleep(80);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (client.getClientThread().isConnected) {
            currentUsername.setText("Current User: " + username);
        } else {
            alertMessage("username exists or invalid username");
            Platform.exit();
            client.getClientThread().stop = false;
        }
    } else {
        alertMessage("username exists or invalid username");
        Platform.exit();
        client.getClientThread().stop = false;
    }
}

//maintain the current the number of online user
public void setCurrentOnlineCntText(int num) {
    currentOnlineCnt.setText("Online: " + num);
}

//create a private chat
@FXML
public void createPrivateChat() {
    AtomicReference<String> user = new AtomicReference<>();
    Stage stage = new Stage();
    ComboBox<String> userSel = new ComboBox<>();
    userSel.getItems().addAll(onlineUsers);

    Button okBtn = new Button("OK");
    okBtn.setOnAction(e -> {
        user.set(userSel.getSelectionModel().getSelectedItem());
        stage.close();

        chatTo = new ArrayList<>();
        chatTo.add(user);
    });

    HBox box = new HBox(10);
    box.setAlignment(Pos.CENTER);
    box.setPadding(new Insets(20, 20, 20, 20));
    box.getChildren().addAll(userSel, okBtn);
    stage.setScene(new Scene(box));
    stage.showAndWait();

    // if the current user already chatted with the selected user, just open the chat with that user
    // otherwise, create a new chat item in the left panel, the title should be the selected user's name
    if (user.get() != null) {
        currentGroupName = user.get() + "," + username;
        if (userList.contains(currentGroupName)) {
            chatContentList.setItems(content.get(currentGroupName));
        } else {
            ObservableList<Message> message = FXCollections.observableArrayList();
            content.put(currentGroupName, message);
            chatContentList.setItems(message);

            ArrayList<String> sendTo = new ArrayList<>();
            chatTo.forEach(o -> sendTo.add(o.get()));
            userList.add(currentGroupName);
            userHint.add("");
            client.getClientThread().sendCreateChat(sendTo,currentGroupName);
        }
    }
}

// add name to the memberList
public void createChat(String groupName) {
    if (!userList.contains(groupName)) {
        userList.add(groupName);
        userHint.add("");

        ObservableList<Message> message = FXCollections.observableArrayList();
        content.put(groupName, message);
    }
}

// maintain the onlineUserList
public void setOnlineUsers(ArrayList<String> onlineUsers) {
    this.onlineUsers = onlineUsers;
}

// create a group chat
@FXML
public void createGroupChat() {
    ArrayList<people> user = new ArrayList<>();
    Stage stage = new Stage();

    TableView<people> userSel = new TableView<>();

    TableColumn<people, CheckBox> select = new TableColumn<>("select");
    TableColumn<people, String> name = new TableColumn<>("name");
    userSel.getColumns().add(select);
    userSel.getColumns().add(name);
    name.setCellValueFactory(
        cellData -> new SimpleStringProperty(cellData.getValue().peopleName));
    select.setCellValueFactory(cellData -> cellData.getValue().checkbox.getCheckBox());

    ObservableList<people> temp = FXCollections.observableArrayList();
    userSel.setItems(temp);

    onlineUsers.forEach(o -> {
        people people = new people(o);
        user.add(people);
        temp.add(people);
    });

    Button okBtn = new Button("OK");
    okBtn.setOnAction(e -> {
        stage.close();

        chatTo = new ArrayList<>();
        user.forEach(o -> {
            if (o.checkbox.isSelected()) {
                chatTo.add(new AtomicReference<>(o.peopleName));
            }
        });
    });

    HBox box = new HBox(10);
    box.setAlignment(Pos.CENTER);
    box.setPadding(new Insets(20, 20, 20, 20));
    box.getChildren().addAll(userSel, okBtn);
    stage.setScene(new Scene(box));
    stage.showAndWait();

    if (chatTo.size() < 2) {
        alertMessage("number of people is not enough");
    } else {
        StringBuffer groupName = new StringBuffer();
        chatTo.forEach(o -> groupName.append(o.get()).append(","));
        groupName.append(username);
        currentGroupName = groupName.toString();
        if (userList.contains(groupName.toString())) {
            chatContentList.setItems(content.get(groupName.toString()));
        } else {
            ObservableList<Message> message = FXCollections.observableArrayList();
            content.put(groupName.toString(), message);
            chatContentList.setItems(message);

            ArrayList<String> sendTo = new ArrayList<>();
            chatTo.forEach(o -> sendTo.add(o.get()));
            userList.add(groupName.toString());
            userHint.add("");
            client.getClientThread().sendCreateChat(sendTo, groupName.toString());
        }
    }
}

// send message to another
@FXML
public void doSendMessage() {
    String data = inputArea.getText();
    if (!data.isEmpty()) {
        ArrayList<String> sendTo = new ArrayList<>();
        chatTo.forEach(o -> sendTo.add(o.get()));

        client.getClientThread().sendMSG(username, sendTo, data, currentGroupName);
        inputArea.clear();

        Message message = new Message();
        message.setSentBy(username);
        message.setSendTo(sendTo);
        message.setData(data);
        message.setGroupName(currentGroupName);

        chatContentList.getItems().add(message);
    }
}

// Add new message into the listView
public void addMessage(Message message) {
    content.get(message.getGroupName()).add(message);

    if (!message.getGroupName().equals(currentGroupName)) userHint.set(userList.indexOf(message.getGroupName()),"!!!!");
}

// Show message of server close
public void alertMessage(String warning) {
    Alert alert = new Alert(AlertType.WARNING);
    alert.setContentText(warning);
    alert.showAndWait();
}

// Message Factory
private class MessageCellFactory implements Callback<ListView<Message>, ListCell<Message>> {

    @Override
    public ListCell<Message> call(ListView<Message> param) {
        return new ListCell<Message>() {

            @Override
            public void updateItem(Message msg, boolean empty) {
                super.updateItem(msg, empty);
                if (empty || Objects.isNull(msg)) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                HBox wrapper = new HBox();
                Label nameLabel = new Label(msg.getSentBy());
                Label msgLabel = new Label(msg.getData());

                nameLabel.setPrefSize(50, 20);
                nameLabel.setWrapText(true);
                nameLabel.setStyle("-fx-border-color: black; -fx-border-width: 1px;");

                if (username.equals(msg.getSentBy())) {
                    wrapper.setAlignment(Pos.TOP_RIGHT);
                    wrapper.getChildren().addAll(msgLabel, nameLabel);
                    msgLabel.setPadding(new Insets(0, 20, 0, 0));
                } else {
                    wrapper.setAlignment(Pos.TOP_LEFT);
                    wrapper.getChildren().addAll(nameLabel, msgLabel);
                    msgLabel.setPadding(new Insets(0, 0, 0, 20));
                }

                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                setGraphic(wrapper);
            }
        };
    }
}
}

class people {

checkbox checkbox;
String peopleName;

public people(String peopleName) {
    this.peopleName = peopleName;
    checkbox = new checkbox();
}
}

class checkbox {

CheckBox checkBox = new CheckBox();

public ObservableValue<CheckBox> getCheckBox() {
    return new ObservableValue<CheckBox>() {
        @Override
        public void addListener(ChangeListener<? super CheckBox> changeListener) {

        }

        @Override
        public void removeListener(ChangeListener<? super CheckBox> changeListener) {

        }

        @Override
        public CheckBox getValue() {
            return checkBox;
        }

        @Override
        public void addListener(InvalidationListener invalidationListener) {

        }

        @Override
        public void removeListener(InvalidationListener invalidationListener) {

        }
    };
}

public boolean isSelected() {
    return checkBox.isSelected();
}
}