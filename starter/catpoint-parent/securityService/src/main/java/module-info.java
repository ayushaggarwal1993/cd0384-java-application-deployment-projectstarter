module com.udacity.catpoint.securityService {
    requires java.desktop;
    requires com.miglayout.swing;
    requires java.prefs;
    requires gson;
    requires guava;
    requires com.udacity.catpoint.imagingService;
    opens com.udacity.catpoint.securityService.data to gson;
}