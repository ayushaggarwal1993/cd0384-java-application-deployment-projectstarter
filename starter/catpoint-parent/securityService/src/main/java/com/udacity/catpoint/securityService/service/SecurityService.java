package com.udacity.catpoint.securityService.service;

import com.udacity.catpoint.imagingService.service.ImageService;
import com.udacity.catpoint.securityService.application.StatusListener;
import com.udacity.catpoint.securityService.data.AlarmStatus;
import com.udacity.catpoint.securityService.data.ArmingStatus;
import com.udacity.catpoint.securityService.data.SecurityRepository;
import com.udacity.catpoint.securityService.data.Sensor;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Service that receives information about changes to the security system. Responsible for
 * forwarding updates to the repository and making any decisions about changing the system state.
 *
 * This is the class that should contain most of the business logic for our system, and it is the
 * class you will be writing unit tests for.
 */
public class SecurityService {


    private ImageService imageService;
    private boolean catFound = false;
    private SecurityRepository securityRepository;
    private Set<StatusListener> statusListeners = new HashSet<>();


    public SecurityService(SecurityRepository securityRepository, ImageService imageService) {
        this.securityRepository = securityRepository;
        this.imageService = imageService;
    }

    /**
     * Sets the current arming status for the system. Changing the arming status
     * may update both the alarm status.
     * @param armingStatus
     */
    public void setArmingStatus(ArmingStatus armingStatus) {
        if(armingStatus == ArmingStatus.DISARMED) {
            setAlarmStatus(AlarmStatus.NO_ALARM);
        } else {
            if(armingStatus == ArmingStatus.ARMED_HOME && catFound){
                setAlarmStatus(AlarmStatus.ALARM);
            }
            // ConcurrentSkipListSet is used to solve concurrency issue with GUI
            ConcurrentSkipListSet<Sensor> allSensors = new ConcurrentSkipListSet<>(securityRepository.getSensors());
            allSensors.forEach(sensor -> changeSensorActivationStatus(sensor, false));
        }

        securityRepository.setArmingStatus(armingStatus);
    }

    /**
     * Internal method to check if all sensors are inactive
     * @return True if  all sensors are active else false
     */
    private boolean checkIfAllSensorsInactive(){
        boolean allSensorsInactive = true;
        Set<Sensor> allSensors = securityRepository.getSensors();
        for (Sensor sensor : allSensors) {
            if(sensor.getActive()){
                allSensorsInactive = false;
                break;
            }
        }
        return allSensorsInactive;
    }

    /**
     * Internal method that handles alarm status changes based on whether
     * the camera currently shows a cat.
     * @param cat True if a cat is detected, otherwise false.
     */
    private void catDetected(Boolean cat) {
        catFound = cat;
        if(cat && getArmingStatus() == ArmingStatus.ARMED_HOME) {
            setAlarmStatus(AlarmStatus.ALARM);
        } else if(!cat && checkIfAllSensorsInactive()){
            setAlarmStatus(AlarmStatus.NO_ALARM);
        }

        statusListeners.forEach(sl -> sl.catDetected(cat));
    }

    /**
     * Register the StatusListener for alarm system updates from within the SecurityService.
     * @param statusListener
     */
    public void addStatusListener(StatusListener statusListener) {
        statusListeners.add(statusListener);
    }

    public void removeStatusListener(StatusListener statusListener) {
        statusListeners.remove(statusListener);
    }

    /**
     * Change the alarm status of the system and notify all listeners.
     * @param status
     */
    public void setAlarmStatus(AlarmStatus status) {
        securityRepository.setAlarmStatus(status);
        statusListeners.forEach(sl -> sl.notify(status));
    }

    /**
     * Internal method for updating the alarm status when a sensor has been activated.
     */
    private void handleSensorActivated(Sensor sensor) {
        if(securityRepository.getArmingStatus() == ArmingStatus.DISARMED) {
            return; //no problem if the system is disarmed
        }
        switch(securityRepository.getAlarmStatus()) {
            case NO_ALARM : if(!sensor.getActive()){
                setAlarmStatus(AlarmStatus.PENDING_ALARM);
                break;
            }
            case PENDING_ALARM : setAlarmStatus(AlarmStatus.ALARM);
        }
    }

    /**
     * Internal method for updating the alarm status when a sensor has been deactivated
     */
    private void handleSensorDeactivated(Sensor sensor) {
        switch(securityRepository.getAlarmStatus()) {
            case PENDING_ALARM :
                boolean allSensorsInactive = true;
                for (Sensor sensor1 : securityRepository.getSensors()) {
                    if (sensor1.equals(sensor)){
                        continue;
                    }
                    if (sensor1.getActive()){
                        allSensorsInactive = false;
                        break;
                    }
                }
                if(allSensorsInactive) {
                    setAlarmStatus(AlarmStatus.NO_ALARM);
                }
            //case ALARM -> setAlarmStatus(AlarmStatus.PENDING_ALARM);
        }
    }

    /**
     * Change the activation status for the specified sensor and update alarm status if necessary.
     * @param sensor
     * @param active
     */
    public void changeSensorActivationStatus(Sensor sensor, Boolean active) {
        if(active) {
            handleSensorActivated(sensor);
        } else if (sensor.getActive()) {
            handleSensorDeactivated(sensor);
        }
        sensor.setActive(active);
        securityRepository.updateSensor(sensor);
        statusListeners.forEach(sl -> sl.sensorStatusChanged());
    }

    /**
     * Send an image to the SecurityService for processing. The securityService will use its provided
     * ImageService to analyze the image for cats and update the alarm status accordingly.
     * @param currentCameraImage
     */
    public void processImage(BufferedImage currentCameraImage) {
        catDetected(imageService.imageContainsCat(currentCameraImage, 50.0f));
    }

    public AlarmStatus getAlarmStatus() {
        return securityRepository.getAlarmStatus();
    }

    public Set<Sensor> getSensors() {
        return securityRepository.getSensors();
    }

    public void addSensor(Sensor sensor) {
        securityRepository.addSensor(sensor);
    }

    public void removeSensor(Sensor sensor) {
        securityRepository.removeSensor(sensor);
    }

    public ArmingStatus getArmingStatus() {
        return securityRepository.getArmingStatus();
    }
}
