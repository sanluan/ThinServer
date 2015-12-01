package com.sanluan.gpio4pi;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.sanluan.server.handler.ThinHttpHandler;

public class PiController {
    ThinHttpHandler handler;
    final GpioController GPIOCONTROLLER = GpioFactory.getInstance();
    final GpioPinDigitalInput[] input = { GPIOCONTROLLER.provisionDigitalInputPin(RaspiPin.GPIO_01),
            GPIOCONTROLLER.provisionDigitalInputPin(RaspiPin.GPIO_02), GPIOCONTROLLER.provisionDigitalInputPin(RaspiPin.GPIO_03),
            GPIOCONTROLLER.provisionDigitalInputPin(RaspiPin.GPIO_04) };
    final GpioPinDigitalOutput[] output = { GPIOCONTROLLER.provisionDigitalOutputPin(RaspiPin.GPIO_05, PinState.HIGH),
            GPIOCONTROLLER.provisionDigitalOutputPin(RaspiPin.GPIO_06, PinState.HIGH),
            GPIOCONTROLLER.provisionDigitalOutputPin(RaspiPin.GPIO_07, PinState.HIGH),
            GPIOCONTROLLER.provisionDigitalOutputPin(RaspiPin.GPIO_08, PinState.HIGH) };

    public PiController(ThinHttpHandler handler) {
        for (int i = 0; i < input.length; i++) {
            output[i].setShutdownOptions(true, PinState.HIGH, PinPullResistance.OFF);
            input[i].addListener(new MyGpioPinListenerDigital(output[i], this, i + 1));
        }
    }

    public void click(String gpioName) {
        try {
            int i = Integer.parseInt(gpioName);
            if (i < output.length) {
                output[i].toggle();
                if (3 == i) {
                    shutdownServer();
                }
            }
        } catch (Exception e) {
        }
    }

    public void shutdown() {
        for (int i = 0; i < input.length; i++) {
            input[i].removeAllListeners();
        }
    }

    public void shutdownServer() {
        if (null != handler.getHttpServer()) {
            shutdown();
            handler.getHttpServer().stop();
            handler.getHttpServer().shutdownServerSocketController();
        }
    }
}

class MyGpioPinListenerDigital implements GpioPinListenerDigital {
    GpioPinDigitalOutput output;
    PiController controller;
    int i;

    public MyGpioPinListenerDigital(GpioPinDigitalOutput output, PiController controller, int i) {
        this.output = output;
        this.controller = controller;
        this.i = i;
    }

    @Override
    public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
        if (PinState.HIGH == event.getState()) {
            output.toggle();
            if (4 == i) {
                controller.shutdownServer();
            }
        }
    }
}