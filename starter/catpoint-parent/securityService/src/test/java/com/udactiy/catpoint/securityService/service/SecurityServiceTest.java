package com.udactiy.catpoint.securityService.service;


import com.udacity.catpoint.imagingService.service.ImageService;
import com.udacity.catpoint.securityService.data.*;
import com.udacity.catpoint.securityService.service.SecurityService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {


    private SecurityService securityService;

    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private ImageService imageService;

    private  Sensor sensor;

    // This method will be used to generate sensors of mixed status
    private Set<Sensor> getAllMixedSensors(int count) {
        Set<Sensor> sensors = new HashSet<>();
        for (int i = 0; i < count; i++) {
            Sensor sensor = new Sensor("Test"+i, SensorType.DOOR);
            sensor.setActive(new Random().nextBoolean());
            sensors.add(sensor);

        }

        return sensors;
    }

    //This method will be used to generate sensors of same status
    private Set<Sensor> getRandomSensorsSameStatus(int number, boolean status) {
        Set<Sensor> sensors = new HashSet<>();
        for (int i = 0; i < number; i++) {
            Sensor sensor = new Sensor("Test"+i,SensorType.MOTION);
            sensors.add(sensor);
            sensor.setActive(status);
        }
        return sensors;
    }

    @BeforeEach
    public void init(){
        securityService = new SecurityService(securityRepository,imageService);
        sensor = new Sensor("Testing",SensorType.WINDOW);
    }

    //Test1
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME","ARMED_AWAY"})
    public void ifAlarmIsArmed_sensorIsActivated_systemInNoAlarm_putsSystemInPendingStatus(ArmingStatus status){
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        when(securityRepository.getArmingStatus()).thenReturn(status);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    //Test2
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME","ARMED_AWAY"})
    public void ifAlarmIsArmed_sensorIsActivated_systemInPendingStatus_setsAlarmStatusToAlarm(ArmingStatus status){
        when(securityRepository.getArmingStatus()).thenReturn(status);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    //Test3
    @Test
    public void ifAlarmIsPendingAlarm_sensorIsInActivated_systemInNoAlarm(){
        Set<Sensor> allSensors = getRandomSensorsSameStatus(4, false);
        Sensor lastSensor = allSensors.iterator().next();
        lastSensor.setActive(true);
        when(securityRepository.getSensors()).thenReturn(allSensors);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(lastSensor, false);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    //Test4
    @Test
    public void ifAlarmIsActive_sensorIsInActivatedOrDeactivated_NoChangeInAlarmStatus(){
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    // Test 5
    @Test
    public void ifSensorIsActive_againActivate_systemInPendingState_systemChangesToAlarmStatus(){
        sensor.setActive(true);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    // Test 6
    @Test
    public void ifSensorIsInactive_againDeactivate_NoChangeInAlarmStatus(){
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    //Test 7
    @Test
    public void ifImageServiceIdentifiesCat_systemIsArmedHome_putSystemInAlarmStatus(){
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(BufferedImage.class),anyFloat())).thenReturn(true);
        BufferedImage image = new BufferedImage(123, 789, BufferedImage.TYPE_BYTE_INDEXED);
        securityService.processImage(image);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    //Test 8
    @Test
    public void ifImageServiceDoesNotIdentifiesCat_sensorsAreNotActive_putSystemInNoAlarm(){
        Set<Sensor> sensors = getRandomSensorsSameStatus(3, false);
        when(securityRepository.getSensors()).thenReturn(sensors);
        when(imageService.imageContainsCat(any(BufferedImage.class),anyFloat())).thenReturn(false);
        BufferedImage image = new BufferedImage(123, 789, BufferedImage.TYPE_BYTE_INDEXED);
        securityService.processImage(image);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }


    //Test 9
    @Test
    public void ifSystemIsDisarmed_statusIsSetToNoAlarm(){
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    private static Stream<Arguments> provideArmingStatusAndIsAlarmStatus() {
        return Stream.of(
                Arguments.of(ArmingStatus.ARMED_HOME, AlarmStatus.ALARM),
                Arguments.of(ArmingStatus.ARMED_HOME, AlarmStatus.NO_ALARM),
                Arguments.of(ArmingStatus.ARMED_HOME, AlarmStatus.PENDING_ALARM),
                Arguments.of(ArmingStatus.ARMED_AWAY, AlarmStatus.ALARM),
                Arguments.of(ArmingStatus.ARMED_AWAY, AlarmStatus.NO_ALARM),
                Arguments.of(ArmingStatus.ARMED_AWAY, AlarmStatus.PENDING_ALARM)
        );
    }

    // Test 10
    @ParameterizedTest
    @MethodSource("provideArmingStatusAndIsAlarmStatus")
    public void ifSystemIsArmed_allSensorsAreResetToInactive(ArmingStatus status, AlarmStatus alarm){
        Set<Sensor> sensors = getAllMixedSensors(6);
        when(securityRepository.getSensors()).thenReturn(sensors);
        when(securityRepository.getAlarmStatus()).thenReturn(alarm);
        securityService.setArmingStatus(status);

        securityService.getSensors().forEach(sensor -> {
            assertFalse(sensor.getActive());
            verify(securityRepository,times(1)).updateSensor(sensor);
        });
    }


    //Test 11
    @Test
    public void ifSystemIsArmedHome_cameraShowsCat_setAlarmStatusToAlarm(){
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(BufferedImage.class),anyFloat())).thenReturn(true);
        BufferedImage image = new BufferedImage(123, 789, BufferedImage.TYPE_BYTE_INDEXED);
        securityService.processImage(image);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }


}
