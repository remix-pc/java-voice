module com.gui.demo {
    // Módulos do JavaFX que sua aplicação usa
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.swing;
    requires javafx.media;

    // Módulos de outras dependências
    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires com.almasb.fxgl.all;


    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires java.desktop;

    // Abre seu pacote para o JavaFX poder acessar os arquivos FXML
    opens com.gui.demo to javafx.fxml;

    // Exporta seu pacote principal
    exports com.gui.demo;
}