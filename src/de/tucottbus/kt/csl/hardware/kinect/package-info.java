/**
 * This package implements an interface to the Microsoft`s kinect. 
 * The class {@link de.tucottbus.kt.csl.hardware.kinect.devices.AKinectSensor} is the wrapper for every kinect. 
 * This wrapper communicates with a sensor and handles the data. Do not use directly this wrapper to use a kinect. 
 * To use a certain kinect you have to implement a new class, which extends this class.
 * The classes {@link de.tucottbus.kt.csl.hardware.kinect.devices.KinectSensor1_000} and 
 * {@link de.tucottbus.kt.csl.hardware.kinect.devices.KinectSensor2_000} implement the 
 * two kinects (version 1 and 2) in the cognitive system lab. 
 * The extended classes are singletons. The kinect v1 is not usable after disconnecting and reconnecting. 
 * However the kinect v2 does. The reinitializing is managed by the J4KSDK itself, 
 * therefore you can consider the kinect v2 as connected. 
 * But if you want to know wether the kinect is really connected, you can use the method 
 * {@link de.tucottbus.kt.csl.hardware.kinect.devices.KinectSensor2_000#isRunning()}.
 * 
 * <p>Real objects in the cognitive system lab (e.g. Display) have to register in the class 
 * {@link de.tucottbus.kt.csl.hardware.kinect.room.ListOfObject3D}. 
 * This class creates virtual objects as {@link de.tucottbus.kt.csl.hardware.kinect.room.Object3D} 
 * of the registered objects for the internal use.</p>
 * 
 * @author Thomas Jung
 */
package de.tucottbus.kt.csl.hardware.kinect;