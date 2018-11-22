package org.khanhtd.javafx;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.khanhtd.javafx.controller.StageSceneController;

import java.net.URL;

public final class FXHelper {
    static {
        try {
            setupFX();
        } catch (Exception ignore) {
        
        }
    }
    
    public static void setupFX() {
        Platform.startup(() -> {
        });
    }
    
    public static void destroyFX() {
        try {
            Platform.exit();
        } catch (Exception e) {
            System.err.println("FXHelper: error destroyFX");
            e.printStackTrace();
        }
    }
    
    public static void setImplicitExit(boolean implicitExit) {
        try {
            Platform.setImplicitExit(implicitExit);
        } catch (Exception e) {
            System.err.println("FXHelper: error set implicit Exit");
            e.printStackTrace();
        }
    }
    
    public static boolean isFXThread() {
        try {
            return Platform.isFxApplicationThread();
        } catch (Exception e) {
            return false;
        }
    }
    
    
    public static FXMLLoadTask loadFXML(URL xmlFile) {
        return new FXMLLoadTask(xmlFile, false);
    }
    
    public static CreateWindowTask createWindow(URL fxmlFile) {
        return new CreateWindowTask(fxmlFile, false);
    }
    
    public static OpenWindowTask openWindow(URL fxmlFile) {
        return new OpenWindowTask(fxmlFile, false);
    }
    
    public static FXMLLoadTask loadFXMLAsync(URL xmlFile) {
        return new FXMLLoadTask(xmlFile, true);
    }
    
    public static CreateWindowTask createWindowAsync(URL fxmlFile) {
        return new CreateWindowTask(fxmlFile, true);
    }
    
    public static OpenWindowTask openWindowAsync(URL fxmlFile) {
        return new OpenWindowTask(fxmlFile, true);
    }
    
    
    public static class FXMLLoadTask {
        private boolean async;
        private URL fxmlFile;
        private Parent root = null;
        private Object controller = null;
        private FXMLLoadedCallback onLoaded = null;
        private FXTaskFailedCallback onFailed = null;
        
        FXMLLoadTask(URL fxmlFile, boolean async) {
            this.fxmlFile = fxmlFile;
            this.async = async;
        }
        
        public FXMLLoadTask withController(Object controller) {
            this.controller = controller;
            return this;
        }
        
        public FXMLLoadTask onLoaded(FXMLLoadedCallback callback) {
            this.onLoaded = callback;
            return this;
        }
        
        public FXMLLoadTask onFailed(FXTaskFailedCallback callback) {
            this.onFailed = callback;
            return this;
        }
        
        public void load() {
            if (async) {
                new Thread(this::startLoad).start();
            } else {
                startLoad();
            }
        }
        
        private void startLoad() {
            try {
                FXMLLoader loader = new FXMLLoader(fxmlFile);
                if (controller != null) loader.setController(controller);
                
                root = loader.load();
                controller = loader.getController();
                
                if (onLoaded != null) onLoaded.onLoaded(root, controller);
            } catch (Exception e) {
                if (onFailed != null) onFailed.onFailed(e);
            }
        }
    }
    
    public static class CreateWindowTask {
        private FXMLLoadTask fxmlLoadTask;
        private Stage stage = null;
        private FXTaskFailedCallback onFailed = null;
        private CreateWindowDoneCallback onDone = null;
        
        CreateWindowTask(URL fxmlFile, boolean async) {
            fxmlLoadTask = new FXMLLoadTask(fxmlFile, async);
            fxmlLoadTask.onLoaded(this::doOnFxmlLoaded);
        }
        
        public CreateWindowTask withController(Object controller) {
            fxmlLoadTask.withController(controller);
            return this;
        }
        
        public CreateWindowTask withStage(Stage stage) {
            this.stage = stage;
            return this;
        }
        
        public CreateWindowTask onLoaded(FXMLLoadedCallback callback) {
            fxmlLoadTask.onLoaded((root, controller) -> {
                if (callback != null) callback.onLoaded(root, controller);
                doOnFxmlLoaded(root, controller);
            });
            
            return this;
        }
        
        public CreateWindowTask onFailed(FXTaskFailedCallback callback) {
            fxmlLoadTask.onFailed(callback);
            this.onFailed = callback;
            return this;
        }
        
        public CreateWindowTask onDone(CreateWindowDoneCallback onDone) {
            this.onDone = onDone;
            return this;
        }
        
        public void create() {
            fxmlLoadTask.load();
        }
        
        private void doOnFxmlLoaded(Parent root, Object controller) {
            Scene scene = new Scene(root);
            
            if (isFXThread()) {
                createStage(scene, controller);
            } else {
                Platform.runLater(() -> createStage(scene, controller));
            }
        }
        
        private void createStage(Scene scene, Object controller) {
            try {
                if (stage == null) {
                    stage = new Stage();
                }
                stage.setScene(scene);
                
                if (controller instanceof StageSceneController) {
                    ((StageSceneController) controller).setScene(scene);
                    ((StageSceneController) controller).setStage(stage);
                }
                
                if (onDone != null) onDone.onDone(stage, scene, controller);
            } catch (Exception e) {
                if (onFailed != null) onFailed.onFailed(e);
            }
        }
    }
    
    public static class OpenWindowTask {
        private CreateWindowTask createWindowTask;
        private FXTaskFailedCallback onFailed = null;
        private WindowShownCallback onShown = null;
        
        OpenWindowTask(URL fxmlFile, boolean async) {
            createWindowTask = new CreateWindowTask(fxmlFile, async);
            createWindowTask.onDone(this::doOnCreateWindowDone);
        }
        
        public OpenWindowTask withController(Object controller) {
            createWindowTask.withController(controller);
            return this;
        }
        
        public OpenWindowTask withStage(Stage stage) {
            createWindowTask.stage = stage;
            return this;
        }
        
        public OpenWindowTask onLoaded(FXMLLoadedCallback callback) {
            createWindowTask.onLoaded(callback);
            return this;
        }
        
        public OpenWindowTask onFailed(FXTaskFailedCallback callback) {
            createWindowTask.onFailed(callback);
            this.onFailed = callback;
            return this;
        }
        
        public OpenWindowTask onDone(CreateWindowDoneCallback callback) {
            createWindowTask.onDone((stage, scene, controller) -> {
                if (callback != null) {
                    callback.onDone(stage, scene, controller);
                    doOnCreateWindowDone(stage, scene, controller);
                }
            });
            return this;
        }
        
        public OpenWindowTask onShown(WindowShownCallback onShown) {
            this.onShown = onShown;
            return this;
        }
        
        public void open() {
            createWindowTask.create();
        }
        
        private void doOnCreateWindowDone(Stage stage, Scene scene, Object controller) {
            try {
                stage.show();
                if (onShown != null) onShown.onShown();
            } catch (Exception e) {
                if (onFailed != null) onFailed.onFailed(e);
            }
        }
    }
    
    
    public interface FXMLLoadedCallback {
        void onLoaded(Parent root, Object controller);
    }
    
    public interface FXTaskFailedCallback {
        void onFailed(Exception e);
    }
    
    public interface CreateWindowDoneCallback {
        void onDone(Stage stage, Scene scene, Object controller);
    }
    
    public interface WindowShownCallback {
        void onShown();
    }
    
    private FXHelper() {
    }
}