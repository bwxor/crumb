module com.bwxor.piejfxsdk {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.bwxor.plugin;
    requires org.json;
    requires java.sql;
    requires anthropic.java.core;
    requires anthropic.java.client.okhttp;

    opens com.bwxor.piejfxsdk to javafx.fxml;
    exports com.bwxor.piejfxsdk;
}