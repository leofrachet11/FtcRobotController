package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.PwmControl;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.ServoControllerEx;

@TeleOp(name="Spindexer/arm Calibration", group="Calibration")
public class SpindexerArmCalibration extends LinearOpMode {

    private Servo spindexerServo, armServo;
    private double spinPos = 0.02;
    private double armPos = 0.475;
    private boolean calibratingSpindexer = true;

    @Override
    public void runOpMode() {
        spindexerServo = hardwareMap.get(Servo.class, "spindexerServo");
        armServo = hardwareMap.get(Servo.class, "armServo");

        setServoRange(spindexerServo);
        setServoRange(armServo);

        waitForStart();

        while (opModeIsActive()) {
            if (gamepad1.back) {
                calibratingSpindexer = !calibratingSpindexer;
                sleep(300);
            }

            if (calibratingSpindexer) {
                if (gamepad1.dpad_up) spinPos += 0.1;
                if (gamepad1.dpad_down) spinPos -= 0.1;
                if (gamepad1.dpad_right) spinPos += 0.005;
                if (gamepad1.dpad_left) spinPos -= 0.005;
                spinPos = Math.max(0, Math.min(1, spinPos));
                spindexerServo.setPosition(spinPos);
            } else {
                if (gamepad1.dpad_up) armPos += 0.05;
                if (gamepad1.dpad_down) armPos -= 0.05;
                if (gamepad1.dpad_right) armPos += 0.005;
                if (gamepad1.dpad_left) armPos -= 0.005;
                armPos = Math.max(0, Math.min(1, armPos));
                armServo.setPosition(armPos);
            }

            telemetry.addData("CALIBRATING", calibratingSpindexer ? "SPINDEXER" : "ARM");
            telemetry.addData("Spindexer Pos", "%.3f", spinPos);
            telemetry.addData("Arm Pos", "%.3f", armPos);
            telemetry.update();
            sleep(100);
        }
    }

    private void setServoRange(Servo s) {
        if (s.getController() instanceof ServoControllerEx) {
            ((ServoControllerEx) s.getController()).setServoPwmRange(s.getPortNumber(), new PwmControl.PwmRange(500, 2500));
        }
    }
}